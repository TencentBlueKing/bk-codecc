package com.tencent.bk.codecc.defect.vo;

import lombok.Data;

import java.util.Map;

/**
 * 查询一次扫描中成功的灰度工具的信息
 *
 * @date 2024/08/23
 */
@Data
public class QuerySuccGrayToolInfoResVO {
    private Long taskId;
    private String buildId;
    // 【工具名】:【该工具在本次扫描中花费的时间, 扫出的告警数, 扫描的代码量】
    private Map<String, TaskCostInfo> info;
}
