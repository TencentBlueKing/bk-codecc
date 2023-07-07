package com.tencent.bk.codecc.defect.model;

import lombok.Data;

/**
 * CCN 低于阈值的缺陷集合
 */
@Data
public class SummaryGatherInfo {
    private String fileName;
    private Integer defectCount;
    private Integer fileCount;

}
