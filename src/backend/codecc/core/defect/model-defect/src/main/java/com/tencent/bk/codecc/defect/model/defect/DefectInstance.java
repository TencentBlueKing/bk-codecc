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

package com.tencent.bk.codecc.defect.model.defect;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 告警实例的数据
 *
 * @version V1.0
 * @date 2022/3/14
 */
@Data
public class DefectInstance {
    /**
     * 有关引起告警的跟踪数据。可以有多个跟踪数据。
     */
    private List<Trace> traces;

    @Data
    public static class Trace {
        /**
         * 关联告警跟踪信息
         */
        @Field("link_trace")
        List<Trace> linkTrace;
        private String message;
        @Field("file_md5")
        private String fileMd5;
        @Field("file_pathname")
        private String filePath;
        private String tag;
        @Field("trace_number")
        private Integer traceNum;
        @Field("line_number")
        private int lineNum;
        @Field("start_column")
        private int startColumn;
        @Field("end_column")
        private int endColumn;
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
        private String kind;
    }
}
