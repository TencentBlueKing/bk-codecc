package com.tencent.bk.codecc.defect.vo;

import lombok.Data;

@Data
public class CCNDefectGroupStatisticVO {
    /**
     * 状态：NEW(1), FIXED(2), IGNORE(8)
     */
    private int status;

    /**
     * 状态：严重(1), 一般(2), 提示(3)
     */
    private int severity;

    private Long lineUpdateTime;

    private String filePathName;

    private String checker;

    private String author;

    private int defectCount;
}
