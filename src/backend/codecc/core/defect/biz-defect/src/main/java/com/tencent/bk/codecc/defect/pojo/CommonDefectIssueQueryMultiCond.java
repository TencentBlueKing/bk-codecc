package com.tencent.bk.codecc.defect.pojo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/**
 * 查询Common IssueList 的查询条件封装类
 */
@Data
@AllArgsConstructor
public class CommonDefectIssueQueryMultiCond {
    /**
     * 构建ID列表
     */
    private String buildId;
    /**
     * 缺陷ID列表
     */
    private Set<String> defectIds;
    /**
     * 规则列表
     */
    private Set<String> checkers;
    /**
     * 作者列表
     */
    private String author;
    /**
     * 路径匹配列表
     */
    private Set<String> filePathRegexes;
    /**
     * 开始时间查询的起始点
     */
    private Long startCreateTime;
    /**
     * 开始时间查询的结束点
     */
    private Long endCreateTime;
    /**
     * 修复时间查询的起始点
     */
    private Long startFixTime;
    /**
     * 修复时间查询的结束点
     */
    private Long endFixTime;
    /**
     * 状态列表
     */
    private Set<Integer> statuses;
    /**
     * 严重性列表
     */
    private Set<Integer> severities;
    /**
     * 忽略类型ID
     */
    private Set<Integer> ignoreReasonTypes;
}
