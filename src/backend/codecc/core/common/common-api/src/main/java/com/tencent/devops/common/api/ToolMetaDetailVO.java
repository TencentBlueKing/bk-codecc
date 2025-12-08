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
 * Permission is hereby granted, free of charge,
 * to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 工具完整信息对象
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工具完整信息对象")
public class ToolMetaDetailVO extends ToolMetaBaseVO {
    @Schema(description = "工具简介，一句话介绍语", required = true)
    private String briefIntroduction;

    @Schema(description = "工具描述，较详细的描述")
    private String description;

    @Schema(description = "是否公开：true表示私有、false或者空表示公开")
    private boolean privated;

    @Schema(description = "工具的图标")
    private String logo;

    @Schema(description = "工具的图文详情")
    private String graphicDetails;

    @Schema(description = "工具支持的代码语言列表", required = true)
    private List<String> supportedLanguages;

    @Schema(description = "docker启动运行的命令，命令由工具开发者提供，并支持带选项--json传入input.json", required = true)
    private String dockerTriggerShell;

    @Schema(description = "docker镜像存放URL，如：xxx.xxx.xxx.com/paas/public/tlinux2.2_codecc_tools", required = true)
    private String dockerImageURL;

    @Schema(description = "docker镜像版本", required = true)
    private String dockerImageVersion;

    @Schema(description = "docker镜像版本类型", required = true)
    private String dockerImageVersionType;

    @Schema(description = "工具外部docker镜像版本号，用于关联第三方直接提供的docker镜像版本")
    private String foreignDockerImageVersion;

    @Schema(description = "docker镜像仓库账号")
    private String dockerImageAccount;

    @Schema(description = "docker镜像仓库密码")
    private String dockerImagePasswd;

    @Schema(description = "调试流水线Id", required = true)
    private String debugPipelineId;

    @Schema(description = "工具bin目录路径")
    private String toolHomeBin;

    @Schema(description = "工具扫描命令")
    private String toolScanCommand;

    @Schema(description = "工具环境")
    private String toolEnv;

    @Schema(description = "工具运行类型：docker,local")
    private String toolRunType;

    @Schema(description = "工具历史版本号列表")
    private List<String> toolHistoryVersion;

    @Schema(description = "用户自定义关注的工具信息")
    private CustomToolInfo customToolInfo;

    @Schema(description = "工具在插件端处理工具输出告警的任务链")
    private List<String> processList;

    @Schema(description = "工具二进制相关信息")
    private Binary binary;

    @Schema(description = "工具是否需要 git diff 信息")
    private Boolean gitDiffRequired = false;

    @Data
    public static class Binary {

        @Schema(description = "win二进制的下载路径")
        private String winUrl;

        @Schema(description = "linux二进制的下载路径")
        private String linuxUrl;

        @Schema(description = "mac二进制的下载路径")
        private String macUrl;

        @Schema(description = "win环境下命令行")
        private String winCommand;

        @Schema(description = "linux环境下命令行")
        private String linuxCommand;

        @Schema(description = "mac环境下命令行")
        private String macCommand;

        @Schema(description = "二进制依赖环境")
        private List<ToolEnv> toolEnvs;

        @Schema(description = "二进制工具版本")
        private String binaryVersion;
    }

    @Data
    public static class ToolEnv {
        @Schema(description = "依赖的环境命令")
        private String dependBin;

        @Schema(description = "依赖的环境命令版本")
        private String dependVersion;
    }

    @Data
    public static class CustomToolInfo {
        @Schema(description = "工具上报关注参数")
        private Map<String, String> customToolParam;

        @Schema(description = "上报告警统计维度")
        private Map<String, String> customToolDimension;
    }
}
