/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("蓝盾项目视图模型")
data class ProjectVO(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("项目名称")
    val projectName: String,
    @ApiModelProperty("项目代码（蓝盾项目Id）")
    val projectCode: String,
    @ApiModelProperty("项目类型")
    val projectType: Int?,
    @ApiModelProperty("审批状态")
    val approvalStatus: Int?,
    @ApiModelProperty("审批时间")
    val approvalTime: String?,
    @ApiModelProperty("审批人")
    val approver: String?,
    @ApiModelProperty("cc业务ID")
    val ccAppId: Long?,
    @ApiModelProperty("cc业务名称")
    val ccAppName: String?,
    @ApiModelProperty("创建时间")
    val createdAt: String?,
    @ApiModelProperty("创建人")
    val creator: String?,
    @ApiModelProperty("数据ID")
    val dataId: Long?,
    @ApiModelProperty("部署类型")
    val deployType: String?,
    @ApiModelProperty("事业群ID")
    val bgId: String?,
    @ApiModelProperty("事业群名字")
    val bgName: String?,
    @ApiModelProperty("业务线ID")
    val businessLineId: String?,
    @ApiModelProperty("业务线名称")
    val businessLineName: String?,
    @ApiModelProperty("部门ID")
    val deptId: String?,
    @ApiModelProperty("部门名称")
    val deptName: String?,
    @ApiModelProperty("中心ID")
    val centerId: String?,
    @ApiModelProperty("中心名称")
    val centerName: String?,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("英文缩写")
    val englishName: String,
    @ApiModelProperty("extra")
    val extra: String?,
    @ApiModelProperty("是否离线")
    val offlined: Boolean?,
    @ApiModelProperty("是否保密")
    val secrecy: Boolean?,
    @ApiModelProperty("是否启用图表激活")
    val helmChartEnabled: Boolean?,
    @ApiModelProperty("kind")
    val kind: Int?,
    val remark: String?,
    @ApiModelProperty("修改时间")
    val updatedAt: String?,
    @ApiModelProperty("修改人")
    val updator: String?,
    @ApiModelProperty("启用：true；停用：false")
    val enabled: Boolean?,
    @ApiModelProperty("是否灰度")
    val gray: Boolean,
    @ApiModelProperty("混合云CC业务ID")
    val hybridCcAppId: Long?,
    @ApiModelProperty("支持构建机访问外网")
    val enableExternal: Boolean?,
    @ApiModelProperty("支持IDC构建机")
    val enableIdc: Boolean? = false,
    @ApiModelProperty("流水线数量上限")
    val pipelineLimit: Int? = 500,
    @ApiModelProperty("项目路由指向")
    val routerTag: String?,
    @ApiModelProperty("运营产品ID")
    val productId: Int? = null
)
