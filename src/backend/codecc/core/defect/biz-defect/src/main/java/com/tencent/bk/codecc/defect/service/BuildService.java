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

import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;

import java.util.List;
import java.util.Set;

/**
 * 构建信息逻辑接口
 * 
 * @date 2021/11/19
 * @version V1.0
 */
public interface BuildService {
    /**
     * 更新并且获取构建信息
     * @param buildVO
     * @return
     */
    BuildVO upsertAndGetBuildInfo(BuildVO buildVO);

    /**
     * 通过buildId获取构建信息
     * @param buildId
     * @return
     */
    BuildVO getBuildVOByBuildId(String buildId);

    /**
     * 通过buildId获取构建实体
     * @param buildId
     * @return
     */
    BuildEntity getBuildEntityByBuildId(String buildId);

    /**
     * 通过buildId和taskId获取单个任务的构建实体
     * @param buildId
     * @return
     */
    BuildEntity getBuildEntityByBuildIdAndTaskId(String buildId, Long taskId);

    /**
     * 通过 BuildId list 获取 BuildEntity list
     * @param buildIds
     * @return
     */
    List<BuildEntity> getBuildEntityInBuildIds(List<String> buildIds);

    /**
     * 根据 buildId 获取所对应的构建的 buildNum 和扫描白名单
     *
     * @date 2024/10/18
     * @param buildId
     * @param taskId
     * @param toolName
     * @param taskDetailVO
     * @param whitePaths
     * @return java.lang.String 返回 buildNum
     */
    String getBuildInfo(String buildId, Long taskId, String toolName, TaskDetailVO taskDetailVO,
            Set<String> whitePaths);

    /**
     * 获取扫描白名单
     *
     * @date 2024/10/18
     * @param buildId
     * @param taskVO
     * @return java.util.Set<java.lang.String>
     */
    Set<String> getWhitePaths(String buildId, TaskDetailVO taskVO);
}
