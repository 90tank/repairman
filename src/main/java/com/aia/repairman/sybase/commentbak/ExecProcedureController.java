package com.aia.repairman.sybase.commentbak;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ExecProcedureController {

    @Autowired
    @Qualifier("sybase2ProcedureService")
    private ExecSybase2ProcedureService sybase2ProcedureService;


    @RequestMapping(value = "/db2/autoFixedSql",method = RequestMethod.GET)
    public String autoFixedSql2(String sqlFile) throws IOException {
        String handelSql = sybase2ProcedureService.handelFixed(sqlFile);
        return handelSql;

    }
}
