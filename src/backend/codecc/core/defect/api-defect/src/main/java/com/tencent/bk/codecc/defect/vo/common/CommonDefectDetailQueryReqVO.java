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

package com.tencent.bk.codecc.defect.vo.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectDetailQueryReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;


/**
 * 公共告警详情查询请求视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@Schema(description = "公共告警详情查询请求视图")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "pattern", visible = true, defaultImpl = CommonDefectDetailQueryReqVO.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LintDefectDetailQueryReqVO.class, name = "LINT"),
        @JsonSubTypes.Type(value = SCADefectDetailQueryReqVO.class, name = "SCA")
})
public class CommonDefectDetailQueryReqVO {

    @Schema(description = "告警主键id", required = true)
    private String entityId;

    @Schema(description = "工具名", required = true)
    private String toolName;

    @Schema(description = "工具维度", required = true)
    private String dimension;

    @Schema(description = "工具模型", required = true)
    @NotNull(message = "工具模型不能为空")
    private String pattern;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "构建Id")
    private String buildId;

    @Schema(description = "流模式响应")
    private Boolean stream = false;

    @Schema(description = "是否刷新缓存")
    private Boolean flushCache = false;
}
