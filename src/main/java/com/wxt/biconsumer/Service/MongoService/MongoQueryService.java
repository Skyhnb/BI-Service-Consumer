package com.wxt.biconsumer.Service.MongoService;

import com.wxt.biconsumer.Entity.MongoEntity.ResponseFormat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "MongoQueryService", url = "${feign.Mongo}/query")
public interface MongoQueryService {
    @GetMapping("/getSingleLinksByName/{nodeName}")
    ResponseFormat getSingleLinkByName(@PathVariable("nodeName")String nodeName);

    @GetMapping("/getSingleLinksById/{uniqueId}")
    ResponseFormat getSingleLinksById(@PathVariable("uniqueId") int uniqueId);

    @GetMapping("/getEntityNameById/{uniqueId}")
   ResponseFormat getEntityNameById(@PathVariable("uniqueId")int uniqueId);

    @GetMapping("/getEntityIdByName/{nodeName}")
    ResponseFormat getEntityIdByName(@PathVariable("nodeName")String nodeName);

    @GetMapping("/getBatch/{start}/{size}")
    ResponseFormat getBatch(@PathVariable("start")int start, @PathVariable("size") int size);

    @GetMapping("/getBatchIds/{start}/{size}")
    ResponseFormat getBatchIds(@PathVariable("start")int start, @PathVariable("size") int size);

    @GetMapping("/getMaxUniqueId")
    ResponseFormat getMaxUniqueId();
}
