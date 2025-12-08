/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.bk.codecc.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.vo.GetTaskStatusAndCreateFromResponse;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.PipelineBasicInfoVO;
import com.tencent.bk.codecc.task.vo.RuntimeUpdateMetaVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigRequest;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigResponse;
import com.tencent.bk.codecc.task.vo.TaskListReqVO;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.TaskMemberVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.TaskOwnerAndMemberVO;
import com.tencent.bk.codecc.task.vo.TaskStatisticVO;
import com.tencent.bk.codecc.task.vo.TaskStatusVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateDeptInfoVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO;
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.pojo.Page;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务服务接口
 *
 * @version V1.0
 * @date 2019/4/23
 */
public interface TaskService {

    /**
     * 查询任务清单
     *
     * @param projectId
     * @param user
     * @return
     */
    TaskListVO getTaskList(String projectId, String user, TaskSortType taskSortType, TaskListReqVO taskListReqVO);

    /**
     * 查询任务名字清单
     *
     * @param projectId
     * @param user
     * @return
     */
    TaskListVO getTaskBaseList(String projectId, String user);

    /**
     * 根据任务Id查询任务完整信息
     *
     * @return
     */
    TaskBaseVO getTaskInfo();

    /**
     * 获取任务信息
     *
     * @param taskId
     * @return
     */
    TaskDetailVO getTaskInfoById(Long taskId);

    /**
     * 获取任务信息
     *
     * @param projectId)
     * @return
     */
    List<TaskDetailVO> getTaskInfoByProjectId(String projectId);

    /**
     * 通过taskid查询任务信息，不包含工具信息
     *
     * @param taskId
     * @return
     */
    TaskDetailVO getTaskInfoWithoutToolsByTaskId(Long taskId);

    /**
     * 根据流名称获取任务的有效工具及语言等信息
     *
     * @param streamName
     * @return
     */
    TaskDetailVO getTaskInfoByStreamName(String streamName);

    /**
     * 根据任务Id查询任务接入工具情况
     *
     * @param taskId
     * @return
     */
    TaskBaseVO getTaskToolList(long taskId);

    /**
     * 修改任务基本信息
     *
     * @param taskUpdateVO
     * @param userName
     * @return
     */
    Boolean updateTask(TaskUpdateVO taskUpdateVO, Long taskId, String userName);

    /**
     * 修改任务repoOwner
     *
     * @param taskId
     * @param repoOwners
     * @param userName
     * @return
     */
    Boolean updateTaskRepoOwner(Long taskId, List<String> repoOwners, String userName);


    /**
     * 修改任务repoOwner
     *
     * @param taskId
     * @param userName
     * @return
     */
    Boolean updateRuntimeInfo(Long taskId, RuntimeUpdateMetaVO runtimeUpdateMetaVO, String userName);

    /**
     * 获取任务信息概览
     *
     * @param taskId
     * @return
     */
    TaskOverviewVO getTaskOverview(Long taskId, String buildId, String buildNum);

    /**
     * 获取任务信息概览
     *
     * @param taskId
     * @return
     */
    TaskOverviewVO getTaskOverview(Long taskId, String buildId, String buildNum, String orderBy);

    /**
     * 开启任务
     *
     * @param taskId
     * @param userName
     * @return
     */
    Boolean startTask(Long taskId, String userName);


    /**
     * 停用任务
     *
     * @param taskId
     * @param disabledReason
     * @param userName
     * @return Boolean
     */
    Boolean stopTask(Long taskId, String disabledReason, String userName);

    /**
     * 停用任务
     *
     * @param pipelineId
     * @param disabledReason
     * @param userName
     * @return Boolean
     */
    Boolean stopTask(String pipelineId, String disabledReason, String userName);

    /**
     * 将自建任务 id 和异步执行该自建任务的流水线 id 建立联系
     *
     * @param taskId
     * @param pipelineId
     * @param userName
     * @return Boolean
     */
    Boolean addRelationshipBetweenTaskAndPipeline(Long taskId, String userName, String projectId, String pipelineId);

    /**
     * 删除任务
     *
     * @param taskId
     * @param userName
     * @return
     */
    Boolean deleteTask(String projectId, Long taskId, String userName);

    /**
     * 获取用到该 CodeCC 任务的流水线
     *
     * @param taskId
     * @return PipelineBasicInfoVO
     */
    List<PipelineBasicInfoVO> getRelatedPipelinesByTaskId(String projectId, Long taskId, String userName);

    /**
     * 停用单个流水线任务
     *
     * @param pipelineId
     * @param multiPipelineMark
     * @param disabledReason
     * @param userName
     * @param asyncTaskId
     * @return
     */
    Boolean stopSinglePipelineTask(String pipelineId, String multiPipelineMark,
                                   String disabledReason, String userName, String asyncTaskId);

    /**
     * 停用任务
     *
     * @param taskId
     * @param userName
     * @return
     */
    Boolean stopTaskByAdmin(Long taskId, String disabledReason, String userName);

    /**
     * 开启任务
     *
     * @param taskId
     * @param userName
     * @return
     */
    Boolean startTaskByAdmin(Long taskId, String userName);


    /**
     * 获取代码库配置信息
     *
     * @param taskId
     * @return
     */
    TaskCodeLibraryVO getCodeLibrary(Long taskId);


    /**
     * 更新代码库配置信息
     *
     * @param taskId
     * @param taskDetailVO
     * @return
     */
    Boolean updateCodeLibrary(Long taskId, String userName, TaskDetailVO taskDetailVO) throws JsonProcessingException;

    /**
     * 获取任务有权限的各角色人员清单
     *
     * @param taskId
     * @param projectId
     * @return
     */
    TaskMemberVO getTaskUsers(long taskId, String projectId);

    /**
     * 检查任务是否存在
     *
     * @param taskId
     * @return
     */
    Boolean checkTaskExists(long taskId);


    /**
     * 获取所有的基础工具信息
     *
     * @return
     */
    Map<String, ToolMetaBaseVO> getToolMetaListFromCache();

    /**
     * 手动触发分析-不加代理
     *
     * @param taskId
     * @param isFirstTrigger
     * @param userName
     * @return
     */
    Boolean manualExecuteTaskNoProxy(long taskId, String isFirstTrigger, String userName);

    /**
     * 手动触发分析
     *
     * @param taskId
     * @param isFirstTrigger
     * @param userName
     * @return
     */
    Pair<Boolean, String> manualExecuteTask(long taskId, String isFirstTrigger, String userName, Integer timeout);

    /**
     * 发送任务开始信号
     *
     * @param taskId
     * @param buildId
     * @return
     */
    Boolean sendStartTaskSignal(Long taskId, String buildId, Integer timeout);


    /**
     * 通过流水线ID获取任务信息
     *
     * @param pipelineId
     * @param user
     * @return
     */
    PipelineTaskVO getTaskInfoByPipelineId(String pipelineId, String multiPipelineMark, String user);

    /**
     * 通过流水线ID获取任务信息
     *
     * @param pipelineId
     * @param user
     * @return
     */
    List<PipelineTaskVO> getTaskInfoByPipelineId(String pipelineId, String user);

    /**
     * 通过流水线ID获取任务ID
     *
     * @param pipelineId
     * @param user
     * @return
     */
    List<Long> getTaskIdsByPipelineId(String pipelineId, String user);

    /**
     * 检查工具是否已经下架
     *
     * @param toolName
     * @param taskInfoEntity
     * @return true:已下架，false:未下架
     */
    boolean checkToolRemoved(String toolName, TaskInfoEntity taskInfoEntity);

    /**
     * 通过流水线信息获取任务id
     *
     * @param pipelineId
     * @param multiPipelineMark
     * @return
     */
    Long getTaskIdByPipelineInfo(String pipelineId, String multiPipelineMark);


    /**
     * 获取任务状态
     *
     * @param taskId
     * @return
     */
    TaskStatusVO getTaskStatus(Long taskId);

    /**
     * 根据id查询task信息
     *
     * @param taskId
     * @return
     */
    TaskInfoEntity getTaskById(Long taskId);

    /**
     * 根据bg id查询任务清单
     *
     * @param bgId
     * @return
     */
    List<TaskBaseVO> getTasksByBgId(Integer bgId);

    /**
     * 通过task id查询任务清单
     *
     * @param taskIds
     * @return
     */
    List<TaskBaseVO> getTasksByIds(List<Long> taskIds);

    List<TaskDetailVO> getTaskDetailListByIdsWithDelete(QueryTaskListReqVO reqVO);

    /**
     * 根据任务状态及任务接入过的工具获取
     *
     * @param taskListReqVO 请求体
     * @return list
     */
    TaskListVO getTaskDetailList(QueryTaskListReqVO taskListReqVO);


    /**
     * 根据作者名、代码仓库地址及分支名获取任务列表
     *
     * @param reqVO 查询'我的'任务请求体
     * @return list
     */
    Page<TaskInfoVO> getTasksByAuthor(QueryMyTasksReqVO reqVO);


    /**
     * 更新定时报告信息
     *
     * @param taskId
     * @param notifyCustomVO
     */
    void updateReportInfo(Long taskId, NotifyCustomVO notifyCustomVO);

    /**
     * 更新置顶用户信息
     *
     * @param taskId
     * @param user
     * @return
     */
    Boolean updateTopUserInfo(Long taskId, String user, Boolean topFlag);

    /**
     * 设置强制全量扫描标志
     *
     * @param taskEntity
     */
    void setForceFullScan(TaskInfoEntity taskEntity);

    /**
     * 修改任务扫描触发配置
     *
     * @param taskId
     * @param scanConfigurationVO
     * @return
     */
    Boolean updateScanConfiguration(Long taskId, String user, ScanConfigurationVO scanConfigurationVO);

    /**
     * api使用作者转换
     *
     * @param taskId
     * @param transferAuthorPairs
     * @param userId
     * @return
     */
    Boolean authorTransferForApi(Long taskId, List<ScanConfigurationVO.TransferAuthorPair> transferAuthorPairs,
            String userId);


    /**
     * 按事业群ID获取部门ID集合
     *
     * @param bgId bgId
     * @return set
     */
    Set<Integer> queryDeptIdByBgId(Integer bgId);

    /**
     * 多条件查询任务列表
     *
     * @param taskListReqVO 请求体
     * @return 任务列表
     */
    List<TaskDetailVO> getTaskInfoList(QueryTaskListReqVO taskListReqVO);

    /**
     * 分页查询任务详情列表
     *
     * @param reqVO 请求体
     * @return page
     */
    Page<TaskDetailVO> getTaskDetailPage(QueryTaskListReqVO reqVO);

    /**
     * 刷新组织架构
     *
     * @return boolean
     */
    Boolean refreshTaskOrgInfo(Long taskId);

    /**
     * 更新任务管理员和任务成员
     *
     * @param vo
     * @param taskId
     */
    void updateTaskOwnerAndMember(TaskOwnerAndMemberVO vo, Long taskId);


    /**
     * 触发蓝盾插件打分任务
     */
    Boolean triggerBkPluginScoring();

    /**
     * 按创建来源查询任务ID
     *
     * @param taskCreateFrom 任务来源列表
     * @return list
     */
    List<Long> queryTaskIdByCreateFrom(List<String> taskCreateFrom);

    /**
     * 根据任务英文名查询任务信息，不包含工具信息
     *
     * @param nameEn 流名称
     * @return vo
     */
    TaskDetailVO getTaskInfoWithoutToolsByStreamName(String nameEn);


    /**
     * 获取工蜂代码库信息
     */
    TaskCodeLibraryVO getRepoInfo(Long taskId);

    /**
     * 添加路径白名单
     */
    boolean addWhitePath(long taskId, List<String> pathList);

    /**
     * 编辑任务信息
     *
     * @param reqVO 请求体
     * @return bool
     */
    Boolean editTaskDetail(TaskUpdateDeptInfoVO reqVO);

    /**
     * 按任务id获取项目id map
     */
    Map<Long, String> getProjectIdMapByTaskId(QueryTaskListReqVO taskListReqVO);

    Map<String, Set<Long>> queryTaskIdByWithdrawTool(Set<String> toolSet);

    long countTaskSize();

    List<TaskDetailVO> getTaskIdByPage(int page, int pageSize);

    List<TaskBaseVO> queryTaskListByProjectId(String projectId);

    List<Long> queryTasIdByProjectId(String projectId);

    List<Long> queryAllTaskIdByProjectId(String projectId);

    List<String> queryAllPipelineIdByProjectId(String projectId);

    List<String> queryProjectIdPage(Set<String> createFrom, Integer pageNum, Integer pageSize);

    List<Long> queryTaskIdPageByProjectId(String projectId, Integer pageNum, Integer pageSize);

    /**
     * 获取任务详情，且工具信息是已排序的
     *
     * @param request
     * @return
     */
    TaskInfoWithSortedToolConfigResponse getTaskInfoWithSortedToolConfig(
            TaskInfoWithSortedToolConfigRequest request
    );

    List<TaskBaseVO> getTaskIdAndCreateFromWithPage(long lastTaskId, Integer limit);

    List<TaskBaseVO> listTaskBase(String userId, String projectId);

    List<Long> queryTaskIdByProjectIdWithPermission(String projectId, String userId);

    Map<Long, String> listTaskNameCn(List<Long> taskIdList);

    boolean multiTaskVisitable(String projectId);


    List<MetadataVO> listTaskToolDimension(List<Long> taskIdList, String projectId);

    /**
     * iam（蓝盾）回调实现方法
     */
    String getInstanceByResource(CallbackRequestDTO callBackInfo);

    String getLatestBuildId(Long taskId);

    Map<Long, String> getLatestBuildIdMap(List<Long> taskIdList);

    void setTaskToColdFlag(long taskId);

    void setTaskToEnableFlag(long taskId);

    List<Long> getTaskIdListForHotColdDataSeparation(Long lastTaskId, Integer limit);

    List<String> getTaskToolNameList(Long taskId);

    GetTaskStatusAndCreateFromResponse getTaskStatusAndCreateFrom(Long taskId);

    List<TaskStatisticVO> getTaskStatisticByIds(List<Long> taskIds);

    List<Long> getTaskIdNeProjectIdWithPage(String filterProjectId, int pageNum, int pageSize);

    /**
     * 项目禁用后停用任务
     * @param projectId
     */
    void stopDisableProjectTask(String projectId);

    /**
     * 项目启用后重新启用任务
     * @param projectId
     */
    void startEnableProjectTask(String projectId);

}
