package com.wxt.biconsumer.Controller.Neo4jController;

import com.wxt.biconsumer.Entity.Neo4jEntity.QueryEntity;
import com.wxt.biconsumer.Service.Neo4jService.Neo4jQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Api(tags = "Neo4jQuery")
@RequestMapping("/Neo4jQuery")
public class Neo4jQueryController {
    @Resource
    Neo4jQueryService neo4jQueryService;

    @PostMapping("/query_paths_by_two_nodes")
    @ApiOperation("查询两节点间的路径")
    public List<List<Integer>> queryNodesPaths(@RequestBody QueryEntity entity){
        return neo4jQueryService.queryNodesPaths(entity);
    }
}
