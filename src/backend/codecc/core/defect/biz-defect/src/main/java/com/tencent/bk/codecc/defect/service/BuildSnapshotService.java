package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.BuildDefectSummaryEntity;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.pojo.HandlerDTO;
import com.tencent.bk.codecc.defect.vo.common.BuildWithBranchVO;
import java.util.List;
import org.springframework.data.util.Pair;

public interface BuildSnapshotService {

    /**
     * 保存快照概要
     *
     * @param dto
     */
    void saveBuildSnapshotSummary(HandlerDTO dto);

    /**
     * 获取最近快照概要
     *
     * @param taskId
     * @return
     */
    List<BuildWithBranchVO> getRecentBuildSnapshotSummary(Long taskId);

    /**
     * 保存Lint告警快照
     *
     * @param taskId
     * @param toolName
     * @param buildEntity
     * @param allNewDefectList
     */
    void saveLintBuildDefect(
            long taskId, String toolName, BuildEntity buildEntity, List<LintDefectV2Entity> allNewDefectList,
            List<LintDefectV2Entity> allIgnoreDefectList
    );

    /**
     * 保存CCN告警快照
     *
     * @param taskId
     * @param toolName
     * @param buildEntity
     * @param allNewDefectList
     */
    void saveCCNBuildDefect(
            long taskId, String toolName, BuildEntity buildEntity, List<CCNDefectEntity> allNewDefectList,
            List<CCNDefectEntity> allIgnoreDefectList
    );

    /**
     * 保存Common告警快照
     *
     * @param taskId
     * @param toolName
     * @param buildEntity
     * @param allNewDefectList
     */
    void saveCommonBuildDefect(
            long taskId, String toolName, BuildEntity buildEntity, List<CommonDefectEntity> allNewDefectList,
            List<CommonDefectEntity> allIgnoreDefectList
    );

    /**
     * 获取该告警在"同分支"的最后一次扫描中是否已被修复了
     *
     * @param taskId
     * @param buildId
     * @param defectId 注意Common传的是"id"，Lint跟CCN传的是"_id"
     * @return key为最后的构建号buildNum；value为修复状态，true为已修复，false反之
     */
    Pair<String, Boolean> getDefectFixedStatusOnLastBuild(long taskId, String buildId, String defectId);

    /**
     * 获取告警在哪次构建被修复了
     *
     * @param taskId 任务Id
     * @param selectedBuildId 选中的构建Id
     * @param branch 分支名
     * @param defectId 告警Id
     * @return 构建号；若返回0则代表还未修复
     */
    int getBuildNumOfConvertToFixed(long taskId, String selectedBuildId, String branch, String defectId);

    /**
     * 获取概要信息
     *
     * @param taskId
     * @param buildId
     * @return
     */
    BuildDefectSummaryEntity getSummary(long taskId, String buildId);


    /**
     * 获取最后的一次概要信息
     *
     * @param taskId
     * @return
     */
    BuildDefectSummaryEntity getLatestSummaryByTaskId(long taskId);
}
