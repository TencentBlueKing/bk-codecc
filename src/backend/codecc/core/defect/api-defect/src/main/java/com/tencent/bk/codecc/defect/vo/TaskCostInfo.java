package com.tencent.bk.codecc.defect.vo;

import lombok.Data;

/**
 * 工具的扫描开销信息
 *
 * @date 2024/08/23
 */
@Data
public class TaskCostInfo {
    private Long costTime;
    private Integer defectCount;
    private Long codeCount;

}
