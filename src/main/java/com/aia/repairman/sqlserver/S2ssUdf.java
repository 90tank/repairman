package com.aia.repairman.sqlserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理系统转换时生成的s2Ss用户自定义函数
 *
 */
@Slf4j
public class S2ssUdf {
    public static String REG = "(.*)(s2ss\\.)(.*)(\\(.*\\))";


    /**
     * 处理自定函数
     * @param originDirPath 原始待处理文件目录
     * @param targetDir s2ss文件夹将要放置在的父路径
     * @param dbName
     */
    public void handleS2ss(String originDirPath, String targetDir, String dbName) {
        if (StringUtils.isEmpty(dbName)) {
            log.error("dbName is empty");
            return ;
        }

        File file = new File(originDirPath);
        File [] fs = file.listFiles();

        for (File tmpFile : fs) {
            checkS2ss(tmpFile, targetDir, dbName);
        }
    }

    public  static void checkS2ss(File handleFile, String targetDir, String dbName) {
        String pattern = REG;
        Pattern r = Pattern.compile(pattern);

        // 创建文件
        InputStream stream = null;
        BufferedReader bufferedReader = null;

        // TODO 传入的  DB name ？
        /*String filePath="D:\\IdeaProjects\\repairman\\src\\main\\resources\\static\\allDbFiles\\db_1\\include_s2ss_4test.sql";
        String dbDirPath = "D:\\IdeaProjects\\repairman\\src\\main\\resources\\static\\allDbFiles\\db_1";*/

        String dbDirPath = targetDir;

        Map<String,String> funcMap = new HashMap<>();
        try {
            stream = new FileInputStream(handleFile);
            bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8.name()));

            String tmpLine ="";
            while ((tmpLine = bufferedReader.readLine()) != null) {
                Matcher m = r.matcher(tmpLine);
                if (m.find()) {
                    String funcName = m.group(3);
                    log.info(m.group(0)); // 打印匹配行
                    log.info(funcName); // 打印方法名

                    funcMap.put(funcName,funcName);
                }
            }
            // 文件读完之后,根据funcMap创建UDF文件，如果存在则不创建表示已经处理过该类型的函数
            bufferedReader.close();
            createUdfByMap(funcMap,dbDirPath,dbName);
        } catch (FileNotFoundException e) {
            log.error("file not found ",e);
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException",e);
        } catch (IOException e) {
            log.error("IOException",e);
        }
    }


    /**
     * 创建s2Ss 函数文件
     * @param funcMap
     * @param dbFilePath 以DB区分的文件夹名称  （每个DB下面是各自要处理或转换完成的的sqlserver ddl文件）
     * @return
     */
    private static List<String> createUdfByMap(Map<String, String> funcMap, String dbFilePath, String dbName) throws IOException {
        List<String> resultList = new ArrayList<>();
        if(funcMap.isEmpty()) {
            return resultList;
        }

        File file = new File(dbFilePath);
        if(!file.isDirectory()) {
            log.error(dbFilePath+"is not dir");
            return resultList;
        }

        String s2ssDirPath = dbFilePath+File.separator+"s2ss";
        File s2ssDir = new File(s2ssDirPath);

        if(!s2ssDir.exists()) {
            s2ssDir.mkdir();
        }

        // 文件夹下原有的已定义好的function ,文件名即function name
        String[] udfNames = s2ssDir.list();
        Set<String> set = new HashSet<String>(Arrays.asList(udfNames));

        funcMap.forEach((key,value) -> {
            // DB DIR -- > s2ss DIR--> FUNC FILES
            // 先检查本DB文件夹下是否有该func文件 约定其文件命名即为func名称

            // 不存在 则写入
            if (set.add(key)) {
                System.out.println("key:-->"+key);
                System.out.println("value:-->"+value);
                // 创建文件
                String outPutS2ssFuncPath = s2ssDirPath + File.separator + key + ".sql";
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new FileWriter(outPutS2ssFuncPath, false));

                    // 判断写入类型
                    if (key.equalsIgnoreCase("char_length_varchar")) {
                        writer.write("USE "+dbName + "\r\n");
                        writer.write("GO " + "\r\n");
                        writer.write("SET ANSI_NULLS ON\n" +
                                "GO\n" +
                                "SET QUOTED_IDENTIFIER ON\n" +
                                "GO");
                        writer.write(RepairManConstant.CHAR_LENGTH_VARCHAR); //写入函数内容到文件
                    } else {
                        log.error("need function content /logic : "+ key);
                    }

                    writer.flush();//清空缓冲区数据
                    writer.close();//关闭读写流
                    System.out.println("写入成功");
                } catch (IOException e) {
                    log.error("IOException when print s2ss to file",e);
                }

                resultList.add(key);
            }
        });
        return resultList;
    }
}
