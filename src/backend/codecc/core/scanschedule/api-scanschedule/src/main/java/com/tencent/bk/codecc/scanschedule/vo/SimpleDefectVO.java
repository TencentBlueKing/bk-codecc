package com.tencent.bk.codecc.scanschedule.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("告警信息")
public class SimpleDefectVO {

    /**
     * scanId
     */
    private String scanId;

    /**
     * 告警作者
     */
    private String author;

    /**
     * 告警创建时间
     */
    private Long createTime;

    /**
     * 工具名称
     */
    private  String toolName;

    /**
     * 规则名称
     */
    @JsonProperty("checkerName")
    private String checker;

    /**
     * 规则严重等级
     */
    private  int severity;

    /**
     * 规则描述
     */
    @JsonProperty("description")
    private String message;

    /**
     * 代码行
     */
    @JsonProperty("line")
    private int lineNum;

}
