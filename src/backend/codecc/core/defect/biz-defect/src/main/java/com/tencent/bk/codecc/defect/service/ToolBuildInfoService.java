package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.vo.ToolBuildStackReqVO;
import com.tencent.bk.codecc.defect.vo.ToolBuildInfoReqVO;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;

import java.util.Collection;
import java.util.List;

/**
 * 工具构建信息服务
 *
 * @version V1.0
 * @date 2019/11/17
 */
public interface ToolBuildInfoService {
    /**
     * 查询工具构建信息
     * @param analyzeConfigInfoVO
     * @return
     */
    AnalyzeConfigInfoVO getBuildInfo(AnalyzeConfigInfoVO analyzeConfigInfoVO);

    Boolean setToolBuildStackFullScan(Long taskId, ToolBuildStackReqVO toolBuildStackReqVO);

    Boolean setToolBuildStackCommitSince(Long taskId, ToolBuildStackReqVO toolBuildStackReqVO);

    Long getToolBuildStackCommitSince(Long taskId, ToolBuildStackReqVO toolBuildStackReqVO);

    /**
     * 更新强制全量扫描标志位
     */
    Boolean setForceFullScan(Long taskId, List<String> toolNames);

    /**
     * 编辑单个工具构建信息
     *
     * @param reqVO 请求体
     * @return boolean
     */
    Boolean editOneToolBuildInfo(ToolBuildInfoReqVO reqVO);

    /**
     * 批量编辑工具构建信息
     *
     * @param reqVO 请求体
     * @return boolean
     */
    Boolean editToolBuildInfo(ToolBuildInfoReqVO reqVO);

    Boolean batchSetForceFullScan(Collection<Long> taskIdSet, String toolName);

    /**
     * 若是重试触发的增量扫描，则设full_scan为false
     *
     * @param taskId
     * @param toolName
     * @param buildId
     */
    void setToolBuildStackNotFullScanIfRebuildIncr(Long taskId, String toolName, String buildId, Integer scanType);

    /**
     * 获取基准buildId
     *
     * @param toolBuildStack
     * @param taskId
     * @param toolName
     * @param curBuildId
     * @param forBuildSnapshot 标识是否快照业务
     * @return
     */
    String getBaseBuildIdWhenDefectCommit(
            ToolBuildStackEntity toolBuildStack,
            Long taskId,
            String toolName,
            String curBuildId,
            boolean forBuildSnapshot
    );
}
