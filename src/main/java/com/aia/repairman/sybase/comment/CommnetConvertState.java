package com.aia.repairman.sybase.comment;

/**
 * 注释转换工具 之 状态机状态枚举
 */
public enum CommnetConvertState {
    NULL_STATE("无状态", 0),
    BLOCK_COMMENT_STATE("块注释状态", 1), // /* 杠星注释
    SINGLE_LINE_COMMMENT_STATE("单行状态", 2), // -- 双斜杠注释
    STRING_STATE("字符串状态", 3),
    END_STATE("结束状态",4);

    private String state;
    private Integer value;

    CommnetConvertState(String name, Integer value) {
        this.state = name;
        this.value = value;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
