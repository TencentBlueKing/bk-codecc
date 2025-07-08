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

package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 告警查询持久代码
 *
 * @version V1.0
 * @date 2019/10/20
 */
@Repository
public interface DefectRepository extends MongoRepository<CommonDefectEntity, String> {

    /**
     * 通过实体id查询告警信息
     *
     * @param entityId
     * @return
     */
    CommonDefectEntity findFirstByEntityId(String entityId);

    /**
     * 通过实体id查询告警信息
     *
     * @param entityId
     * @return
     */
    List<CommonDefectEntity> findByEntityIdIn(Set<String> entityId);

    /**
     * 根据任务ID、告警ID分页列表查询告警信息
     *
     * @param entityIds
     * @param pageable
     * @return
     */
    @Query(value = "{'_id': {'$in': ?0}}")
    List<CommonDefectEntity> findByEntityIdIn(Set<String> entityIds, Pageable pageable);

    @Query(fields = "{'defect_instances':0}", value = "{'task_id': ?0, '_id': {'$in': ?1}}")
    List<CommonDefectEntity> findNoneInstancesFieldByTaskIdAndEntityIdIn(Long taskId, Set<String> entityIdSet);

    /**
     * 通过任务id，工具名查询告警信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    List<CommonDefectEntity> findByTaskIdAndToolName(long taskId, String toolName);

    @Query(fields = "{'status':1, 'author_list': 1, 'severity': 1}")
    List<CommonDefectEntity> findCheckReportBizFieldByTaskIdAndToolName(long taskId, String toolName);


    @Query(fields = "{'defect_instances':0}")
    List<CommonDefectEntity> findForBatchBizByTaskIdAndToolName(long taskId, String toolName);


    /**
     * 通过任务id查询告警信息
     *
     * @param taskId
     * @return
     */
    List<CommonDefectEntity> findByTaskId(long taskId);

    List<CommonDefectEntity> findByTaskId(long taskId, Pageable pageable);

    /**
     * 通过任务id，工具名查询告警信息
     *
     * @param taskId
     * @param toolNameSet
     * @return
     */
    List<CommonDefectEntity> findByTaskIdAndToolNameIn(long taskId, List<String> toolNameSet);

    /**
     * 通过任务id，工具名查询告警信息(去除defect_instances)
     *
     * @param taskId
     * @param toolNameSet
     * @return
     */
    @Query(fields = "{'defect_instances':0}", value = "{ 'task_id' : ?0 , 'tool_name' : { '$in' : ?1 }}")
    List<CommonDefectEntity> findByTaskIdAndToolNameInExcludeInstances(long taskId, List<String> toolNameSet);

    /**
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    List<CommonDefectEntity> findByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);

    /**
     * 根据条件获取，但屏蔽掉defect_instances字段
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    @Query(fields = "{'defect_instances':0}", value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}")
    List<CommonDefectEntity> findNoneInstancesFieldByTaskIdAndToolNameAndStatus(
            long taskId,
            String toolName,
            int status
    );

    /**
     * 根据条件获取"快照"和"红线"所需字段
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    @Query(
            fields = "{'id':1, 'revision':1, 'line_number':1, 'status':1, 'checker_name':1, 'severity':1, "
                    + "'create_time':1}",
            value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}"
    )
    List<CommonDefectEntity> findSnapshotRedLineFieldByTaskIdAndToolNameAndStatus(
            long taskId,
            String toolName,
            int status
    );

    /**
     * 根据条件获取Id集
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    @Query(fields = "{'id':1}", value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}")
    List<CommonDefectEntity> findIdByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);

    /**
     * 根据条件获取"红线"所需字段数据
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    @Query(
            fields = "{'status':1, 'checker_name':1, 'severity':1, 'create_time':1}",
            value = "{'task_id': ?0, 'tool_name': ?1, 'status': ?2}"
    )
    List<CommonDefectEntity> findRedLineFieldByTaskIdAndToolNameAndStatus(long taskId, String toolName, int status);

    /**
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param toolNameSet
     * @param status
     * @return
     */
    List<CommonDefectEntity> findByTaskIdAndToolNameInAndStatus(long taskId, List<String> toolNameSet, int status);

    /**
     * 根据taskId，工具名查询所有的告警ID
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'id':1}", value = "{'task_id': ?0, 'tool_name': ?1}")
    List<CommonDefectEntity> findIdsByTaskIdAndToolName(long taskId, String toolName);

    /**
     * 通过任务id，工具名查询告警信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'id':1, 'status':1, 'author_list':1, 'severity':1, 'rel_path':1, 'file_path_name':1, "
            + "'exclude_time':1, 'checker_name':1, 'create_time':1}",
            value = "{'task_id': ?0, 'tool_name': ?1, 'id': {'$in': ?2}}")
    List<CommonDefectEntity> findStatusAndAuthorAndSeverityByTaskIdAndToolNameAndIdIn(long taskId, String toolName,
            Set<String> idSet);

    /**
     * 通过任务id，工具名查询告警信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(value = "{'task_id': ?0, 'tool_name': ?1, 'id': {'$in': ?2}}")
    List<CommonDefectEntity> findByTaskIdAndToolNameAndIdIn(long taskId, String toolName, Set<String> idSet);

    /**
     * 根据entityIdSet查询告警信息
     *
     * @param entityIdSet
     * @return
     */
    @Query(fields = "{'id':1, 'status':1, 'tool_name':1}", value = "{'_id': {'$in': ?0}}")
    List<CommonDefectEntity> findStatusByEntityIdIn(Set<String> entityIdSet);

    /**
     * 根据规则名和任务id查询
     *
     * @param checkerName
     * @param taskId
     * @return
     */
    Page<CommonDefectEntity> findByCheckerInAndTaskIdIn(
            List<String> checkerName, List<Long> taskId, Pageable pageable);

    /**
     * 根据规则名查询
     *
     * @param checkerName
     * @return
     */
    Page<CommonDefectEntity> findByCheckerIn(List<String> checkerName, Pageable pageable);


    /**
     * 获取批量任务、规则名范围的告警数据
     *
     * @param toolName 工具名称
     * @param taskIdSet 任务ID集合
     * @param checkerNameSet 规则名集合
     * @return entity list
     */
    @Query(fields = "{'stream_name':0, 'display_category':0, 'display_type':0}",
            value = "{'tool_name': ?0, 'task_id': {'$in': ?1}, 'checker_name': {'$in': ?2}}")
    List<CommonDefectEntity> findByToolNameAndTaskIdInAndCheckerIn(String toolName, Collection<Long> taskIdSet,
            Set<String> checkerNameSet);


    /**
     * 通过任务Id、工具名称、已关闭的告警
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(
            fields = "{'severity':1, 'status':1, 'checker_name': 1}",
            value = "{'task_id': ?0, 'tool_name': ?1, 'status': {'$gt':1}}"
    )
    List<CommonDefectEntity> findCloseDefectByTaskIdAndToolName(long taskId, String toolName);

    /**
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param toolNameList
     * @param status
     * @return
     */
    Integer countByTaskIdAndToolNameInAndStatusAndSeverity(long taskId,
            List<String> toolNameList,
            int status,
            int severity);

    /**
     * 通过任务id，工具名和状态查询告警信息
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    Integer countByTaskIdAndToolNameAndStatusAndSeverity(long taskId, String toolName, int status, int severity);

    /**
     * 分页获取告警
     *
     * @param taskId
     * @param toolName
     * @param pageable
     * @return
     */
    List<CommonDefectEntity> findByTaskIdAndToolNameOrderByStatusDesc(long taskId, String toolName, Pageable pageable);

    Integer countByTaskIdAndToolName(long taskId, String toolName);
}
