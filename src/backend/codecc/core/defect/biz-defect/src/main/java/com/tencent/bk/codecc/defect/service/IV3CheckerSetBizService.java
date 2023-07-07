package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.devops.common.api.checkerset.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.util.Pair;

/**
 * V3规则集服务
 *
 * @version V1.0
 * @date 2020/1/2
 */
public interface IV3CheckerSetBizService
{
    /**
     * 创建规则集
     *
     * @param user
     * @param projectId
     * @param createCheckerSetReqVO
     * @return
     */
    void createCheckerSet(String user, String projectId, CreateCheckerSetReqVO createCheckerSetReqVO);

    /**
     * 全量更新规则
     * @param user
     * @param updateAllCheckerReq
     * @return
     */
    Boolean updateCheckersOfSetForAll(String user, UpdateAllCheckerReq updateAllCheckerReq);

    /**
     * 更新规则集中的规则
     *
     * @param checkerSetId
     * @param user
     * @param checkerProps
     * @param version
     * @return
     */
    void updateCheckersOfSet(String checkerSetId, String user, List<CheckerPropVO> checkerProps, Integer version);

    /**
     * 刷新本次规则更新涉及的任务的信息。包括强制全量标志，工具，告警状态等
     * @param checkerSetEntity
     * @param fromCheckerSet
     * @param projectRelationships
     * @param user
     */
    void updateTaskAfterChangeCheckerSet(CheckerSetEntity checkerSetEntity, CheckerSetEntity fromCheckerSet,
                                         List<CheckerSetProjectRelationshipEntity> projectRelationships,
                                         String user);

    /**
     * 刷新本次规则更新涉及的任务的信息。包括强制全量标志，工具，告警状态等
     * @param checkerSetEntity
     * @param fromCheckerSet
     * @param version
     * @param user
     */
    void updateTaskAfterChangeCheckerSet(
        CheckerSetEntity checkerSetEntity, CheckerSetEntity fromCheckerSet, int version, String user);

    /**
     * 查询规则集列表
     *
     * @param projectId
     * @param queryCheckerSetReq
     * @return
     */
    Page<CheckerSetVO> getOtherCheckerSets(String projectId, OtherCheckerSetListQueryReq queryCheckerSetReq);

    /**
     * 查询规则集列表
     *
     * @param queryCheckerSetReq
     * @return
     */
    List<CheckerSetVO> getCheckerSetsOfProject(CheckerSetListQueryReq queryCheckerSetReq);

    /**
     * 分页查询规则集列表
     *
     * @param queryCheckerSetReq
     * @return
     */
    Page<CheckerSetVO> getCheckerSetsOfProjectPage(CheckerSetListQueryReq queryCheckerSetReq);

    /**
     * 分类查询
     * @param projectId
     * @return
     */
    Map<String, List<CheckerSetVO>> getAvailableCheckerSetsOfProject(String projectId);

    /**
     * 查询规则集列表
     *
     * @param queryCheckerSetReq
     * @return
     */
    List<CheckerSetVO> getCheckerSetsOfTask(CheckerSetListQueryReq queryCheckerSetReq);

    /**
     * 查询规则集列表
     *
     */
    List<CheckerSetVO> getTaskCheckerSets(
            String projectId,
            long taskId,
            String toolName,
            String dimension,
            boolean needProps
    );

    List<CheckerSetVO> getTaskCheckerSets(
            String projectId,
            List<Long> taskIdList,
            List<String> toolNameList
    );

    /**
     * 获取任务的规则集，含checker_props
     *
     * @param projectId
     * @param taskId
     * @param toolNameList
     * @return
     */
    List<CheckerSetVO> getTaskCheckerSets(String projectId, long taskId, List<String> toolNameList);

    /**
     * 分页查询规则集列表
     *
     * @param queryCheckerSetReq
     * @return
     */
    Page<CheckerSetVO> getCheckerSetsOfTaskPage(CheckerSetListQueryReq queryCheckerSetReq);

    /**
     * 查询规则集参数
     *
     * @param projectId
     * @return
     */
    CheckerSetParamsVO getParams(String projectId);

    /**
     * 规则集ID
     *
     * @param checkerSetId
     * @param version
     */
    CheckerSetVO getCheckerSetDetail(String checkerSetId, int version);

    /**
     * 修改规则集基础信息
     *
     * @param checkerSetId
     * @param projectId
     * @param updateCheckerSetReq
     */
    void updateCheckerSetBaseInfo(String checkerSetId, String projectId, V3UpdateCheckerSetReqVO updateCheckerSetReq);

    /**
     * 计算规则数量
     * @param checkerSetListQueryReq
     * @return
     */
    List<CheckerCommonCountVO> queryCheckerSetCountList(CheckerSetListQueryReq checkerSetListQueryReq);

    /**
     * 关联单个规则集与项目或任务
     *
     * @param checkerSetId
     * @param user
     * @param checkerSetRelationshipVO
     */
    void setRelationships(String checkerSetId, String user, CheckerSetRelationshipVO checkerSetRelationshipVO);

    /**
     * 一键关联单个规则集与项目或任务
     *
     */
    Pair<Boolean,String> setRelationshipsOnce(String user, String projectId, long taskId, String toolName);

    /**
     * 批量关联任务和规则集
     *
     * @param projectId
     * @param taskId
     * @param checkerSetList
     * @param user
     * @return
     */
    Boolean batchRelateTaskAndCheckerSet(String projectId, Long taskId, List<CheckerSetVO> checkerSetList, String user, Boolean isOpenSource);

    /**
     * 规则集管理
     *
     * @param user
     * @param checkerSetId
     * @param checkerSetManagementVO
     */
    void management(String user, String checkerSetId, CheckerSetManagementReqVO checkerSetManagementVO);

    /**
     * 根据规则ID列表查询规则集
     * @param checkerSetList
     * @param projectId
     * @return
     */
    List<CheckerSetVO> queryCheckerSets(Set<String> checkerSetList, String projectId);

    /**
     * 根据任务Id查询任务已经关联的规则集列表
     * @param taskId
     * @return
     */
    List<CheckerSetVO> getCheckerSetsByTaskId(Long taskId);

    /**
     * I18N包装: findAvailableCheckerSetsByProject
     *
     * @param projectId
     * @param legacyList
     * @param toolIntegratedStatus
     * @return
     */
    List<CheckerSetEntity> findAvailableCheckerSetsByProjectI18NWrapper(
            String projectId,
            List<Boolean> legacyList,
            int toolIntegratedStatus
    );

    /**
     * 根据项目ID查询规则集
     * legacy == true，查询旧插件规则集
     * legacy == false，查询新插件规则集
     * @param projectId
     * @param legacy
     * @return
     */
    List<CheckerSetEntity> findAvailableCheckerSetsByProject(String projectId,
                                                             List<Boolean> legacy,
                                                             int toolIntegratedStatus);

    /**
     * 根据任务id和语言解绑
     * @param taskId
     * @param codeLang
     * @return
     */
    Boolean updateCheckerSetAndTaskRelation(Long taskId, Long codeLang, String user);

    /**
     * 根据任务id和项目id返回数量
     * @param taskId
     * @param projectId
     * @return
     */
    TaskBaseVO getCheckerAndCheckerSetCount(Long taskId, String projectId);

    /**
     * 为开源扫描配置规则集
     * @param checkerSetList
     * @return
     */
    List<CheckerSetVO> queryCheckerSetsForOpenScan(Set<CheckerSetVO> checkerSetList);

    Boolean updateCheckerSetBaseInfoByOp(String userName, V3UpdateCheckerSetReqExtVO checkerSetVO);

    /**
     * 获取规则集管理初始化参数选项
     *
     * @return
     */
    CheckerSetParamsVO getCheckerSetParams();

    /**
     * 查询规则列表通过规则id和指定版本
     * @return
     */
    List<CheckerSetVO> queryCheckerDetailForPreCI();

    /**
     * 查询规则列表通过指定规则集
     * @param checkerSetIdList
     * @return
     */
    List<CheckerSetVO> queryCheckerDetailForContent(List<String> checkerSetIdList);

    /**
     * 根据规则集ID查询规则集名称
     *
     * @return string
     */
    String queryCheckerSetNameByCheckerSetId(String checkerSetId);

    /**
     * 获取任务关联的规则
     *
     * @param projectId
     * @param taskIdList
     * @param toolNameList
     * @param needProps
     * @return
     */
    List<CheckerSetVO> getTaskCheckerSetsCore(
            String projectId,
            List<Long> taskIdList,
            List<String> toolNameList,
            boolean needProps
    );
}
