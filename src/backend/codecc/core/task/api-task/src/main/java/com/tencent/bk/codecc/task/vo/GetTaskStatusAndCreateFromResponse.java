package com.tencent.bk.codecc.task.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTaskStatusAndCreateFromResponse {

    private int status;
    private String createFrom;
}
