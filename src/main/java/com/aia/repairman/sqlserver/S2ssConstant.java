package com.aia.repairman.sqlserver;

public class S2ssConstant {
    public static String CHAR_LENGTH_VARCHAR = "ALTER FUNCTION [s2ss].[char_length_varchar](@expression varchar(max))\n" +
            "RETURNS INT\n" +
            "AS\n" +
            "BEGIN\n" +
            "  RETURN CASE WHEN len(replace(@expression, ' ', '.')) = 0\n" +
            "\t          THEN 1\n" +
            "\t\t  ELSE len(replace(@expression, ' ', '.'))\n" +
            " \t   END\n" +
            "END";
}
