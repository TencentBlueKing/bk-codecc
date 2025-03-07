package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务扫描代码统计信息
 *
 * @date 2024/04/22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("任务扫描代码统计信息")
public class ClocStatisticInfoVO {

    private Long taskId;
    private String language;
    private Long sumBlank;
    private Long sumCode;
    private Long sumComment;

}
