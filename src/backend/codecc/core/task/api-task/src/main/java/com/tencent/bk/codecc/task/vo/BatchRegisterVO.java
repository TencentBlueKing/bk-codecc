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

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * 蓝盾批量接入请求体
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "批量接入请求体")
public class BatchRegisterVO extends CommonVO
{

    /**
     * 任务id
     */
    @Min(value = 10000L, message = "任务主键数值不正确")
    @Schema(description = "任务主键id", required = true)
    private long taskId;

    /**
     * 代码库hashid
     */
    @Schema(description = "代码库hashid", required = true)
    private String repoHashId;

    /**
     * 代码库别名
     */
    @Schema(description = "代码库别名", required = true)
    private String aliasName;

    /**
     * 代码库路径
     */
    @Schema(description = "代码库路径", required = true)
    private String repositoryUrl;

    /**
     * scm类型
     */
    @Schema(description = "scm类型", required = true)
    private String scmType;

    /**
     * 分支名
     */
    @Schema(description = "分支名", required = true)
    private String branch;

    /**
     * 操作系统各类型
     */
    @Schema(description = "操作系统类型", required = true)
    private String osType;

    /**
     * 构建环境
     */
    @Schema(description = "构建环境", required = true)
    private Map<String, String> buildEnv;

    /**
     * 构建脚本类型
     */
    @Schema(description = "构建脚本类型", required = true)
    private String projectBuildType;

    /**
     * 脚本内容
     */
    @Schema(description = "脚本内容", required = true)
    private String projectBuildCommand;

    /**
     * 扫描类型
     */
    @Schema(description = "扫描类型", required = true)
    private String scanType;

    /**
     * 工具清单
     */
    @Schema(description = "工具清单", required = true)
    private List<ToolConfigInfoVO> tools;

    /**
     * 代码库用户名凭证
     */
    @Schema(description = "用户名", required = false)
    private String userName;

    /**
     * 密码
     */
    @Schema(description = "密码", required = false)
    private String passWord;
}
