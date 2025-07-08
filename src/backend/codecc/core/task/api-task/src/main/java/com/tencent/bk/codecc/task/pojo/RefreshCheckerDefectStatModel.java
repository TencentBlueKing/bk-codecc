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

package com.tencent.bk.codecc.task.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计规则告警数入参model
 *
 * @version V1.0
 * @date 2020/11/17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshCheckerDefectStatModel {

    private String dataFrom;

    /**
     * 统计日期 yyyy-MM-dd
     */
    private String statDate;

    /**
     * 触发部分统计任务，为空则统计所有
     */
    private String triggerPart;

    public RefreshCheckerDefectStatModel(String dataFrom, String statDate) {
        this.dataFrom = dataFrom;
        this.statDate = statDate;
    }

}
