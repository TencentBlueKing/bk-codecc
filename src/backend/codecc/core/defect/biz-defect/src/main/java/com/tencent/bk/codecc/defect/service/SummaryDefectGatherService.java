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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.SummaryGatherInfo;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;

/**
 * 告警收敛的业务逻辑类
 *
 * @version V1.0
 * @date 2020/5/26
 */
public interface SummaryDefectGatherService {
    /**
     * 查询告警收敛清单
     *
     * @param taskId
     * @return
     */
    FileDefectGatherVO getSummaryDefectGather(Long taskId, String toolName, String dimension);

    /**
     * 查询告警收敛清单
     *
     * @param taskId
     * @return
     */
    void saveSummaryDefectGather(Long taskId, String toolName, SummaryGatherInfo gatherInfo);
}
