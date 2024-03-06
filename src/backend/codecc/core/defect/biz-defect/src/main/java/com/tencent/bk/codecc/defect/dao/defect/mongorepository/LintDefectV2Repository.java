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

package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.defect.code.gen.LintDefectV2EntityTracking;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 查询分析记录持久层代码
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Repository
public interface LintDefectV2Repository extends MongoRepository<LintDefectV2Entity, String> {

    /**
     * 快速增量逻辑所有实体数据字段，涵盖快照、数据统计、红线
     */
    String FAST_INCR_FIELD = "{'_id':1, 'status':1, 'task_id':1, 'tool_name':1, 'revision':1, 'ignore_reason_type':1,"
            + "'branch':1, 'sub_module':1, 'line_num':1, 'file_path':1, 'line_update_time':1, 'author':1, "
            + "'severity':1, 'checker':1, 'lang_value':1, 'language':1}";

    LintDefectV2Entity findByEntityId(String entityId);

    /**
     * 根据告警ID分页列表查询告警信息
     *
     * @param entityIds
     * @param pageable
     * @return
     */
    @Query(value = "{'_id': {'$in': ?0}}")
    List<LintDefectV2Entity> findByEntityIdIn(Set<String> entityIds, Pageable pageable);

    /**
     * 通过任务Id、工具名称、状态查询Lint文件信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    @Query(fields = "{'defect_instances': 0}")
    List<LintDefectV2Entity> findNoneInstancesFieldByTaskIdAndToolNameAndStatus(
            long taskId, String toolName, int status
    );


    /**
     * 通过任务Id、工具名称、状态查询Lint文件信息
     *
     * @param taskId
     * @param toolNameSet
     * @param status
     * @return
     */
    List<LintDefectV2Entity> findByTaskIdAndToolNameInAndStatus(long taskId, List<String> toolNameSet, int status);

    /**
     * 通过任务id、工具名、文件路径查询告警文件清单
     *
     * @param taskId
     * @param toolName
     * @param filePathSet
     * @return
     */
    List<LintDefectV2Entity> findByTaskIdAndToolNameAndFilePathIn(long taskId, String toolName,
            Set<String> filePathSet);

    /**
     * 通过任务id、工具名、文件相对路径查询告警文件清单
     *
     * @param taskId
     * @param toolName
     * @param relPathSet
     * @return
     */
    List<LintDefectV2Entity> findByTaskIdAndToolNameAndRelPathIn(long taskId, String toolName, Set<String> relPathSet);

    /**
     * 根据entityIdSet查询告警信息
     *
     * @param entityIdSet
     * @return
     */
    @Query(fields = "{'_id': 1, 'status':1, 'tool_name':1}", value = "{'_id': {'$in': ?0}}")
    List<LintDefectV2Entity> findStatusByEntityIdIn(Set<String> entityIdSet);

    /**
     * 通过任务Id、工具名称、状态查询Lint文件信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    @Query(fields = "{'severity':1, 'author':1, 'checker':1, 'line_update_time':1}", value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}")
    List<LintDefectV2Entity> findFiledsByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);

    List<LintDefectV2Entity> findByTaskIdAndStatus(long taskId, int status);

    /**
     * 通过任务Id、工具名称、已关闭的告警
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(
            fields = "{'severity':1, 'status':1, 'checker':1}",
            value = "{'task_id': ?0, 'tool_name': ?1, 'status': {'$gt':1}}"
    )
    List<LintDefectV2Entity> findCloseDefectByTaskIdAndToolName(long taskId, String toolName);

    @Query(
            fields = "{'severity':1, 'status':1, 'checker':1, '_id':0}",
            value = "{'task_id': ?0, 'tool_name': ?1, 'status': {'$gt':1}}"
    )
    List<LintDefectV2Entity> findCloseDefectByTaskIdAndToolName(long taskId, String toolName, Pageable pageable);

    @Query(fields = "{'severity':1, 'author':1, 'line_update_time':1}", value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}")
    Page<LintDefectV2Entity> findByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status,
            Pageable pageable);

    List<LintDefectV2Entity> findByTaskIdAndToolNameInAndStatus(Long taskId, List<String> toolNameList, int status);

    Integer countByTaskIdAndToolNameInAndStatusAndSeverity(
            Long taskId, List<String> toolNameList, int status, int severity);

    Integer countByTaskIdAndToolNameAndStatusAndSeverity(
            Long taskId, String toolNameList, int status, int severity);

    /**
     * 获取最早的 lineUpdateTime
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return LintDefectV2Entity
     */
    LintDefectV2Entity findFirstByTaskIdAndToolNameAndStatusOrderByLineUpdateTimeAsc(
            Long taskId, String toolName, int status);

    /**
     * 获取 lineUpdateTime 之后的所有告警
     *
     * @param taskId
     * @param toolName
     * @param lineUpdateTime
     * @return
     */
    List<LintDefectV2Entity> findByTaskIdAndToolNameAndFilePathInAndLineUpdateTimeGreaterThanEqual(
            Long taskId, String toolName, Set<String> filePathSet, Long lineUpdateTime);

    /**
     * 获取 lineUpdateTime 之后的所有告警
     *
     * @param taskId
     * @param toolName
     * @param relPathSet
     * @return
     */
    List<LintDefectV2Entity> findByTaskIdAndToolNameAndRelPathInAndLineUpdateTimeGreaterThanEqual(
            long taskId, String toolName, Set<String> relPathSet, Long lineUpdateTime);

    /**
     * 超快增量查询告警专用
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    @Query(fields = FAST_INCR_FIELD, value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}")
    List<LintDefectV2Entity> findFastIncrFieldByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);

    /**
     * 超快增量查询告警专用
     *
     * @param entityIds
     * @return
     */
    @Query(fields = FAST_INCR_FIELD, value = "{'_id': {'$in': ?0}}")
    List<LintDefectV2Entity> findFastIncrFieldByEntityIdIn(Collection<String> entityIds);


    LintDefectV2EntityTracking.ListContainer findTrackingByTaskIdAndToolNameAndFilePathIn(
            long taskId,
            String toolName,
            Set<String> filePathSet
    );

    LintDefectV2EntityTracking.ListContainer findTrackingByTaskIdAndToolNameAndRelPathIn(
            long taskId,
            String toolName,
            Set<String> relPathSet
    );

    @Query(fields = "{'id':1}")
    List<LintDefectV2Entity> findIdsByTaskIdAndToolName(long taskId, String toolName);


    @Query(fields = "{'id':1, 'status':1, 'author':1, 'severity':1, 'rel_path':1, 'file_path':1, "
            + "'exclude_time':1, 'checker':1, 'create_time':1}",
            value = "{'task_id': ?0, 'tool_name': ?1, 'id': {'$in': ?2}}")
    List<LintDefectV2Entity> findStatusAndAuthorAndSeverityByTaskIdAndToolNameAndIdIn(
            long taskId,
            String toolName,
            Set<String> idSet
    );

    @Query(value = "{'task_id': ?0, 'tool_name': ?1, 'id': {'$in': ?2}}")
    List<LintDefectV2Entity> findByTaskIdAndToolNameAndIdIn(long taskId, String toolName, Set<String> idSet);

    List<LintDefectV2Entity> findByTaskIdAndToolName(long taskId, String toolName);

    @Query(
            fields = "{'id':1, 'revision':1, 'line_num':1, 'status':1, 'checker':1, 'severity':1, "
                    + "'create_time':1}",
            value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}"
    )
    List<LintDefectV2Entity> findSnapshotRedLineFieldByTaskIdAndToolNameAndStatus(
            long taskId,
            String toolName,
            int status
    );

    @Query(fields = "{'defect_instances': 0}", value = "{'task_id': ?0, '_id': {'$in': ?1}}")
    List<LintDefectV2Entity> findNoneInstancesFieldByTaskIdAndEntityIdIn(Long taskId, Set<String> entityIdSet);

    @Query(fields = "{'status':1, 'author': 1, 'severity': 1}")
    List<LintDefectV2Entity> findCheckReportBizFieldByTaskIdAndToolName(long taskId, String toolName);
}
