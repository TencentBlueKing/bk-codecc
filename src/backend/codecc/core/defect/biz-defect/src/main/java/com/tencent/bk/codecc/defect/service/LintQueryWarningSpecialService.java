package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVOBase.CheckerSet;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.util.Pair;

/**
 * 减少原代码侵入，数据迁移后，前端lint服务扩展
 */
public interface LintQueryWarningSpecialService {

    /**
     * 获取作者下拉列表、规则下拉列表、文件树
     *
     * @param taskId
     * @param toolName
     * @param dimension
     * @param statusSet
     * @param checkerSet
     * @param buildId
     * @param dataMigrationSuccessful
     * @return
     */
    QueryWarningPageInitRspVO processQueryWarningPageInitRequest(
            List<Long> taskId,
            String toolName,
            String dimension,
            Set<String> statusSet,
            String checkerSet,
            String buildId,
            Boolean dataMigrationSuccessful
    );

    /**
     * 获取快照告警Id
     *
     * @param taskToolMap
     * @param buildId
     * @return first -> mongodb主键, second -> 第三方平台主键id
     */
    Pair<Set<String>, Set<String>> getDefectIdsPairByBuildId(
            Map<Long, List<String>> taskToolMap,
            String buildId
    );

    /**
     * 根据工具列表、维度获取规则
     *
     * @param checkerSet 规则集
     * @param checker 当checkerSet不为空时，才会校验checker归属
     * @param toolNameList
     * @param dimensionList
     * @return
     */
    Set<String> getCheckers(
            CheckerSet checkerSet,
            String checker,
            List<String> toolNameList,
            List<String> dimensionList
    );

    /**
     * 根据工具列表、维度获取规则
     *
     * @param checkerSet 规则集
     * @param checker 当checkerSet不为空时，才会校验checker归属
     * @param toolNameList
     * @param dimensionList
     * @return
     */
    List<CheckerDetailVO> getCheckerDetails(
            CheckerSet checkerSet,
            String checker,
            List<String> toolNameList,
            List<String> dimensionList
    );
}
