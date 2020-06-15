package com.wxt.biconsumer.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wxt.biconsumer.Entity.Graph;
import com.wxt.biconsumer.Entity.MongoEntity.EntityNode;
import com.wxt.biconsumer.Entity.MongoEntity.NodeToRelation;
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
import java.util.stream.Collectors;

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

    public EntityNode getSingleLinksByIdPageble(Integer uniqueId, int startFrom, int limit){
        return getSingleLinksPageable(uniqueId, startFrom, limit);
    }

    public EntityNode getSingleLinksByNamePageble(String name, int startFrom, int limit){
        return getSingleLinksPageable(name, startFrom, limit);
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
        List<List<Integer>> paths = neo4jQueryService.queryNodesPaths(new QueryEntity(id1, id2, skip_num, limit_num, max_jump_num));
        //List<List<Integer>> paths = new ArrayList<>();
        //paths.add(Arrays.asList(1, 4, 4066568));
        //paths.add(Arrays.asList(1, 1025109, 4387057, 12));

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
        if(nonCachedBatchIds.size() > 0){
            List<EntityNode> tempNames = mongoQueryService.getBatchNamesByIds(nonCachedBatchIds);
            if(tempNames == null){
                return null;
            }
            tempNames.forEach(tn -> {
                idToNames.put(tn.getUniqueId(), tn.getName());
                cacheIdToName(tn.getUniqueId(), tn.getName());
                cacheNameToId(tn.getName(), tn.getUniqueId());
            });
        }

        Map<Integer, Set<Integer>> pairs = new HashMap<>(ids.size());
        paths.forEach(l -> {
            for (int i = 1; i < l.size(); i++) {
                int left = l.get(i - 1);
                int right = l.get(i);
                pairs.compute(left, (key, data) -> {
                    if(data == null){
                        data = new HashSet<>(16);
                    }
                    data.add(right);
                    return data;
                });
            }
        });
        Map<Integer, List<Integer>> nonCachedPairs = new HashMap<>(pairs.size());
        Map<Integer, Set<RelationById>> cachedRelations = new HashMap<>(pairs.size());
        pairs.forEach((key, l) -> {
            List<Integer> ll = new ArrayList<>(l);
            List<List<RelationById>> relations = getCachedRelationBetween(key, ll);
            for (int i = 0; i < ll.size(); i++) {
                if(relations.get(i) == null){
                    Integer tempId = ll.get(i);
                    nonCachedPairs.compute(key, (k, nonCachedList) -> {
                        if(nonCachedList == null){
                            nonCachedList = new ArrayList<>(16);
                        }
                        nonCachedList.add(tempId);
                        return nonCachedList;
                    });
                }else{
                    List<RelationById> r = relations.get(i);//relations.get(i).stream().map(o -> JSON.toJavaObject(o, RelationById.class)).collect(Collectors.toList());
                    cachedRelations.compute(key, (k, v) -> {
                        if(v == null){
                            v = new HashSet<>();
                        }
                        v.addAll(r);
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
        g = Graph.generateGraph(idToNames, cachedRelations);
        //redisTemplate.opsForValue().set(queryEntity.toString(), g);
        return g;
    }

    public Double getSimilarityByTwoNodes(String name1, String name2){
        Integer id1 = getNodeId(name1), id2 = getNodeId(name2);
        if(id1 == null || id2 == null){
            return null;
        }
        Double sim = (Double)redisTemplate.opsForValue().get(id1 + "_simwith_" + id2);
        if(sim != null){
            return sim;
        }
        EntityNode node1 = getSingleLinksById(id1);
        EntityNode node2 = getSingleLinksById(id2);
        Set<Integer> s1 = node1.getLinks().stream().map(NodeToRelation::getUniqueId).collect(Collectors.toSet());
        Set<Integer> s2 = node2.getLinks().stream().map(NodeToRelation::getUniqueId).collect(Collectors.toSet());
        Set<Integer> intersected = new HashSet<>(s1);
        intersected.retainAll(s2);
        int intersectedCount = intersected.size();
        intersected = null;
        s1.addAll(s2);
        int unionCount = s1.size();
        sim = intersectedCount * 1. / unionCount;
        redisTemplate.opsForValue().set(id1 + "_simwith_" + id2, sim);
        redisTemplate.opsForValue().set(id2 + "_simwith_" + id1, sim);
        return sim;
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

    private EntityNode getSingleLinksPageable(Object param, int startFrom, int limit){
        EntityNode node = null;
        if(param instanceof String){
            node = getCachedEntityNodeByName((String)param);
            if(node == null){
                node = mongoQueryService.getSingleLinkByNamePageable((String)param, startFrom, limit);
                cacheEntityNode(node);
            }
        }else if(param instanceof Integer){
            node = getCachedEntityNodeById((Integer)param);
            if(node == null){
                node = mongoQueryService.getSingleLinksByIdPageable((Integer) param, startFrom, limit);
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
            Set<RelationById> relations = new HashSet<>();
            entityNode.getLinks().forEach(nodeToRelation -> {
                String name = nodeToRelation.getNode();
                Integer uniqueId = nodeToRelation.getUniqueId();
                cached.put(name + "name", uniqueId);
                cached.put(uniqueId + "id", name);
                RelationById relationById = new RelationById();
                relationById.setRelation(nodeToRelation.getRelation());
                if(nodeToRelation.getDirection() == 1){
                    relationById.setStartUniqueId(entityNode.getUniqueId());
                    relationById.setEndUniqueId(uniqueId);
                }else{
                    relationById.setStartUniqueId(uniqueId);
                    relationById.setEndUniqueId(entityNode.getUniqueId());
                }
                relations.add(relationById);
            });
            cached.put(entityNode.getName() + "name", entityNode.getUniqueId());
            cached.put(entityNode.getUniqueId() + "id", entityNode.getName());
            redisTemplate.opsForValue().multiSetIfAbsent(cached);
            saveOneSetRelations(entityNode.getUniqueId(), relations);
        });
    }

    private EntityNode getCachedEntityNodeById(Integer param){
        return (EntityNode) redisTemplate.opsForValue().get(getSingleLinkRedisKey(param));
    }

    private EntityNode getCachedEntityNodeByName(String name){
        Integer id = getNodeId(name);
        return (EntityNode) redisTemplate.opsForValue().get(getSingleLinkRedisKey(id));
    }

    private Integer getNodeId(String name){
        Integer id = getCachedUniqueId(name);
        if(id == null){
            id = mongoQueryService.getEntityIdByName(name);
            cacheNameToId(name, id);
            cacheIdToName(id, name);
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


    private List<List<RelationById>> getCachedRelationBetween(Integer id1, List<Integer> id2s){
        List<List<RelationById>> list = redisTemplate.opsForHash().multiGet(id1 + "relation", id2s);
        return list;
    }


    private void saveRelationsBetween(Map<Integer, Set<RelationById>> nonCachedRelations){
        if(nonCachedRelations.size() == 0){
            return;
        }
        nonCachedRelations.forEach(this::saveOneSetRelations);
    }

    private void saveOneSetRelations(Integer id, Set<RelationById> set){
        if(set.size() == 0){
            return;
        }
        Map<Integer, List<RelationById>> m = new HashMap<>(set.size());
        set.forEach(rbi -> {
            m.compute(rbi.getEndUniqueId(), (k, v) -> {
                if(v == null){
                    v = new ArrayList<>(set.size());
                }
                v.add(rbi);
                return v;
            });
        });
        redisTemplate.opsForHash().putAll(id + "relation", m);
    }


    private Map<Integer, Set<RelationById>> getBatchRelations(Map<Integer, List<Integer>> pairs){
        return mongoQueryService.getBatchRelations(pairs);
    }
}
