package com.tencent.bk.codecc.task.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流水线回调实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginErrorVO {

    private Long taskId;

    private String buildId;
}


