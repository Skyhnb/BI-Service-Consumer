package com.wxt.biconsumer.Controller.Neo4jController;

import com.wxt.biconsumer.Service.Neo4jService.Neo4jETLService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Api(tags = "Neo4jETL")
@RequestMapping("/Neo4jETL")
public class Neo4jETLController {
    @Resource
    Neo4jETLService neo4jETLService;

    @GetMapping("/test")
    @ApiOperation("test")
    public String test(){
        return neo4jETLService.test();
    }

    @GetMapping("/startETL/{start_num}")
    public String startEtl(@PathVariable int start_num){
        return neo4jETLService.startEtl(start_num);
    }
}
