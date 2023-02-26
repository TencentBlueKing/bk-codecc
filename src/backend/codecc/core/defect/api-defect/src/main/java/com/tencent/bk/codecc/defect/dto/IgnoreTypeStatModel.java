package com.tencent.bk.codecc.defect.dto;

import java.util.Set;
import lombok.Data;

/**
 * 按忽略类型统计告警数DTO
 *
 * @date 2022/7/18
 */
@Data
public class IgnoreTypeStatModel {

    private long taskId;

    private Set<Long> taskIdSet;

    private String ignoreAuthor;

    private Integer ignoreTypeId;

    private int defectCount;
}
