package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 忽略统计详情
 *
 * @date 2022/7/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("忽略统计详情")
public class IgnoreStatDetail {

    public IgnoreStatDetail(Long taskId, String nameCn, String gitRepo) {
        this.taskId = taskId;
        this.nameCn = nameCn;
        this.gitRepo = gitRepo;
    }

    public IgnoreStatDetail(String name) {
        this.name = name;
    }

    @ApiModelProperty("任务id")
    private Long taskId;

    @ApiModelProperty("任务中文名")
    private String nameCn;

    @ApiModelProperty("代码库")
    private String gitRepo;

    @ApiModelProperty("跳转问题列表默认的工具维度")
    private String dimension;

    @ApiModelProperty("忽略问题数量")
    private Integer defectIgnoreCount;

    @ApiModelProperty("忽略风险函数数量")
    private Integer ccnIgnoreCount;

    @ApiModelProperty("告警处理人ID(忽略人)")
    private String name;

    public void addDefectIgnoreCount(int defectIgnoreCount) {
        if (null == this.defectIgnoreCount) {
            this.defectIgnoreCount = defectIgnoreCount;
        } else {
            this.defectIgnoreCount += defectIgnoreCount;
        }
    }

    public void addCcnIgnoreCount(int ccnIgnoreCount) {
        if (null == this.ccnIgnoreCount) {
            this.ccnIgnoreCount = ccnIgnoreCount;
        } else {
            this.ccnIgnoreCount += ccnIgnoreCount;
        }
    }
}
