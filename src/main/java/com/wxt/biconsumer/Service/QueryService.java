package com.wxt.biconsumer.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wxt.biconsumer.Entity.Graph;
import com.wxt.biconsumer.Entity.MongoEntity.EntityNode;
import com.wxt.biconsumer.Entity.MongoEntity.RelationById;
import com.wxt.biconsumer.Entity.Neo4jEntity.QueryEntity;
import com.wxt.biconsumer.Entity.ResponseFormat;
import com.wxt.biconsumer.Service.MongoService.MongoQueryService;
import com.wxt.biconsumer.Service.Neo4jService.Neo4jQueryService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class QueryService {

    @Resource
    private MongoQueryService mongoQueryService;

    @Resource
    private Neo4jQueryService neo4jQueryService;

    @Resource
    private RedisTemplate redisTemplate;

    private ExecutorService es;

    @PostConstruct
    private void init(){
        es = new ThreadPoolExecutor(5,
                20,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public EntityNode getSingleLinkByName(String nodeName){
        return getSingleLinks(nodeName);
    }
    public EntityNode getSingleLinksById(Integer uniqueId){
        return getSingleLinks(uniqueId);
    }

    public Graph getPathsByTwoNodes(String startNodeName,
                                    String endNodeName,           //终点UniqueId
                                    Integer skip_num,       //跳过的边数
                                    Integer limit_num,      //返回的边数
                                    Integer max_jump_num){
        Integer id1 = getNodeId(startNodeName), id2 = getNodeId(endNodeName);
        if(id1 == null || id2 == null){
            return null;
        }
        QueryEntity queryEntity = new QueryEntity(id1, id2, skip_num, limit_num, max_jump_num);
        Graph g = (Graph)redisTemplate.opsForValue().get(queryEntity.toString());
        if(g != null){
            return g;
        }
        //List<List<Integer>> paths = neo4jQueryService.queryNodesPaths(new QueryEntity(id1, id2, skip_num, limit_num, max_jump_num));
        List<List<Integer>> paths = new ArrayList<>();
        paths.add(Arrays.asList(1, 4, 4066568));
        paths.add(Arrays.asList(1, 1025109, 4387057, 12));

        Set<Integer> ids = new HashSet<>(paths.size() * 4);
        paths.forEach(ids::addAll);
        Map<Integer, String> idToNames = new HashMap<>(ids.size());
        List<Integer> nonCachedBatchIds = new ArrayList<>(ids.size());
        ids.forEach(id -> {
            String name = getCachedNodeName(id);
            if(name != null){
                idToNames.put(id, name);
            }else{
                nonCachedBatchIds.add(id);
            }
        });
        List<EntityNode> tempNames = mongoQueryService.getBatchNamesByIds(nonCachedBatchIds);
        if(tempNames == null){
            return null;
        }
        tempNames.forEach(tn -> idToNames.put(tn.getUniqueId(), tn.getName()));

        Map<Integer, List<Integer>> pairs = new HashMap<>(ids.size());
        paths.forEach(l -> {
            for (int i = 1; i < l.size(); i++) {
                int left = l.get(i - 1);
                int right = l.get(i);
                pairs.compute(left, (key, data) -> {
                    if(data == null){
                        data = new ArrayList<>(16);
                    }
                    data.add(right);
                    return data;
                });
            }
        });
        Map<Integer, List<Integer>> nonCachedPairs = new HashMap<>(pairs.size());
        Map<Integer, Set<RelationById>> cachedRelations = new HashMap<>(pairs.size());
        pairs.forEach((key, l) -> {
            List<String> relations = getCachedRelationBetween(key, l);
            for (int i = 0; i < l.size(); i++) {
                if(relations.get(i) == null){
                    Integer tempId = l.get(i);
                    nonCachedPairs.compute(key, (k, nonCachedList) -> {
                        if(nonCachedList == null){
                            nonCachedList = new ArrayList<>(16);
                        }
                        nonCachedList.add(tempId);
                        return nonCachedList;
                    });
                }else{
                    String r = relations.get(i);
                    cachedRelations.compute(key, (k, v) -> {
                        if(v == null){
                            v = new HashSet<>();
                        }
                        for (Integer right : l) {
                            RelationById rbi = new RelationById();
                            rbi.setStartUniqueId(k);
                            rbi.setRelation(r);
                            rbi.setEndUniqueId(right);
                            v.add(rbi);
                        }
                        return v;
                    });
                }
            }
        });
        Map<Integer, Set<RelationById>> nonCachedRelations = getBatchRelations(nonCachedPairs);
        if(nonCachedRelations == null){
            return null;
        }
        saveRelationsBetween(nonCachedRelations);
        nonCachedRelations.forEach((left, r) -> {
            cachedRelations.compute(left, (k, v) -> {
                if(v == null){
                    v = new HashSet<>(16);
                }
                v.addAll(r);
                return v;
            });
        });
        return Graph.generateGraph(idToNames, cachedRelations);
    }

    public Double getSimilarityByTwoNodes(String name1, String name2){
        return 0.;
    }




    //private utils

    private EntityNode getSingleLinks(Object param){
        EntityNode node = null;

        if(param instanceof String){
            node = getCachedEntityNodeByName((String)param);
            if(node == null){
                node = mongoQueryService.getSingleLinkByName((String)param);
                cacheEntityNode(node);
            }
        }else if(param instanceof Integer){
            node = getCachedEntityNodeById((Integer)param);
            if(node == null){
                node = mongoQueryService.getSingleLinksById((Integer) param);
                cacheEntityNode(node);
            }
        }else{
            assert false;
        }
        return node;
    }

    private String getSingleLinkRedisKey(Integer uniqueId){
        return uniqueId + "SingleLinks";
    }

    private void cacheEntityNode(EntityNode entityNode){
        redisTemplate.opsForValue().setIfAbsent(getSingleLinkRedisKey(entityNode.getUniqueId()), entityNode);
        es.submit(() -> {
            Map cached = new HashMap();
            Map<Integer, String> relations = new HashMap<>();
            entityNode.getLinks().forEach(nodeToRelation -> {
                String name = nodeToRelation.getNode();
                Integer uniqueId = nodeToRelation.getUniqueId();
                cached.put(name + "name", uniqueId);
                cached.put(uniqueId + "id", name);
                relations.put(uniqueId, nodeToRelation.getRelation());
            });
            cached.put(entityNode.getName() + "name", entityNode.getUniqueId());
            cached.put(entityNode.getUniqueId() + "id", entityNode.getName());
            redisTemplate.opsForValue().multiSetIfAbsent(cached);
            redisTemplate.opsForHash().putAll(entityNode.getUniqueId() + "relation", relations);
        });
    }

    private EntityNode getCachedEntityNodeById(Integer param){
        return (EntityNode) redisTemplate.opsForValue().get(getSingleLinkRedisKey(param));
    }

    private EntityNode getCachedEntityNodeByName(String name){
        Integer id = getNodeId(name);
        return (EntityNode) redisTemplate.opsForValue().get(getSingleLinkRedisKey(id));
    }

    private String getNodeName(Integer uniqueId){
        String name = getCachedNodeName(uniqueId);
        if(name == null){
            name = mongoQueryService.getEntityNameById(uniqueId);
            cacheIdToName(uniqueId, name);
        }
        return name;
    }

    private Integer getNodeId(String name){
        Integer id = getCachedUniqueId(name);
        if(id == null){
            id = mongoQueryService.getEntityIdByName(name);
            cacheNameToId(name, id);
        }
        return id;
    }

    private String getCachedNodeName(Integer uniqueId){
        return (String)redisTemplate.opsForValue().get(uniqueId + "id");
    }

    private Integer getCachedUniqueId(String name){
        return (Integer)redisTemplate.opsForValue().get(name + "name");
    }

    private void cacheIdToName(Integer id, String name){
        redisTemplate.opsForValue().setIfAbsent(id + "id", name);
    }

    private void cacheNameToId(String name, Integer id){
        redisTemplate.opsForValue().setIfAbsent(name + "name", id);
    }


    private List<String> getCachedRelationBetween(Integer id1, List<Integer> id2s){
        List<String> list = redisTemplate.opsForHash().multiGet(id1 + "relation", id2s);
        return list;
    }


    private void saveRelationsBetween(Map<Integer, Set<RelationById>> nonCachedRelations){
        nonCachedRelations.forEach((left, rbiSet) -> {
            Map<Integer, String> m = new HashMap<>(rbiSet.size());
            rbiSet.forEach(rbi -> {
                m.put(rbi.getEndUniqueId(), rbi.getRelation());
            });
            redisTemplate.opsForHash().putAll(left + "relation", m);
        });
    }


    private Map<Integer, Set<RelationById>> getBatchRelations(Map<Integer, List<Integer>> pairs){
        return mongoQueryService.getBatchRelations(pairs);
    }
}
