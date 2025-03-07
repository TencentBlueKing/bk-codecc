package com.tencent.bk.codecc.defect.vo.enums;

public enum CheckerGranularityType {
    FILE("文件", "FILE"),
    FUNCTION("函数", "FUNCTION"),
    PROJECT("项目", "PROJECT"),
    SINGLE_LINE("单行", "SINGLE_LINE"),
    TRACE_LINK("追踪链接", "TRACE_LINK");


    private String nameCn;
    private String nameEn;

    CheckerGranularityType(String nameCn, String nameEn) {
        this.nameCn = nameCn;
        this.nameEn = nameEn;
    }

    public String getNameCn() {
        return this.nameCn;
    }

    public String getNameEn() {
        return this.nameEn;
    }
}
