/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 告警实例的数据
 */
@Data
@ApiModel("告警实例的数据")
public class DefectInstanceVO {
    /**
     * 有关引起告警的跟踪数据。可以有多个跟踪数据。
     */
    @ApiModelProperty(value = "告警涉及的相关文件信息。可以有多个。", required = true)
    private List<Trace> traces;

    @Data
    @ApiModel("告警跟踪数据视图")
    public static class Trace {
        @ApiModelProperty(value = "报错行的描述信息", required = true)
        private String message;

        @ApiModelProperty(value = "文件MD5与文件路径名共同唯一标志一个文件", required = true)
        private String fileMd5;

        @ApiModelProperty(value = "文件路径名", required = true)
        private String filePath;

        @ApiModelProperty(value = "告警出现（实例）的简短标识符。用在 UI 中")
        private String tag;

        @ApiModelProperty(value = "跟踪时间序号", required = true)
        private Integer traceNum;

        @ApiModelProperty(value = "行号", required = true)
        private int lineNum;

        @ApiModelProperty(value = "开始列号")
        private Integer startColumn;

        @ApiModelProperty(value = "结束列号")
        private int endColumn;

        @ApiModelProperty(value = "是否是告警的主事件，告警也可能不存在主事件")
        private boolean main;

        /**
         * 事件类型:
         * MODEL：       与函数调用对应。在 Coverity Connect 中，模型事件显示在“显示详情”(Show Details) 链接旁边。
         * -----------------------------------------------------------------------------------------------------
         * PATH：        标识软件问题发生所需的 conditional 分支和决定。
         * 示例：Condition !p, taking false branch
         * Related lines 107-108 of sample code: 107 if (!p) 108 return NO_MEM;
         * -----------------------------------------------------------------------------------------------------
         * MULTI：       提供支持检查器发现的软件问题的源代码中的证据。也称为证据事件。
         * -----------------------------------------------------------------------------------------------------
         * NORMAL：      引用被标识为检查器发现的软件问题的引起因素的代码行。
         * 示例：
         * 1. alloc_fn: Storage is returned from allocation function malloc.
         * 2. var_assign: Assigning: p = storage returned from malloc(12U)
         * Related line 5 of sample code: 5 char *p = malloc(12);
         * -----------------------------------------------------------------------------------------------------
         * REMEDIATION： 提供旨在帮助您修复报告的软件问题的补救建议，而不只是报告问题。用在安全缺陷中。
         */
        @ApiModelProperty(value = "事件类型")
        private String kind;

        /**
         * 关联告警跟踪信息
         */
        @ApiModelProperty(value = "关联告警跟踪信息")
        List<Trace> linkTrace;
    }
}
