package com.wxt.biconsumer.Service.MongoService;

import com.wxt.biconsumer.Entity.MongoEntity.EntityNode;
import com.wxt.biconsumer.Entity.MongoEntity.RelationById;
import com.wxt.biconsumer.Entity.ResponseFormat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.websocket.server.PathParam;
import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(name = "MongoQueryService", url = "${feign.Mongo}/query")
public interface MongoQueryService {
    @GetMapping("/getSingleLinksByName/{nodeName}")
    EntityNode getSingleLinkByName(@PathVariable("nodeName")String nodeName);

    @GetMapping("/getSingleLinksById/{uniqueId}")
    EntityNode getSingleLinksById(@PathVariable("uniqueId") int uniqueId);

    @GetMapping("/getSingleLinksByNamePageable/{nodeName}/{startFrom}/{limit}")
    EntityNode getSingleLinkByNamePageable(@PathVariable("nodeName")String nodeName, @PathVariable("startFrom")int startFrom, @PathVariable("limit")int limit);

    @GetMapping("/getSingleLinksByIdPageable/{uniqueId}/{startFrom}/{limit}")
    EntityNode getSingleLinksByIdPageable(@PathVariable("uniqueId") int uniqueId, @PathVariable("startFrom")int startFrom, @PathVariable("limit")int limit);

    @GetMapping("/getEntityNameById/{uniqueId}")
    String getEntityNameById(@PathVariable("uniqueId")int uniqueId);

    @GetMapping("/getEntityIdByName/{nodeName}")
    Integer getEntityIdByName(@PathVariable("nodeName")String nodeName);

    @GetMapping("/getBatch/{start}/{size}")
    List<EntityNode> getBatch(@PathVariable("start")int start, @PathVariable("size") int size);

    @PostMapping("/getBatchNamesByIds")
    List<EntityNode> getBatchNamesByIds(@RequestBody List<Integer> ids);

    @GetMapping("/getMaxUniqueId")
    Integer getMaxUniqueId();

    @PostMapping("/getBatchRelations")
    Map<Integer, Set<RelationById>> getBatchRelations(@RequestBody Map<Integer, List<Integer>> pairs);
}
