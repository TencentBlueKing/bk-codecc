/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 任务的基本信息
 *
 * @version V1.0
 * @date 2019/4/30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "任务的基本信息")
public class TaskBaseVO extends CommonVO {
    @Schema(description = "任务主键id", required = true)
    private long taskId;

    @Schema(description = "任务英文名", required = true)
    @Pattern(regexp = "^[0-9a-zA-Z_]{1,50}$", message = "输入的英文名称不符合命名规则")
    private String nameEn;

    @Schema(description = "任务中文名", required = true)
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,50}", message = "输入的中文名称不符合命名规则")
    private String nameCn;

    @Schema(description = "项目ID", required = true)
    private String projectId;

    @Schema(description = "项目名称", required = true)
    private String projectName;

    @Schema(description = "流水线ID", required = true)
    private String pipelineId;

    @Schema(description = "流水线一对多标识", required = false)
    private String multiPipelineMark;

    @Schema(description = "流水线名称", required = true)
    private String pipelineName;

    @Schema(description = "代码语言", required = true)
    private Long codeLang;

    @Schema(description = "任务负责人", required = true)
    private List<String> taskOwner;

    @Schema(description = "任务状态", required = true)
    private Integer status;

    @Schema(description = "创建来源", required = true)
    private String createFrom;

    @Schema(description = "定时任务执行时间")
    private String executeTime;

    @Schema(description = "定时任务执行时间")
    private List<String> executeDate;

    @Schema(description = "扫描方式0：全量；1：增量")
    private Integer scanType;

    @Schema(description = "启用的工具列表", required = true)
    private List<ToolConfigBaseVO> enableToolList;

    @Schema(description = "停用的工具列表", required = true)
    private List<ToolConfigBaseVO> disableToolList;

    @Schema(description = "新告警判定设置", required = true)
    private NewDefectJudgeVO newDefectJudge;

    @Schema(description = "通知配置信息")
    private NotifyCustomVO notifyCustomInfo;

    @Schema(description = "任务关联的规则集列表", required = true)
    @NotNull(message = "规则集列表不能为空")
    @Size(min = 1, message = "规则集列表不能为空")
    private List<CheckerSetVO> checkerSetList;

    /**
     * 原子插件码，
     * 旧插件: 值为空
     * 新插件: 值为CodeccCheckAtom
     */
    @Schema(description = "原子插件码")
    private String atomCode;

    @Schema(description = "规则集数目")
    private String checkerSetName;

    @Schema(description = "规则数目")
    private Long checkerCount;

    @Schema(description = "个性化项目视图")
    private CustomProjVO customProjInfo;

    @Schema(description = "任务扫描的代码库配置信息")
    private TaskCodeLibraryVO codeLibraryInfo;

    @Schema(description = "是否可以运行")
    private Boolean hasNoPermission;
    

    @Schema(description = "是否禁止页面忽略告警")
    private Boolean prohibitIgnore;

    @Schema
    private Integer cleanIndex;
}
