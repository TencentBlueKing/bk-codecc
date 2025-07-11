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

package com.tencent.bk.codecc.defect.model.pipelinereport;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工具快照实体类
 *
 * @version V1.0
 * @date 2019/6/27
 */
@Data
public class ToolSnapShotEntity
{
    /**
     * 工具中文名
     */
    @Field("tool_name_cn")
    @JsonProperty("tool_name_cn")
    private String toolNameCn;

    /**
     * 工具英文名
     */
    @Field("tool_name_en")
    @JsonProperty("tool_name_en")
    private String toolNameEn;

    /**
     * 工具告警详情页面
     */
    @Field("defect_detail_url")
    @JsonProperty("defect_detail_url")
    private String defectDetailUrl;

    /**
     * 工具报表页面
     */
    @Field("defect_report_url")
    @JsonProperty("defect_report_url")
    private String defectReportUrl;

    /**
     * 工具分析结果状态
     */
    @Field("result_status")
    @JsonProperty("result_status")
    private String resultStatus;

    /**
     * 工具分析结果状态描述
     */
    @Field("result_message")
    @JsonProperty("result_message")
    private String resultMessage;

}
