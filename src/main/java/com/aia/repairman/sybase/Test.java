package com.aia.repairman.sybase;

import cn.hutool.core.util.ReUtil;

public class Test {
    public static void main(String[] args) {

        if(13 == '\r' ){
            System.out.println(13);
        }

        String line = "*/";

        if (ReUtil.isMatch("^/\\*(.*?)\\*/$", line.trim())) {
//            commentText=commentText+"--"+line;
            System.out.println("match");
        }

        if (ReUtil.isMatch("(.*?)/\\*(.*?)\\*/$", line.trim())) {
            System.out.println("match2");
        }

        if (ReUtil.isMatch("/\\*(.*?)\\*/(.*?)$", line.trim())) {
            System.out.println("match3");
        }
    }
}
