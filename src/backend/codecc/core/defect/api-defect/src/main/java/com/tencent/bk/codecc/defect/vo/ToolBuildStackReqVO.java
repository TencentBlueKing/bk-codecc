package com.tencent.bk.codecc.defect.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import lombok.NoArgsConstructor;

/**
 * 设置强制全量扫描标志请求体
 *
 * @version V1.0
 * @date 2020/3/10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolBuildStackReqVO {

    private String landunBuildId;
    private List<String> toolNames;
    private String toolName;
    private Long commitSince;
    private Integer scanType;
}
