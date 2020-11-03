package com.aia.repairman.controller;


import com.sun.org.glassfish.gmbal.ParameterNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aia.repairman.service.RepairmanService;
import com.aia.repairman.util.ClientResult;

import java.io.IOException;

@RestController
public class RepairmanController {

    @Autowired
    private RepairmanService repairmanService;


    @RequestMapping(value = "/db2/autoConvertBeforeFixedSql",method = RequestMethod.POST)
    public ClientResult handelConvertBeforeFixed(@RequestParam("dirFile")String dirFile, @RequestParam("targetPath")String targetPath) {
        System.out.println(dirFile);
        System.out.println(targetPath);
    	ClientResult handelSql = repairmanService.handelConvertBeforeFixed(dirFile,targetPath);
        return handelSql;
    }
    
    @RequestMapping(value = "/db2/autoConvertAfterFixedSql",method = RequestMethod.POST
    		)
    public ClientResult handelConvertAfterFixed(@RequestParam("sourceDirFile")String sourceDirFile,@RequestParam("targetPath")String targetPath) {
    	ClientResult handelSql = repairmanService.handelConvertAfterFixed(sourceDirFile,targetPath);
        return handelSql;
    } 
    @RequestMapping(value = "/db2/test",method = RequestMethod.GET)
    public String handelConvertAfterFixed1(String driFile,String targetPath) {
        return "hello world";
    } 
}
