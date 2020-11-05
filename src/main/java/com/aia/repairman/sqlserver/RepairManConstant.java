package com.aia.repairman.sqlserver;

public class RepairManConstant {
    public static String CHAR_LENGTH_VARCHAR = "ALTER FUNCTION [s2ss].[char_length_varchar](@expression varchar(max))\n" +
            "RETURNS INT\n" +
            "AS\n" +
            "BEGIN\n" +
            "  RETURN CASE WHEN len(replace(@expression, ' ', '.')) = 0\n" +
            "\t          THEN 1\n" +
            "\t\t  ELSE len(replace(@expression, ' ', '.'))\n" +
            " \t   END\n" +
            "END\n";
    public static String HEADER="GO\r\n" + 
    		"\r\n" +  
    		"SET ANSI_NULLS OFF\r\n" + 
    		"GO\r\n" + 
    		"\r\n" + 
    		"SET QUOTED_IDENTIFIER ON\r\n" + 
    		"GO\n";
    
    public static synchronized String deleteProc(String procName) {
    	StringBuilder delStr=new StringBuilder();
    	delStr.append("IF EXISTS(select 1 from sysobjects where id=object_id('"+procName+"') and xtype='P')\n");
    	delStr.append("\tBEGIN\n");
    	delStr.append("\t\tdrop procedure "+procName+"\n");
    	delStr.append("\tEND\n");
    	delStr.append("GO\n");
    	return delStr.toString();
    }
}
