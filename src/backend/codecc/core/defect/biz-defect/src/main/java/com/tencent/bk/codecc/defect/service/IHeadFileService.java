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

import com.tencent.bk.codecc.defect.model.HeadFileEntity;
import com.tencent.bk.codecc.defect.vo.HeadFileVO;

import java.util.Set;

/**
 * oc头文件识别接口
 * 
 * @date 2021/12/31
 * @version V1.0
 */
public interface IHeadFileService {

    /**
     * 查询oc头文件信息
     * @param taskId
     * @return
     */
    HeadFileVO findHeadFileInfo(Long taskId);

    /**
     * 删除头文件信息
     * @param taskId
     * @param headFileSet
     * @return
     */
    Boolean deleteHeadFileInfo(Long taskId, Set<String> headFileSet);

    /**
     * 更新头文件信息
     * @param headFileEntity
     * @return
     */
    Boolean upsertHeadFileInfo(HeadFileEntity headFileEntity);
}
