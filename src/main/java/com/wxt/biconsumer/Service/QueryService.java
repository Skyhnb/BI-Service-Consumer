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
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class QueryService {

    @Resource
    private MongoQueryService mongoQueryService;

    @Resource
    private Neo4jQueryService neo4jQueryService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private DataAccessHelper dah;

    @Resource
    private ThreadPoolHelper threadPoolHelper;

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
        Integer id1 = dah.getNodeId(startNodeName), id2 = dah.getNodeId(endNodeName);
        if(id1 == null || id2 == null){
            return null;
        }
        QueryEntity queryEntity = new QueryEntity(id1, id2, skip_num, limit_num, max_jump_num);
        Graph g = dah.getCachedGraph(queryEntity);
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
            String name = dah.getCachedNodeName(id);
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
                dah.cacheIdToName(tn.getUniqueId(), tn.getName());
                dah.cacheNameToId(tn.getName(), tn.getUniqueId());
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
            List<List<RelationById>> relations = dah.getCachedRelationBetween(key, ll);
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
        dah.saveRelationsBetween(nonCachedRelations);
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
        Graph p = g;
        threadPoolHelper.es.submit(() -> dah.cacheGraph(queryEntity, p));
        return g;
    }

    public Map getSimilarityByTwoNodes(String name1, String name2){
        Integer id1 = dah.getNodeId(name1), id2 = dah.getNodeId(name2);
        if(id1 == null || id2 == null){
            return null;
        }
        EntityNode node1 = getSingleLinksById(id1);
        EntityNode node2 = getSingleLinksById(id2);
        Future<Double> jaccardTask = threadPoolHelper.es.submit(() -> {
            Double sim = (Double)redisTemplate.opsForValue().get(id1 + "_jaccardwith_" + id2);
            if(sim != null){
                return sim;
            }
            Set<Integer> s1 = node1.getLinks().stream().map(NodeToRelation::getUniqueId).collect(Collectors.toSet());
            Set<Integer> s2 = node2.getLinks().stream().map(NodeToRelation::getUniqueId).collect(Collectors.toSet());
            Set<Integer> intersected = new HashSet<>(s1);
            intersected.retainAll(s2);
            int intersectedCount = intersected.size();
            intersected = null;
            s1.addAll(s2);
            int unionCount = s1.size();
            s1 = null; s2 = null;
            sim = intersectedCount * 1. / unionCount;
            dah.cacheSim(id1, id2, sim, true);
            return sim;
        });
        Future<Double> consineTask = threadPoolHelper.es.submit(() -> {
            Double sim = dah.getCachedSim(id1, id2, false);
            if(sim != null){
                return sim;
            }
            Map<String, Integer> bits = new HashMap<>(128);
            EntityNode[] nodes = {node1, node2};
            AtomicInteger index = new AtomicInteger(0);
            Arrays.stream(nodes).forEach(n -> n.getLinks().forEach(ntr -> bits.computeIfAbsent(ntr.getNode() + "___connects___" + ntr.getRelation(), (k) -> index.getAndIncrement())));
            int[] vector1 = new int[bits.size()];
            int[] vector2 = new int[bits.size()];
            node1.getLinks().forEach(ntr -> {
                String b = ntr.getNode() + "___connects___" + ntr.getRelation();
                vector1[bits.get(b)]++;
            });
            node2.getLinks().forEach(ntr -> {
                String b = ntr.getNode() + "___connects___" + ntr.getRelation();
                vector2[bits.get(b)]++;
            });
            //cal consine similarity
            double non = 0.;
            for (int i = 0; i < bits.size(); i++) {
                non += vector1[i] * vector2[i];
            }
            double den1 = 0., den2 = 0.;
            for (int i : vector1) {
                den1 += Math.pow(i, 2);
            }
            for (int i : vector2) {
                den2 += Math.pow(i, 2);
            }
            double den = Math.sqrt(den1 * den2);
            sim = non / den;
            dah.cacheSim(id1, id2, sim, false);
            return sim;
        });
        Map<String, Double> sims = new HashMap<>();
        try{
            Double jaccard = jaccardTask.get();
            Double cosine  = consineTask.get();
            sims.put("Jaccard", jaccard); sims.put("Cosine", cosine);
            return sims;
        }catch (Exception e){
            return null;
        }
    }


    public Integer getSingleLinksCountByName(String name){
        Integer id = dah.getCachedUniqueId(name);
        Integer count = dah.getCachedSingleLinksCount(id);
        if(count != null){
            return count;
        }
        if(id == null){
            count = mongoQueryService.getSingleLinksCountByName(name);
        }else{
            count = mongoQueryService.getSingleLinksCountById(id);
        }
        dah.cacheSingleLinksCount(id, count);
        return count;
    }

    public int getSingleLinksCountById(Integer id){
        Integer count = dah.getCachedSingleLinksCount(id);
        if(count != null){
            return count;
        }
        count = mongoQueryService.getSingleLinksCountById(id);
        dah.cacheSingleLinksCount(id, count);
        return count;
    }



    private EntityNode getSingleLinks(Object param){
        EntityNode node = null;
        if(param instanceof String){
            node = dah.getCachedEntityNodeByName((String)param);
            if(node == null){
                node = mongoQueryService.getSingleLinkByName((String)param);
                dah.cacheEntityNode(node, true);
            }
        }else if(param instanceof Integer){
            node = dah.getCachedEntityNodeById((Integer)param);
            if(node == null){
                node = mongoQueryService.getSingleLinksById((Integer) param);
                dah.cacheEntityNode(node, true);
            }
        }else{
            assert false;
        }
        return node;
    }

    private EntityNode getSingleLinksPageable(Object param, int startFrom, int limit){
        EntityNode node = null;
        if(param instanceof String){
            node = dah.getCachedEntityNodeByNamePageable((String)param, startFrom, limit);
            if(node == null){
                node = mongoQueryService.getSingleLinkByNamePageable((String)param, startFrom, limit);
                dah.cacheEntityNodePageable(node, startFrom, limit);
            }
        }else if(param instanceof Integer){
            node = dah.getCachedEntityNodeByIdPageable((Integer)param, startFrom, limit);
            if(node == null){
                node = mongoQueryService.getSingleLinksByIdPageable((Integer) param, startFrom, limit);
                dah.cacheEntityNodePageable(node, startFrom, limit);
            }
        }else{
            assert false;
        }
        return node;
    }

    private Map<Integer, Set<RelationById>> getBatchRelations(Map<Integer, List<Integer>> pairs){
        return mongoQueryService.getBatchRelations(pairs);
    }

}

@Component
class DataAccessHelper{

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private MongoQueryService mongoQueryService;

    @Resource
    private ThreadPoolHelper threadPoolHelper;

    String getCachedNodeName(Integer uniqueId){
        return (String)redisTemplate.opsForValue().get(uniqueId + "id");
    }

    Integer getCachedUniqueId(String name){
        return (Integer)redisTemplate.opsForValue().get(name + "name");
    }

    void cacheIdToName(Integer id, String name){
        redisTemplate.opsForValue().setIfAbsent(id + "id", name);
    }

    void cacheNameToId(String name, Integer id){
        redisTemplate.opsForValue().setIfAbsent(name + "name", id);
    }

    EntityNode getCachedEntityNodeById(Integer param){
        return (EntityNode) redisTemplate.opsForValue().get(getSingleLinkRedisKey(param));
    }

    Integer getNodeId(String name){
        Integer id = getCachedUniqueId(name);
        if(id == null){
            id = mongoQueryService.getEntityIdByName(name);
            cacheNameToId(name, id);
            cacheIdToName(id, name);
        }
        return id;
    }

    EntityNode getCachedEntityNodeByName(String name){
        Integer id = getNodeId(name);
        return getCachedEntityNodeById(id);
    }

    EntityNode getCachedEntityNodeByIdPageable(Integer param, int startFrom, int limit){
        return (EntityNode) redisTemplate.opsForValue().get(getSingleLinkRedisKeyPageable(param, startFrom, limit));
    }

    EntityNode getCachedEntityNodeByNamePageable(String name, int startFrom, int limit){
        Integer id = getNodeId(name);
        return getCachedEntityNodeByIdPageable(id, startFrom, limit);
    }

    void saveOneSetRelations(Integer id, Set<RelationById> set){
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

    List<List<RelationById>> getCachedRelationBetween(Integer id1, List<Integer> id2s){
        List<List<RelationById>> list = redisTemplate.opsForHash().multiGet(id1 + "relation", id2s);
        return list;
    }


    void saveRelationsBetween(Map<Integer, Set<RelationById>> nonCachedRelations){
        if(nonCachedRelations.size() == 0){
            return;
        }
        nonCachedRelations.forEach(this::saveOneSetRelations);
    }


    private String getSingleLinkRedisKey(Integer uniqueId){
        return uniqueId + "SingleLinks";
    }

    private String getSingleLinkRedisKeyPageable(Integer uniqueId, int startFrom, int limit){
        return "" + uniqueId + startFrom + limit + "SingleLinks";
    }

    void cacheEntityNodePageable(EntityNode entityNode, int startFrom, int limit){
        redisTemplate.opsForValue().setIfAbsent(getSingleLinkRedisKeyPageable(entityNode.getUniqueId(), startFrom, limit), entityNode);
        cacheEntityNode(entityNode, false);
    }

    void cacheEntityNode(EntityNode entityNode, boolean cacheSingleLink){
        threadPoolHelper.es.submit(() -> {
            if(cacheSingleLink) {
                redisTemplate.opsForValue().setIfAbsent(getSingleLinkRedisKey(entityNode.getUniqueId()), entityNode);
            }
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

    Graph getCachedGraph(QueryEntity queryEntity){
        return (Graph)redisTemplate.opsForValue().get(queryEntity.toString());
    }

    void cacheGraph(QueryEntity queryEntity, Graph graph){
        redisTemplate.opsForValue().set(queryEntity.toString(), graph);
    }

    private static String jaccardConnects = "_jaccardwith_";
    private static String cosineConnects =  "_cosinewith_";

    void cacheSim(Integer id1, Integer id2, Double sim, boolean isJaccard){
        String connects = isJaccard ? jaccardConnects : cosineConnects;
        redisTemplate.opsForValue().set(id1 + connects + id2, sim);
        redisTemplate.opsForValue().set(id2 + connects + id1, sim);
    }

    Double getCachedSim(Integer id1, Integer id2, boolean isJaccard){
        String connects = isJaccard ? jaccardConnects : cosineConnects;
        Double res = (Double)redisTemplate.opsForValue().get(id1 + connects + id2);
        if(res != null){
            return res;
        }
        res = (Double)redisTemplate.opsForValue().get(id2 + connects + id1);
        return res;
    }

    void cacheSingleLinksCount(Integer id, Integer count){
        redisTemplate.opsForValue().set(id + "singleLinksCount", count);
    }

    Integer getCachedSingleLinksCount(Integer id){
        return (Integer)redisTemplate.opsForValue().get(id + "singleLinksCount");
    }
}

@Component
class ThreadPoolHelper{
    ExecutorService es;

    @PostConstruct
    private void init(){
        es = new ThreadPoolExecutor(5,
                20,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
