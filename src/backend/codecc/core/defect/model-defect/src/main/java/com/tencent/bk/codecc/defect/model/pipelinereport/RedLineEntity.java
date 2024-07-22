package com.tencent.bk.codecc.defect.model.pipelinereport;

import com.tencent.bk.codecc.defect.model.RedLineMetaEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 质量红线数据实体类
 *
 * @version V1.0
 * @date 2019/12/19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "t_red_line")
public class RedLineEntity extends RedLineMetaEntity {

    /**
     * 构建ID
     */
    @Field("build_id")
    private String buildId;

    /**
     * 数据的值
     */
    @Field("value")
    private String value;

    /**
     * CodeCC任务ID
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 单工具，按规则标签统计的维度信息
     */
    @Field("dimension_by_checker")
    private Dimension dimensionByChecker;


    @Data
    public static class Dimension {

        /* =========代码缺陷========= */
        @Field("defect_new_prompt")
        private Integer defectNewPrompt;
        @Field("defect_new_normal")
        private Integer defectNewNormal;
        @Field("defect_new_serious")
        private Integer defectNewSerious;
        @Field("defect_history_prompt")
        private Integer defectHistoryPrompt;
        @Field("defect_history_normal")
        private Integer defectHistoryNormal;
        @Field("defect_history_serious")
        private Integer defectHistorySerious;


        /* =========代码规范========= */
        @Field("standard_new_prompt")
        private Integer standardNewPrompt;
        @Field("standard_new_normal")
        private Integer standardNewNormal;
        @Field("standard_new_serious")
        private Integer standardNewSerious;
        @Field("standard_history_prompt")
        private Integer standardHistoryPrompt;
        @Field("standard_history_normal")
        private Integer standardHistoryNormal;
        @Field("standard_history_serious")
        private Integer standardHistorySerious;


        /* =========安全漏洞========= */
        @Field("security_new_prompt")
        private Integer securityNewPrompt;
        @Field("security_new_normal")
        private Integer securityNewNormal;
        @Field("security_new_serious")
        private Integer securityNewSerious;
        @Field("security_history_prompt")
        private Integer securityHistoryPrompt;
        @Field("security_history_normal")
        private Integer securityHistoryNormal;
        @Field("security_history_serious")
        private Integer securityHistorySerious;
    }
}
