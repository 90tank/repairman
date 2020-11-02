package com.aia.repairman.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aia.repairman.service.RepairmanService;
import com.aia.repairman.util.ClientResult;

import java.io.IOException;

@RestController
public class RepairmanController {

    @Autowired
    private RepairmanService repairmanService;


    @RequestMapping(value = "/db2/autoConvertBeforeFixedSql",method = RequestMethod.GET)
    public ClientResult handelConvertBeforeFixed(String driFile,String targetPath) {
    	ClientResult handelSql = repairmanService.handelConvertBeforeFixed(driFile,targetPath);
        return handelSql;
    }
    
    @RequestMapping(value = "/db2/autoConvertAfterFixedSql",method = RequestMethod.GET)
    public ClientResult handelConvertAfterFixed(String driFile,String targetPath) {
    	ClientResult handelSql = repairmanService.handelConvertAfterFixed(driFile,targetPath);
        return handelSql;
    } 
    @RequestMapping(value = "/db2/test",method = RequestMethod.GET)
    public String handelConvertAfterFixed1(String driFile,String targetPath) {
        return "hello world";
    } 
}
