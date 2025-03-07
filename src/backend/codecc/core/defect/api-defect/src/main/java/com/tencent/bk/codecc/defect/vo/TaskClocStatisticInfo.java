package com.tencent.bk.codecc.defect.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.N;

/**
 * 记录一个任务的 cloc 代码量统计信息
 *
 * @date 2024/04/23
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TaskClocStatisticInfo {

    private Long taskId;
    /**
     * 该任务所有语言的代码量总和
     */
    private Long totalCode;
    /**
     * 代码量最多的语言的代码量
     */
    private Long maxCode;
    /**
     * 代码量最多的语言
     */
    private String maxLang;
}
