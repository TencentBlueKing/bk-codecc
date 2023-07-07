package com.tencent.bk.codecc.defect.vo.redline;

import lombok.Data;

/**
 * 红线维度数据，根据规则标签统计
 */
@Data
public class RLDimensionVO {

    // 代码缺陷
    private Integer defectNewPrompt;
    private Integer defectNewNormal;
    private Integer defectNewSerious;
    private Integer defectHistoryPrompt;
    private Integer defectHistoryNormal;
    private Integer defectHistorySerious;
    private boolean defectInitialized;

    public void initDefect() {
        if (!this.defectInitialized) {
            this.defectNewPrompt = 0;
            this.defectNewNormal = 0;
            this.defectNewSerious = 0;
            this.defectHistoryPrompt = 0;
            this.defectHistoryNormal = 0;
            this.defectHistorySerious = 0;
            this.defectInitialized = true;
        }
    }


    // 代码规范
    private Integer standardNewPrompt;
    private Integer standardNewNormal;
    private Integer standardNewSerious;
    private Integer standardHistoryPrompt;
    private Integer standardHistoryNormal;
    private Integer standardHistorySerious;
    private boolean standardInitialized;

    public void initStandard() {
        if (!this.standardInitialized) {
            this.standardNewPrompt = 0;
            this.standardNewNormal = 0;
            this.standardNewSerious = 0;
            this.standardHistoryPrompt = 0;
            this.standardHistoryNormal = 0;
            this.standardHistorySerious = 0;
            this.standardInitialized = true;
        }
    }


    // 安全漏洞
    private Integer securityNewPrompt;
    private Integer securityNewNormal;
    private Integer securityNewSerious;
    private Integer securityHistoryPrompt;
    private Integer securityHistoryNormal;
    private Integer securityHistorySerious;
    private boolean securityInitialized;

    public void initSecurity() {
        if (!this.securityInitialized) {
            this.securityNewPrompt = 0;
            this.securityNewNormal = 0;
            this.securityNewSerious = 0;
            this.securityHistoryPrompt = 0;
            this.securityHistoryNormal = 0;
            this.securityHistorySerious = 0;
            this.securityInitialized = true;
        }
    }
}
