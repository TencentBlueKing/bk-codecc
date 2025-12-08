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
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api;

import com.tencent.devops.common.api.annotation.I18NFieldMarker;
import com.tencent.devops.common.api.annotation.I18NModuleCode;
import com.tencent.devops.common.constant.ToolConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Set;

import lombok.EqualsAndHashCode;

/**
 * 工具完整信息对象
 *
 * @version V1.0
 * @date 2019/4/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "工具完整信息视图")
public class ToolMetaBaseVO extends CommonVO {
    /**
     * 标记发送该请求的来源, 默认来源: BKCI
     */
    @Schema(description = "请求来源")
    private String requestSource = ToolConstants.RegisterRequestSource.BKCI.getName();

    /**
     * 工具模型,LINT、COMPILE、TSCLUA、CCN、DUPC，决定了工具的接入、告警、报表的处理及展示类型
     */
    @Schema(description = "工具模型,决定了工具的接入、告警、报表的处理及展示类型")
    private String pattern;

    @Schema(description = "工具聚类跟踪方式")
    private String clusterType;

    /**
     * 工具名称，也是唯一KEY
     */
    @Schema(description = "工具名称，也是唯一KEY", required = true)
    @Pattern(regexp = "[A-Z]+", message = "工具名称，只能包含大写字母")
    private String name;

    /**
     * 工具的展示名
     */
    @Schema(description = "工具的展示名", required = true)
    @I18NFieldMarker(keyFieldHolder = "entityId", moduleCode = I18NModuleCode.TOOL_DISPLAY_NAME)
    private String displayName;

    /**
     * 工具类型，界面上展示工具归类：
     * 发现缺陷和安全漏洞、规范代码、复杂度、重复代码
     */
    @Schema(description = "工具类型", required = true)
    private String type;

    /**
     * 支持语言，通过位运算的值表示
     */
    @Schema(description = "支持语言，通过位运算的值表示")
    private long lang;

    /**
     * 根据项目语言来判断是否推荐该款工具,true表示推荐，false表示不推荐
     */
    @Schema(description = "根据项目语言来判断是否推荐该款工具,true表示推荐，false表示不推荐")
    private boolean recommend;

    /**
     * 状态：测试（T）、灰度（保留字段）、发布（P）、下架(D)， 注：测试类工具只有管理员可以在页面上看到，只有管理员可以接入
     */
    @Schema(description = "态：测试（T）、灰度（保留字段）、发布（P）、下架(D)， 注：测试类工具只有管理员可以在页面上看到，"
            + "只有管理员可以接入", allowableValues = "{T,P,D}")
    private String status;

    /**
     * 工具的个性参数，如pylint的Python版本，这个参数用json保存。
     * 用户在界面上新增参数，填写参数名，参数变量， 类型（单选、复选、下拉框等），枚举值
     */
    @Schema(description = "工具的个性参数")
    private String params;

    /**
     * 工具的自定义参数列表, params 的新版实现.
     * 后续接入的工具统一使用 tool_options 来实现自定义参数.
     * 字段具体说明见 CodeCC 工具开发规范
     */
    @Schema(description = "工具的自定义参数列表")
    private List<ToolOption> toolOptions;

    /**
     * 工具版本号
     */
    @Schema(description = "工具版本")
    private String toolVersion;

    /**
     * 最新的工具镜像版本（hash值）
     */
    @Schema(description = "工具镜像版本")
    private String toolImageRevision;

    /**
     * 工具版本列表，T-测试版本，G-灰度版本，P-正式发布版本
     */
    @Schema(description = "工具镜像版本")
    private List<ToolVersionVO> toolVersions;

    /**
     * 可以使用该工具的项目(的项目 id)
     */
    @Schema(description = "可使用该工具的项目")
    private Set<String> visibleProjects;

    /**
     * 可以使用该工具的组织(的组织 id, 以 tof 的数据为准)
     */
    @Schema(description = "可使用该工具的组织")
    private Set<String> visibleOrgIds;

    /**
     * 是否启用编译脚本输入框
     */
    @Schema(description = "是否启用编译脚本输入框")
    private Boolean scriptInputEnabled;

    @Schema(description = "租户id")
    private String tenantId;
}
