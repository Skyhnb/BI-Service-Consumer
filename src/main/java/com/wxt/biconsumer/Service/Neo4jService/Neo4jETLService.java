package com.wxt.biconsumer.Service.Neo4jService;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "Neo4jETLService", url = "${feign.Neo4j}/etl")
public interface Neo4jETLService {

    @GetMapping("/test")
    String test();

    @GetMapping("/startETL/{start_num}")
    String startEtl(@PathVariable int start_num);
}
