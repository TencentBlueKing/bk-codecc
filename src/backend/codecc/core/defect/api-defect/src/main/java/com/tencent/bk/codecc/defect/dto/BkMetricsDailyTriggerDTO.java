package com.tencent.bk.codecc.defect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 蓝盾度量统计触发实体类
 *
 * @date 2025/7/14
 * @version V1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BkMetricsDailyTriggerDTO {
    private String projectId;
    private String statisticsDate;
}
