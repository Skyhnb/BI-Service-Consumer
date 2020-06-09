package com.wxt.biconsumer.Controller.MongoController;

import com.wxt.biconsumer.Service.MongoService.MongoETLService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@Api(tags = "MongoETL")
@RequestMapping("/MongoETL")
public class MongoETLController {
    @Resource
    MongoETLService mongoEtlService;

    @GetMapping("/test")
    @ApiOperation("test")
    public String test(){
        return mongoEtlService.test();
    }

    @GetMapping("/startETL/{batchSize}")
    @ApiOperation("")
    public String startETL(@PathVariable("batchSize")int batchSize, @RequestBody String source){
        return mongoEtlService.startETL(batchSize, source);
    }

    @GetMapping("/startETLByDir/{batchSize}")
    @ApiOperation("")
    public String startETLByDir(@PathVariable("batchSize")int batchSize, @RequestBody String source){
        return mongoEtlService.startETLByDir(batchSize, source);
    }

    @GetMapping("/resetLinksId/{batchSize}")
    @ApiOperation("")
    public String resetLinksId(@PathVariable("batchSize")int batchSize){
        return mongoEtlService.resetLinksId(batchSize);
    }
}
