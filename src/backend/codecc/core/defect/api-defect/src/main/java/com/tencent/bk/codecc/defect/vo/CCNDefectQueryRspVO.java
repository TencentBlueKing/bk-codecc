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

package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Page;

/**
 * 圈复杂度查询返回视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("圈复杂度查询返回视图")
public class CCNDefectQueryRspVO extends CommonDefectQueryRspVO {

    /**
     * 风险系数极高的个数
     */
    @ApiModelProperty("风险系数极高的个数")
    private long superHighCount;

    /**
     * 风险系数高的个数
     */
    @ApiModelProperty("风险系数高的个数")
    private long highCount;

    /**
     * 风险系数中的个数
     */
    @ApiModelProperty("风险系数中的个数")
    private long mediumCount;

    /**
     * 风险系数低的个数
     */
    @ApiModelProperty("风险系数低的个数")
    private long lowCount;

    @ApiModelProperty("待修复告警数")
    private long existCount;

    @ApiModelProperty("已修复告警数")
    private long fixCount;

    @ApiModelProperty("已忽略告警数")
    private long ignoreCount;

    @ApiModelProperty("已屏蔽告警数")
    private long maskCount;

    /**
     * 新增告警的个数
     */
    @ApiModelProperty("新增告警的个数")
    private long newDefectCount;

    /**
     * 历史告警的个数
     */
    @ApiModelProperty("历史告警的个数")
    private long historyDefectCount;

    /**
     * 告警总数
     */
    @ApiModelProperty("告警总数")
    private long totalCount;

    /**
     * 缺陷列表
     */
    @ApiModelProperty("缺陷列表")
    private Page<CCNDefectVO> defectList;

    /**
     * 圈复杂度阀值
     */
    @ApiModelProperty(value = "圈复杂度阀值")
    private int ccnThreshold;
}
