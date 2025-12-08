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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.vo;

import com.sun.org.apache.xpath.internal.operations.Bool;
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.CheckerSetPackageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务详细信息
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "任务详细信息")
public class TaskDetailVO extends TaskBaseVO {

    @Schema(description = "任务成员")
    private List<String> taskMember;

    /**
     * 已接入的所有工具名称，格式; COVERITY,CPPLINT,PYLINT
     */
    @Schema(description = "已接入的所有工具名称")
    private String toolNames;

    @Schema(description = "置顶标识")
    private Integer topFlag;

    /**
     * 项目接入的工具列表，查询时使用
     */
    @Schema(description = "项目接入的工具列表")
    private List<ToolConfigInfoVO> toolConfigInfoList;

    @Schema(description = "任务停用时间")
    private String disableTime;

    @Schema(description = "编译平台")
    private String compilePlat;

    @Schema(description = "运行平台")
    private String runPlat;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "蓝盾项目ID")
    private String projectId;

    @Schema(description = "蓝盾项目名称")
    private String projectName;

    @Schema(description = "事业群id")
    private int bgId;

    @Schema(description = "业务线id")
    private Integer businessLineId;

    @Schema(description = "部门id")
    private int deptId;

    @Schema(description = "中心id")
    private int centerId;

    @Schema(description = "组id")
    private int groupId;

    @Schema(description = "工作空间id")
    private long workspaceId;

    @Schema(description = "凭证管理的主键id")
    private String repoHashId;

    @Schema(description = "仓库别名")
    private String aliasName;

    @Schema(description = "分支名，默认为master")
    private String branch;

    @Schema(description = "代码库类型")
    private String scmType;

    @Schema(description = "代码库的最新版本号")
    private String repoRevision;

    @Schema(description = "将默认过滤路径放到任务实体对象下面")
    private List<String> defaultFilterPath;

    @Schema(description = "已添加的自定义过滤路径")
    private List<String> filterPath;

    @Schema(description = "code.yml自定义过滤路径")
    private List<String> testSourceFilterPath;

    @Schema(description = "code.yml自定义过滤路径")
    private List<String> autoGenFilterPath;

    @Schema(description = "code.yml自定义过滤路径")
    private List<String> thirdPartyFilterPath;

    @Schema(description = "路径白名单")
    private List<String> whitePaths;

    @Schema(description = "持续集成传递代码语言信息")
    private String devopsCodeLang;

    @Schema(description = "是否从工蜂创建")
    private Boolean gongfengFlag;

    @Schema(description = "工蜂项目id")
    private Integer gongfengProjectId;

    @Schema(description = "最近的commitId")
    private String gongfengCommitId;

    @Schema(description = "插件唯一标识")
    private String atomCode;

    /**
     * V5 版本新增字段
     */
    @Schema(description = "任务类型：流水线、服务创建、开源扫描、API触发")
    private String taskType;

    @Schema(description = "任务创建来源：当类型为 API触发 时这里代表创建来源的 appCode")
    private String createSource;

    /**
     * 持续集成传递工具信息
     */
    @Schema(description = "工具")
    private String devopsTools;

    @Schema(description = "工具特定参数")
    private List<ToolConfigParamJsonVO> devopsToolParams;

    /**
     * 目的: 在保证向后兼容的前提下, 升级工具自定义参数体系, 详见 TAPD 121208909
     * 如果 devopsToolParamVersion = v2, 则使用新的处理逻辑;
     * 否则还是走原来的那套逻辑
     */
    @Schema(description = "工具自定义参数体系版本")
    private String devopsToolParamVersion;

    @Schema(description = "编译类型")
    private String projectBuildType;

    @Schema(description = "编译命令")
    private String projectBuildCommand;

    @Schema(description = "操作系统类型")
    private String osType;

    @Schema(description = "构建环境")
    private Map<String, String> buildEnv;

    @Schema(description = "工具关联规则集")
    private List<ToolCheckerSetVO> toolCheckerSets;

    @Schema(description = "工具列表")
    private Set<String> toolSet;

    @Schema(description = "最近分析时间")
    private Long minStartTime;

    /*----------------新任务页面显示-------------------*/
    @Schema(description = "显示工具")
    private String displayToolName;

    @Schema(description = "当前步骤")
    private Integer displayStep;

    @Schema(description = "步骤状态")
    private Integer displayStepStatus;

    @Schema(description = "显示进度条")
    private Integer displayProgress;

    @Schema(description = "显示工具信息")
    private String displayName;

    @Schema(description = "是否回写工蜂")
    private Boolean mrCommentEnable;

    @Schema(description = "启用开源扫描规则集选择的语言")
    private List<String> languages;

    @Schema(description = "启用哪种规则集配置")
    private CheckerSetPackageType checkerSetType;

    /**
     * 是否是老插件切换为新插件，不对外接口暴露，仅用于内部逻辑参数传递
     */
    private boolean oldAtomCodeChangeToNew;

    /**
     * (谨供开源扫描注册用)是否强制更新项目规则集
     */
    private Boolean forceToUpdateOpenSource;

    /**
     * (谨供开源扫描注册用)配置项目规则集类型
     */
    private ComConstants.OpenSourceCheckerSetType openSourceCheckerSetType;

    /**
     * 是否扫描测试代码，true-扫描，false-不扫描，默认不扫描
     */
    private Boolean scanTestSource = false;

    @Schema(description = "自动语言识别")
    private Boolean autoLang;

    @Schema(description = "自动语言识别扫描标识")
    private Boolean autoLangScanFlag;

    @Schema(description = "是否新的自动识别语言方式")
    private Boolean newAutoLangScanFlag;

    @Schema(description = "检查规则集环境类型:preProd/prod")
    private String checkerSetEnvType;

    @Schema
    private Long totalSecurityDefectCount;

    @Schema
    private Long totalDefectCount;

    @Schema
    private Long totalStyleDefectCount;

    @Schema(description = "代码库总分")
    private double rdIndicatorsScore;

    @Schema(description = "是否按开源治理计分")
    private boolean openScan;

    @Schema(description = "仓库管理者")
    private List<String> repoOwner;

    @Schema(description = "流水线model的taskId")
    private String pipelineTaskId;

    @Schema(description = "流水线model的taskName")
    private String pipelineTaskName;

    @Schema(description = "数据是否迁移成功")
    private Boolean dataMigrationSuccessful;

    @Schema(description = "超时时间")
    private Integer timeout;

    @Schema(description = "是否开启缓存")
    private Boolean fileCacheEnable;

    @Schema(description = "最新BuildId")
    private String latestBuildId;

    @Schema(description = "测试任务的阶段号")
    private Integer testStage;

    @Schema(description = "测试工具名")
    private String testTool;

    @Schema(description = "测试版本号")
    private String testVersion;
}
