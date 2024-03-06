/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.GrayToolProjectEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 灰度工具项目数据持久化接口
 *
 * @version V1.0
 * @date 2020/12/29
 */
@Repository
public interface GrayToolProjectRepository extends MongoRepository<GrayToolProjectEntity, String> {

    /**
     * 查询灰度项目下所有的工具清单
     * @param projectId 项目id
     * @return list
     */
    List<GrayToolProjectEntity> findByProjectId(String projectId);

    /**
     * 根据状态查询数据信息
     *
     * @param status
     * @return
     */
    List<GrayToolProjectEntity> findAllByStatus(String status);

    /**
     * 根据项目id查询
     * @param projectIdSet
     * @return
     */
    List<GrayToolProjectEntity> findByProjectIdIn(Set<String> projectIdSet);

    /**
     * 根据项目ID查询当前项目的更新人
     *
     * @param projectId 项目ID
     * @return string
     */
    @Query(fields = "{'updated_by': 1}")
    GrayToolProjectEntity findFirstUpdatedByProjectIdAndToolName(String projectId, String toolName);

    /**
     * 新增工具维度，项目id + 工具 = 唯一实体
     */
    GrayToolProjectEntity findFirstByProjectIdAndToolName(String projectId, String toolName);
}
