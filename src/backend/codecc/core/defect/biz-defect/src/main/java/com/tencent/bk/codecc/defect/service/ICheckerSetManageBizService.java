package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.vo.UpdateAllCheckerReq;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetManagementReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqExtVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqVO;
import java.util.List;
import org.springframework.data.util.Pair;

/**
 * V3规则集管理服务
 *
 * @version V1.0
 * @date 2020/1/2
 */
public interface ICheckerSetManageBizService {

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
     *
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
     * @param versionAndTool 指定version时也必须指定tool
     */
    void updateCheckersOfSet(String checkerSetId, String user, List<CheckerPropVO> checkerProps,
            Pair<Integer, String> versionAndTool);

    /**
     * 刷新本次规则更新涉及的任务的信息。包括强制全量标志，工具，告警状态等
     *
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
     *
     * @param checkerSetEntity
     * @param fromCheckerSet
     * @param version
     * @param user
     */
    void updateTaskAfterChangeCheckerSet(
            CheckerSetEntity checkerSetEntity, CheckerSetEntity fromCheckerSet, int version, String user);

    /**
     * 修改规则集基础信息
     *
     * @param checkerSetId
     * @param projectId
     * @param updateCheckerSetReq
     */
    void updateCheckerSetBaseInfo(String checkerSetId, String projectId, V3UpdateCheckerSetReqVO updateCheckerSetReq);

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
     */
    Pair<Boolean, String> setRelationshipsOnce(String user, String projectId, long taskId, String toolName);

    /**
     * 批量关联任务和规则集
     *
     * @param projectId
     * @param taskId
     * @param checkerSetList
     * @param user
     * @return
     */
    Boolean batchRelateTaskAndCheckerSet(String projectId, Long taskId, List<CheckerSetVO> checkerSetList, String user,
            Boolean isOpenSource);

    /**
     * 规则集管理
     *
     * @param user
     * @param checkerSetId
     * @param checkerSetManagementVO
     */
    void management(String user, String checkerSetId, CheckerSetManagementReqVO checkerSetManagementVO);

    /**
     * 根据任务id和语言解绑
     *
     * @param taskId
     * @param codeLang
     * @return
     */
    Boolean updateCheckerSetAndTaskRelation(Long taskId, Long codeLang, String user);

    Boolean updateCheckerSetBaseInfoByOp(String userName, V3UpdateCheckerSetReqExtVO checkerSetVO);
}
