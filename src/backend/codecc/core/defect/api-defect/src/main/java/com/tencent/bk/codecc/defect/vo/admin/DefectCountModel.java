package com.tencent.bk.codecc.defect.vo.admin;

import lombok.Data;

/**
 * 统计告警数
 *
 * @version V1.0
 * @date 2021/6/1
 */

@Data
public class DefectCountModel {

    private long taskId;

    private int defectCount;

    private long sum;
}
