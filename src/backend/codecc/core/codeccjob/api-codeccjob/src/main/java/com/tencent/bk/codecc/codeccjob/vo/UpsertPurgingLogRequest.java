package com.tencent.bk.codecc.codeccjob.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpsertPurgingLogRequest {

    private Long taskId;
    private Long delCount;

    private Long cost;

    private Boolean finalResult;
}
