package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 忽略类型的报表详情视图
 *
 * @date 2022/7/15
 */
@Data
@NoArgsConstructor
@ApiModel("忽略类型的报表详情视图")
public class IgnoreTypeReportDetailVO {

    public IgnoreTypeReportDetailVO(String projectId, String projectName, Integer ignoreTypeId, String ignoreTypeName) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.ignoreTypeId = ignoreTypeId;
        this.ignoreTypeName = ignoreTypeName;
    }

    @ApiModelProperty("蓝盾项目id")
    private String projectId;

    @ApiModelProperty("蓝盾项目中文名")
    private String projectName;

    @ApiModelProperty("忽略类型名称")
    private String ignoreTypeName;

    @ApiModelProperty("忽略类型id")
    private Integer ignoreTypeId;

    @ApiModelProperty("用于跳转的url")
    private String urlRoot;

    @ApiModelProperty("总计忽略的任务数")
    private int taskIgnoreSum;

    @ApiModelProperty("总计忽略问题数")
    private Integer defectIgnoreSum;

    @ApiModelProperty("总计忽略的风险函数个数")
    private Integer ccnIgnoreSum;

    @ApiModelProperty("忽略问题所在任务分布")
    private List<IgnoreStatDetail> taskList;

    @ApiModelProperty("忽略问题忽略人分布")
    private List<IgnoreStatDetail> authorList;
}
