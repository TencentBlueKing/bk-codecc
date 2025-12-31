package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "忽略类型的报表详情视图")
public class IgnoreTypeReportDetailVO {

    public IgnoreTypeReportDetailVO(String projectId, String projectName, Integer ignoreTypeId, String ignoreTypeName) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.ignoreTypeId = ignoreTypeId;
        this.ignoreTypeName = ignoreTypeName;
    }

    @Schema(description = "蓝盾项目id")
    private String projectId;

    @Schema(description = "蓝盾项目中文名")
    private String projectName;

    @Schema(description = "忽略类型名称")
    private String ignoreTypeName;

    @Schema(description = "忽略类型id")
    private Integer ignoreTypeId;

    @Schema(description = "用于跳转的url")
    private String urlRoot;

    @Schema(description = "总计忽略的任务数")
    private int taskIgnoreSum;

    @Schema(description = "总计忽略问题数")
    private Integer defectIgnoreSum;

    @Schema(description = "总计忽略的风险函数个数")
    private Integer ccnIgnoreSum;

    @Schema(description = "忽略问题所在任务分布")
    private List<IgnoreStatDetail> taskList;

    @Schema(description = "忽略问题忽略人分布")
    private List<IgnoreStatDetail> authorList;
}
