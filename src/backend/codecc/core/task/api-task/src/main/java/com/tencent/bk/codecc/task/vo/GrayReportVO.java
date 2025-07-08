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

import lombok.Data;

import java.util.List;

@Data
public class GrayReportVO {

    private String toolName;

    private String codeccBuildId;

    private String buildNum;

    private List<TaskLogDataVo> logList;

    @Data
    public static class TaskLogDataVo {

        private Long elapseTime;

        private Long taskId;

        private Integer currStep;

        private Integer flag;

        private String buildId;
    }
}
