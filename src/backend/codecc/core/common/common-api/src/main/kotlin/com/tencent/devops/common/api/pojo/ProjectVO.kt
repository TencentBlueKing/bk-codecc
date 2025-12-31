/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "蓝盾项目视图模型")
data class ProjectVO(
    @get:Schema(description = "主键ID")
    val id: Long,
    @get:Schema(description = "项目ID")
    val projectId: String,
    @get:Schema(description = "项目名称")
    val projectName: String,
    @get:Schema(description = "项目代码（蓝盾项目Id）")
    val projectCode: String,
    @get:Schema(description = "项目类型")
    val projectType: Int?,
    @get:Schema(description = "审批状态")
    val approvalStatus: Int?,
    @get:Schema(description = "审批时间")
    val approvalTime: String?,
    @get:Schema(description = "审批人")
    val approver: String?,
    @get:Schema(description = "cc业务ID")
    val ccAppId: Long?,
    @get:Schema(description = "cc业务名称")
    val ccAppName: String?,
    @get:Schema(description = "创建时间")
    val createdAt: String?,
    @get:Schema(description = "创建人")
    val creator: String?,
    @get:Schema(description = "数据ID")
    val dataId: Long?,
    @get:Schema(description = "部署类型")
    val deployType: String?,
    @get:Schema(description = "事业群ID")
    val bgId: String?,
    @get:Schema(description = "事业群名字")
    val bgName: String?,
    @get:Schema(description = "业务线ID")
    val businessLineId: String?,
    @get:Schema(description = "业务线名称")
    val businessLineName: String?,
    @get:Schema(description = "部门ID")
    val deptId: String?,
    @get:Schema(description = "部门名称")
    val deptName: String?,
    @get:Schema(description = "中心ID")
    val centerId: String?,
    @get:Schema(description = "中心名称")
    val centerName: String?,
    @get:Schema(description = "描述")
    val description: String?,
    @get:Schema(description = "英文缩写")
    val englishName: String,
    @get:Schema(description = "extra")
    val extra: String?,
    @get:Schema(description = "是否离线")
    val offlined: Boolean?,
    @get:Schema(description = "是否保密")
    val secrecy: Boolean?,
    @get:Schema(description = "是否启用图表激活")
    val helmChartEnabled: Boolean?,
    @get:Schema(description = "kind")
    val kind: Int?,
    val remark: String?,
    @get:Schema(description = "修改时间")
    val updatedAt: String?,
    @get:Schema(description = "修改人")
    val updator: String?,
    @get:Schema(description = "启用：true；停用：false")
    val enabled: Boolean?,
    @get:Schema(description = "是否灰度")
    val gray: Boolean,
    @get:Schema(description = "混合云CC业务ID")
    val hybridCcAppId: Long?,
    @get:Schema(description = "支持构建机访问外网")
    val enableExternal: Boolean?,
    @get:Schema(description = "支持IDC构建机")
    val enableIdc: Boolean? = false,
    @get:Schema(description = "流水线数量上限")
    val pipelineLimit: Int? = 500,
    @get:Schema(description = "项目路由指向")
    val routerTag: String?,
    @get:Schema(description = "运营产品ID")
    val productId: Int? = null
)
