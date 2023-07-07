package com.tencent.bk.codecc.defect.model.statistic;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 维度信息统计
 */
@Data
public class DimensionStatisticEntity {
    /* =========代码缺陷========= */

    // "新"待修复
    @Field("defect_new_count")
    private int defectNewCount;
    // 已修复
    @Field("defect_fix_count")
    private int defectFixCount;
    // 已屏蔽
    @Field("defect_mask_count")
    private int defectMaskCount;
    // 总待修复
    @Field("defect_total_count")
    private int defectTotalCount;

    /* =========代码规范========= */
    @Field("standard_new_count")
    private int standardNewCount;
    @Field("standard_fix_count")
    private int standardFixCount;
    @Field("standard_mask_count")
    private int standardMaskCount;
    @Field("standard_total_count")
    private int standardTotalCount;

    /* =========安全漏洞========= */
    @Field("security_new_count")
    private int securityNewCount;
    @Field("security_fix_count")
    private int securityFixCount;
    @Field("security_mask_count")
    private int securityMaskCount;
    @Field("security_total_count")
    private int securityTotalCount;
}
