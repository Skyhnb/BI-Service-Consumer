package com.wxt.biconsumer.Controller;

import com.alibaba.fastjson.JSON;
import com.wxt.biconsumer.Entity.Graph;
import com.wxt.biconsumer.Entity.Neo4jEntity.QueryEntity;
import com.wxt.biconsumer.Entity.ResponseFormat;
import com.wxt.biconsumer.Service.MongoService.MongoQueryService;
import com.wxt.biconsumer.Service.QueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.websocket.server.PathParam;

@RestController
@Api(tags = "Query")
@RequestMapping("/query")
public class QueryController {

    @Resource
    QueryService queryService;


    //要求(a)
    @GetMapping("/getSingleLinksByName/{nodeName}")
    @ApiOperation("根据名字获取直接关联的点集合及关系")
    public ResponseFormat getSingleLinkByName(@PathVariable("nodeName")String nodeName){
        Object res = queryService.getSingleLinkByName(nodeName);
        return wrap(res);
    }

    @GetMapping("/getSingleLinksById/{uniqueId}")
    @ApiOperation("根据Id获取直接关联的点集合及关系")
    public ResponseFormat getSingleLinksById(@PathVariable("uniqueId") int uniqueId){
        Object res = queryService.getSingleLinksById(uniqueId);
        return wrap(res);
    }

    //要求(a)
    @GetMapping("/getSingleLinksByNamePageable/{nodeName}/{startFrom}/{limit}")
    @ApiOperation("根据名字获取直接关联的点集合及关系")
    public ResponseFormat getSingleLinkByNamePageable(@PathVariable("nodeName")String nodeName, @PathVariable("startFrom")Integer startFrom, @PathVariable("limit")Integer limit){
        Object res = queryService.getSingleLinksByNamePageble(nodeName, startFrom, limit);
        return wrap(res);
    }

    @GetMapping("/getSingleLinksByIdPageable/{uniqueId}/{startFrom}/{limit}")
    @ApiOperation("根据Id获取直接关联的点集合及关系")
    public ResponseFormat getSingleLinksByIdPageable(@PathVariable("uniqueId") Integer uniqueId, @PathVariable("startFrom")Integer startFrom, @PathVariable("limit")Integer limit){
        Object res = queryService.getSingleLinksByIdPageble(uniqueId, startFrom, limit);
        return wrap(res);
    }


    //要求(b)
    @GetMapping("/getPathsByTwoNodes")
    @ApiOperation("查询两节点间的路径")
    public ResponseFormat getPathsByTwoNodes(@RequestParam("startNodeName") String startNodeName,
                                             @RequestParam("endNodeName") String endNodeName,           //终点UniqueId
                                             @RequestParam("skip_num") Integer skip_num,       //跳过的边数
                                             @RequestParam("limit_num") Integer limit_num,      //返回的边数
                                             @RequestParam("max_jump_num") Integer max_jump_num){   //最大跳数

        Graph graph = queryService.getPathsByTwoNodes(startNodeName, endNodeName, skip_num, limit_num, max_jump_num);
        return wrap(graph);
    }

    //要求(c)
    @GetMapping("/getSimilarityByTwoNodes")
    @ApiOperation("查询两节点的相似度")
    public ResponseFormat getSimilarityByTwoNodes(@RequestParam("name1") String name1,
                                                  @RequestParam("name2") String name2){

        Object res = queryService.getSimilarityByTwoNodes(name1, name2);
        return wrap(res);
    }

    private ResponseFormat wrap(Object o){
        ResponseFormat format = new ResponseFormat();
        if(o == null){
            format.setCode(-1);
            return format;
        }
        format.setCode(0);
        format.setData(o);
        return format;
    }
}
