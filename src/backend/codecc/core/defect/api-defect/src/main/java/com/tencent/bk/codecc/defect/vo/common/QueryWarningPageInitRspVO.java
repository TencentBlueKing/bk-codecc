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

import com.tencent.bk.codecc.defect.vo.CheckerCustomVO;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 公共规则类型与作者视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@Schema(description = "公共规则类型与作者视图")
public class QueryWarningPageInitRspVO {
    /**
     * 代码规则展示维度
     */
    @Schema(description = "规则列表")
    private List<CheckerCustomVO> checkerList;

    @Schema(description = "作者清单")
    private Collection<String> authorList;

    /**
     * 代码缺陷展示数量
     */
    @Schema(description = "规则列表")
    private Map<String, Integer> checkerMap;

    @Schema(description = "作者列表")
    private Map<String, Integer> authorMap;



    @Schema(description = "文件路径树")
    private TreeNodeVO filePathTree;

    @Schema(description = "严重规则数")
    private int seriousCount;

    @Schema(description = "正常规则数")
    private int normalCount;

    @Schema(description = "提示规则数")
    private int promptCount;

    @Schema(description = "待修复告警数")
    private int existCount;

    @Schema(description = "已修复告警数")
    private int fixCount;

    @Schema(description = "已忽略告警数")
    private int ignoreCount;

    @Schema(description = "已屏蔽告警数")
    private int maskCount;

    @Schema(description = "新增文件数")
    private int newCount;

    @Schema(description = "历史文件数")
    private int historyCount;

    @Schema(description = "符合条件的告警总数")
    private int totalCount;

    /**
     * 操作相关
     */
    @Schema(description = "已提交TAPD单")
    private int tapdOpsCount;

    @Schema(description = "已标记处理")
    private int maskOpsCount;

    @Schema(description = "标记处理后任为问题")
    private int maskNotFixCount;

    @Schema(description = "已评论")
    private int commentOpsCount;

    @Schema(description = "无操作")
    private int notOpsCount;

}
