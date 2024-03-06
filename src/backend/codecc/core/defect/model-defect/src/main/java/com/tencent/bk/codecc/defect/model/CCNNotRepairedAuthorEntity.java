package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 圈复杂度处理人信息统计
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CCNNotRepairedAuthorEntity extends RiskNotRepairedAuthorEntity {

    /**
     * 低风险级别告警数
     */
    @Field("low_count")
    private int lowCount;
}
