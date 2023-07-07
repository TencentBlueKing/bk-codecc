package com.tencent.bk.codecc.defect.dto;

import lombok.Data;

@Data
public class IgnoreTypeNotifyTriggerModel {

    /**
     * 项目ID
     */
    private String projectId;
    /**
     * 名称
     */
    private String name;
    /**
     * 类型ID
     */
    private Integer ignoreTypeId;
    /**
     * 来源（sys ｜ project）
     */
    private String createFrom;

}
