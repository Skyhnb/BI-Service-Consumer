package com.wxt.biconsumer.Service.Neo4jService;

import com.wxt.biconsumer.Entity.Neo4jEntity.QueryEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "Neo4jQueryService", url = "${feign.Neo4j}/query")
public interface Neo4jQueryService {
    @PostMapping("/query_paths_by_two_nodes")
    List<List<Integer>> queryNodesPaths(@RequestBody QueryEntity entity);
}
