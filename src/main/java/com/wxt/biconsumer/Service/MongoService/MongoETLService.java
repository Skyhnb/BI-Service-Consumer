package com.wxt.biconsumer.Service.MongoService;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "MongoETLService", url = "${feign.Mongo}/mongo")
public interface MongoETLService {

    @GetMapping("/test")
    String test();

    @GetMapping("/startETL/{batchSize}")
    String startETL(@PathVariable("batchSize")int batchSize, @RequestBody String source);

    @GetMapping("/startETLByDir/{batchSize}")
    String startETLByDir(@PathVariable("batchSize")int batchSize, @RequestBody String source);

    @GetMapping("/resetLinksId/{batchSize}")
    String resetLinksId(@PathVariable("batchSize")int batchSize);

}
