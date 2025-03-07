/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公共文件查询请求视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("公共文件查询请求视图")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "pattern", visible = true, defaultImpl = DefectQueryReqVO.class)
@JsonSubTypes({@JsonSubTypes.Type(value = SCADefectQueryReqVO.class, name = "SCA")
})
public class DefectQueryReqVO extends DefectQueryReqVOBase {

    @ApiModelProperty(value = "任务Id列表", required = true)
    private List<Long> taskIdList;

    @ApiModelProperty("工具名")
    private List<String> toolNameList;

    @ApiModelProperty("维度")
    private List<String> dimensionList;

    @ApiModelProperty(value = "查询模型，用于指定创建的实例类型")
    private String pattern = "DEFAULT";

    @ApiModelProperty("是否为恢复忽略再标记")
    private Boolean revertAndMark;

    @ApiModelProperty("查询的结果是否要插入到 t_ignored_negative_defect 表中")
    private Boolean needBatchInsert;

    @ApiModelProperty("是否需要过滤掉正在审核的告警")
    private Boolean needFilterApprovalDefect;

    @ApiModelProperty("操作")
    private List<String> operates;

    @ApiModelProperty("提单的ID列表， 默认为空")
    private Set<String> submitDefectIds;
}
