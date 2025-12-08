package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量指定测试信息视图
 *
 * @date 2024/03/28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "测试任务信息视图")
public class TestTaskInfoVO {

    private Integer defectCount;
    private Long totalCode;
    private Long totalLineCount;

}
