package com.wxt.biconsumer.Controller.MongoController;

import com.wxt.biconsumer.Entity.MongoEntity.ResponseFormat;
import com.wxt.biconsumer.Service.MongoService.MongoQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Api(tags = "MongoQuery")
@RequestMapping("/MongoQuery")
public class MongoQueryController {
    @Resource
    MongoQueryService mongoQueryService;

    @GetMapping("/getSingleLinksByName/{nodeName}")
    @ApiOperation("根据名字获取直接关联的点集合及关系")
    public ResponseFormat getSingleLinkByName(@PathVariable("nodeName")String nodeName){
        return mongoQueryService.getSingleLinkByName(nodeName);
    }

    @GetMapping("/getSingleLinksById/{uniqueId}")
    @ApiOperation("根据Id获取直接关联的点集合及关系")
    public ResponseFormat getSingleLinksById(@PathVariable("uniqueId") int uniqueId){
        return mongoQueryService.getSingleLinksById(uniqueId);
    }

    @GetMapping("/getEntityNameById/{uniqueId}")
    @ApiOperation("根据Id获取名字")
    public ResponseFormat getEntityNameById(@PathVariable("uniqueId")int uniqueId){
        return mongoQueryService.getEntityNameById(uniqueId);
    }

    @GetMapping("/getEntityIdByName/{nodeName}")
    @ApiOperation("根据名字获取Id")
    public ResponseFormat getEntityIdByName(@PathVariable("nodeName")String nodeName){
        return mongoQueryService.getEntityIdByName(nodeName);
    }

    @GetMapping("/getBatch/{start}/{size}")
    @ApiOperation("批量获取单跳关系")
    public ResponseFormat getBatch(@PathVariable("start")int start, @PathVariable("size") int size){
        return mongoQueryService.getBatch(start, size);
    }

    @GetMapping("/getBatchIds/{start}/{size}")
    @ApiOperation("批量获取Id")
    public ResponseFormat getBatchIds(@PathVariable("start")int start, @PathVariable("size") int size){
        return mongoQueryService.getBatchIds(start, size);
    }

    @GetMapping("/getMaxUniqueId")
    @ApiOperation("获取最大Id")
    public ResponseFormat getMaxUniqueId(){
        return mongoQueryService.getMaxUniqueId();
    }
}
