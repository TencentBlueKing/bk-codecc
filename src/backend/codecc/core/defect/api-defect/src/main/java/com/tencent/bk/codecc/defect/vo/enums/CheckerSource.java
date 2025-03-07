package com.tencent.bk.codecc.defect.vo.enums;

/**
 * 规则来源
 *
 * @version V1.0
 * @date 2024/8/19
 */

public enum CheckerSource {

    DEFAULT("工具集成", "INTEGRATION"),

    CUSTOM("用户自定义", "CUSTOM");


    private String nameCn;
    private String nameEn;

    CheckerSource(String nameCn, String nameEn) {
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
