package com.tencent.bk.codecc.defect.pojo.statistic;

import lombok.Data;

/**
 * 根据规则标签统计维度相关
 */
@Data
public class DimensionStatisticModel {
    /* =========代码缺陷========= */
    // 新增待修复
    private int defectNewCount;
    // 已修复
    private int defectFixCount;
    // 已屏蔽
    private int defectMaskCount;
    // 总待修复
    private int defectTotalCount;

    /* =========代码规范========= */
    private int standardNewCount;
    private int standardFixCount;
    private int standardMaskCount;
    private int standardTotalCount;

    /* =========安全漏洞========= */
    private int securityNewCount;
    private int securityFixCount;
    private int securityMaskCount;
    private int securityTotalCount;
}
