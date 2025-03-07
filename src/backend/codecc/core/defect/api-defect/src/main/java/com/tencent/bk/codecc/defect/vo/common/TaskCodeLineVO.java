package com.tencent.bk.codecc.defect.vo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务的代码行信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCodeLineVO {
    /**
     * 空行数量
     */
    private Long sumBlank;
    /**
     * 代码行数量
     */
    private Long sumCode;
    /**
     * 空行数量
     */
    private Long sumComment;
    /**
     * 语言
     */
    private String language;
    /**
     * 语言值
     */
    private Long langValue;
    /**
     * 文件数
     */
    private Long fileNum;

}
