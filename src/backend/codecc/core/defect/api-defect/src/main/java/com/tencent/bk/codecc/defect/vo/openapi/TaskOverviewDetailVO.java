/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.vo.openapi;

import com.tencent.bk.codecc.defect.vo.report.CommonChartAuthorVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 任务概览详情视图
 *
 * @version V1.0
 * @date 2020/3/16
 */

@Data
@Schema(description = "任务概览详情视图")
public class TaskOverviewDetailVO
{
    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务英文名")
    private String nameEn;

    @Schema(description = "任务中文名")
    private String nameCn;

    @Schema(description = "任务所用语言")
    private String codeLang;

    @Schema(description = "任务拥有者")
    private List<String> taskOwner;

    @Schema(description = "蓝盾项目ID")
    private String projectId;

    @Schema(description = "事业群名称")
    private String bgName;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "中心名称")
    private String centerName;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "流水线ID")
    private String pipelineId;

    @Schema(description = "工具概览信息列表")
    private List<ToolDefectVO> toolDefectInfo;

    @Data
    public static class ToolDefectVO
    {
        @Schema(description = "工具名称")
        private String toolName;

        @Schema(description = "工具展示名称")
        private String displayName;

        @Schema(description = "遗留告警数")
        private Integer exist;

        @Schema(description = "修复告警数")
        private Integer closed;

        @Schema(description = "Cov遗留告警数")
        private CommonChartAuthorVO existCount;

        @Schema(description = "Cov修复告警数")
        private CommonChartAuthorVO closedCount;

        @Schema(description = "超高风险文件数量")
        private Integer superHighCount;

        @Schema(description = "高级别风险函数数量")
        private Integer highCount;

        @Schema(description = "中级别风险函数数量")
        private Integer mediumCount;

        @Schema(description = "低级别风险函数数量")
        private Integer lowCount;

        @Schema(description = "平均圈复杂度")
        private Float averageCcn;

        @Schema(description = "重复文件数")
        private Integer defectCount;

        @Schema(description = "代码重复率")
        private Float dupRate;

        @Schema(description = "新问题")
        private CommonChartAuthorVO newDefect;

        @Schema(description = "历史问题")
        private CommonChartAuthorVO historyDefect;
    }
}
