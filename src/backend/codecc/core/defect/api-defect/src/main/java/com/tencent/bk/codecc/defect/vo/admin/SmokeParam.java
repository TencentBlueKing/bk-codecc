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

package com.tencent.bk.codecc.defect.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 冒烟任务区间参数
 *
 * @version V1.0
 * @date 2021/6/2
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmokeParam {

    /**
     * 跳过数（忽略前面skip条数据）
     */
    private int skip;

    /**
     * 然后取size条数据
     */
    private int size;

    @Override
    public int hashCode() {
        return skip;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SmokeParam) {
            SmokeParam smokeParam = (SmokeParam) obj;
            return smokeParam.skip == skip && smokeParam.size == size;
        }
        return super.equals(obj);
    }

}
