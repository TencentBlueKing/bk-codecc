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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 灰度工具项目
 *
 * @version V1.0
 * @date 2020/12/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "灰度工具项目视图")
public class GrayToolProjectVO extends CommonVO {
    /**
     * 项目Id
     */
    @Schema(description = "项目Id", required = true)
    private String projectId;

    @Schema(description = "工具id（存储状态）")
    private String toolName;

    @Schema(description = "需灰度工具清单,注册灰度工具时必填入参")
    private List<String> toolNameList;

    /**
     * 项目灰度状态
     */
    @Schema(description = "项目灰度状态", required = true)
    private int status;

    /**
     * 项目灰度状态
     */
    @Schema(description = "是否开源治理项目", required = true)
    private boolean openSourceProject;

    /**
     * 配置参数
     */
    @Schema(description = "配置参数")
    private Map<String, Object> configureParam;

    /**
     * 接口人
     */
    @Schema(description = "接口人")
    private String projectOwner;

    /**
     * 原因
     */
    @Schema(description = "原因")
    private String reason;

    /**
     * 筛除机器创建项目(0:筛除 1:不筛除)
     */
    @Schema(description = "筛除机器创建项目")
    private Integer hasRobotTaskBool;
}
