/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工具数量和任务数量VO
 *
 * @version V1.0
 * @date 2020/12/07
 */
@Data
@Schema(description = "工具、任务数量视图")
public class TaskAndToolCountScriptVO {
    @Schema(description = "日期")
    private String date;

    @Schema(description = "来源")
    private String createFrom;

    @Schema(description = "数量")
    private Long count;

    @Schema(description = "工具名")
    private String toolName;

    @Schema(description = "活跃数量")
    private Long activeCount;
}