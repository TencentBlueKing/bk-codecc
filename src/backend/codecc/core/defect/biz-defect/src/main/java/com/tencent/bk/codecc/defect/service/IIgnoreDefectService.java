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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectModel;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreCommentDefectVO;

import java.util.Set;

/**
 * 注释忽略接口
 * 
 * @date 2021/6/30
 * @version V1.0
 */
public interface IIgnoreDefectService {

    /**
     * 通过任务id查询对应注释忽略信息
     * @param taskId
     * @return
     */
    IgnoreCommentDefectVO findIgnoreDefectInfo(Long taskId);

    /**
     * 保存更新注释忽略告警信息
     * @param ignoreCommentDefectModel
     * @return
     */
    Boolean upsertIgnoreDefectInfo(IgnoreCommentDefectModel ignoreCommentDefectModel);

    /**
     * 删除忽略告警映射字段
     * @param taskId
     * @return
     */
    Boolean deleteIgnoreDefectMap(Long taskId, Set<String> currentFileSet);

}
