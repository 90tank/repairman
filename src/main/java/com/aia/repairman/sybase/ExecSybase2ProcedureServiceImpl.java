package com.aia.repairman.sybase;

import cn.hutool.core.util.ReUtil;
import com.aia.repairman.sybase.service.ExecSybase2ProcedureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Service("sybase2ProcedureService")
@Slf4j
public class ExecSybase2ProcedureServiceImpl implements ExecSybase2ProcedureService {

    @Override
    public String handelFixed(String fileName) throws IOException {
        InputStream stream = null;
        BufferedReader br = null;
        String commentText="";
        String filePath="E:\\sqlfixed\\before\\comment\\"+fileName+".sql";
        try {
            stream = new FileInputStream(filePath);
            br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8.name()));
            String line;
            boolean findAnnotationSymbolFlag = false;
            List<String> sqlContent= new ArrayList<>();
            int count1=0;
            int count2=0;
            while ((line = br.readLine()) != null) {
                line+='\n';
                // 匹配类似这种注释    /*--- Selection: Phase 1, Subphase 1, agent, comm = 0.01 ---*/
                if (ReUtil.isMatch("^/\\*(.*?)\\*/$", line.trim())) {
                    commentText=commentText+"--"+line;
                    continue;
                }
                // 匹配类似这种注释    --- @agy_code = a.branch_agency,	/* 5/09/2013	Bernard Fung */
                if (ReUtil.isMatch("(.*?)/\\*(.*?)\\*/$", line.trim())) {
                    count1=count1+1;
                    count2=count2-1;
                    commentText=(count1+count2==0)? commentText+line:commentText+"--"+line;
                    continue;
                }
                if (ReUtil.isMatch("/\\*(.*?)\\*/(.*?)$", line.trim())) {
                    count1=count1+1;
                    count2=count2-1;
                    commentText=(count1+count2==0)? commentText+line:commentText+"--"+line;
                    continue;
                }
                //匹配以/*开始的
                if (ReUtil.isMatch("^/\\*(.*?)", line.trim()) && !ReUtil.isMatch("(.*?)\\*/$", line.trim())) {
                    findAnnotationSymbolFlag = true;
                    commentText=commentText+"--"+line;
                    count1=count1+1;
                    continue;
                }
                //匹配以*/结尾的
                if (!ReUtil.isMatch("^/\\*(.*?)", line.trim()) && ReUtil.isMatch("^(.*?)\\*/", line.trim())) {
                    count2=count2-1;
                    if (count1+count2==0){
                        findAnnotationSymbolFlag = false;
                    }
                    commentText=commentText+"--"+line;
                    continue;
                }
                // 单行注释
                if (ReUtil.isMatch("^[-](.*?)", line.trim())) {
                    commentText=commentText+"--"+line;
                    continue;
                }
                if (ReUtil.isMatch("^print(.*?)", line.trim())) {
                    commentText=commentText+"--"+line;
                    continue;
                }
                if (findAnnotationSymbolFlag) {
                    commentText=commentText+"--"+line;
                    continue;
                }else{
                    commentText=commentText+line;
                }
            }

        } catch (Exception e) {
            log.error("Failed to read the DDL file{}", e.getMessage(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error("BufferedReader Close the failure{}", e.getMessage(), e);
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("InputStream Close the failure{}", e.getMessage(), e);
                }
            }
        }
        log.info("commentText===="+commentText);
        sqlOutWritter(commentText,fileName);
        return commentText;
    }

    public void sqlOutWritter(String sql,String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("E:\\sqlfixed\\after\\comment\\"+fileName+".sql"));
        writer.write(sql);
        writer.close();
    }

}