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
 *  Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.devops.common.api.CodeRepoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 告警配置详情视图
 *
 * @version V1.0
 * @date 2019/5/20
 */
@Data
@Schema(description = "告警配置详情视图")
public class AnalyzeConfigInfoVO {
    @Schema(description = "任务id")
    @JsonProperty("task_id")
    private long taskId;

    @Schema(description = "任务英文名")
    @JsonProperty("stream_name")
    private String nameEn;

    @Schema(description = "代码库路径")
    private String url;

    @Schema(description = "代码库密码")
    @JsonProperty("password")
    private String passWord;

    @Schema(description = "多工具类型")
    @JsonProperty("tool_name")
    private String multiToolType;

    @Schema(description = "过滤路径")
    @JsonProperty("skip_paths")
    private String skipPaths;

    @Schema(description = "屏蔽规则")
    @JsonProperty("skip_checkers")
    private String skipCheckers;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "代码库类型", allowableValues = "{svn,git}")
    @JsonProperty("scm_type")
    private String scmType;

    @Schema(description = "鉴权方式", allowableValues = "{http,ssh}")
    @JsonProperty("cert_type")
    private String certType;

    @Schema(description = "git分支")
    @JsonProperty("git_branch")
    private String gitBranch;

    @Schema(description = "oAuth的access token")
    @JsonProperty("access_token")
    private String accessToken;

    @Schema(description = "ssh鉴权方式的私钥")
    @JsonProperty("ssh_private_key")
    private String sshPrivateKey;

    @Schema(description = "扫描类型0:全量扫描,1:增量扫描,2:diff模式", allowableValues = "{0,1,2}")
    @JsonProperty("scan_type")
    private Integer scanType;

    @Schema(description = "项目接口人")
    @JsonProperty("proj_owner")
    private String projOwner;

    @Schema(description = "工具打开的规则")
    @JsonProperty("open_checkers")
    private List<OpenCheckerVO> openCheckers;

    @Schema(description = "工具特殊参数")
    private List<ToolOptions> toolOptions;

    @Schema(description = "platform ip")
    private String platformIp;

    @Schema(description = "Coverity规则子选项")
    private List<String> covOptions;

    @Schema(description = "Coverity编译规则")
    private String covPWCheckers;

    @Schema(description = "上次扫描的代码仓库列表")
    private List<CodeRepoVO> lastCodeRepos;

    @Schema(description = "本次扫描的代码仓库列表")
    private List<CodeRepoVO> codeRepos;

    @Schema(description = "本次扫描拉取的代码仓库子路径")
    private List<String> repoRelativePathList;

    @Schema(description = "任务语言")
    private Long language;

    @Schema(description = "任务语言")
    private List<String> languageStrList;

    @Schema(description = "任务管理员")
    private List<String> admins;

    @Schema(description = "基础构建Id")
    private String baseBuildId;

    @Schema(description = "上一次执行时间")
    private Long lastExecuteTime;

    /**
     * 工具的个性化参数，专门用来给查询规则列表使用的，不在对外接口暴露
     */
    private String paramJson;

    @Schema(description = "代码库信息")
    @JsonProperty("repo_url_map")
    private Map<String, String> repoUrlMap;

    @Schema(description = "是否只扫路径白名单，true:只扫路径白名单，false:全都扫")
    private Boolean onlyScanWhitePath;

    // ==============================用于调用getBuildInfo时传递参数，无其他作用 begin===================================
    private String buildId;

    /**
     * 代码仓库列表
     */
    private List<String> repoIds;

    /**
     * 扫描白名单列表
     */
    private List<String> repoWhiteList;

    private String atomCode;
    /**
     * 用于 WOODPECKER_COMMITSCAN 工具增量扫描
     */
    private Long commitSince;
    //版本
    private Integer version;

    // ==============================用于调用getBuildInfo时传递参数，无其他作用 end===================================
    @Data
    @Schema(description = "工具特殊参数")
    public static class ToolOptions {

        @Schema(description = "参数名称")
        private String optionName;

        @Schema(description = "参数值")
        private String optionValue;

        @Schema(description = "操作系统类型")
        private String osType;

        @Schema(description = "构建环境")
        private Map<String, String> buildEnv;

    }
}
