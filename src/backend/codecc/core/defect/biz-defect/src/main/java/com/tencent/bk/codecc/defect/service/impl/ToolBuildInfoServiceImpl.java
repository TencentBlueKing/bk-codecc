package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildStackDao;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.vo.ToolBuildInfoReqVO;
import com.tencent.bk.codecc.defect.vo.ToolBuildStackReqVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.devops.common.api.CodeRepoVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ScanType;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工具构建信息服务实现类
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Service
@Slf4j
public class ToolBuildInfoServiceImpl implements ToolBuildInfoService {
    @Autowired
    private CodeRepoInfoRepository codeRepoRepository;

    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;

    @Autowired
    private ToolBuildStackDao toolBuildStackDao;

    @Autowired
    private Client client;

    @Autowired
    private TaskLogRepository taskLogRepository;

    /**
     * 查询工具构建信息
     *
     * @param analyzeConfigInfoVO
     * @return
     */
    @Override
    public AnalyzeConfigInfoVO getBuildInfo(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        // NOCC:VariableDeclarationUsageDistance(设计如此:)
        Integer scanType = analyzeConfigInfoVO.getScanType() == null
                ? ComConstants.ScanType.FULL.code : analyzeConfigInfoVO.getScanType();

        // 把接口传进来的scanType置空，确保返回的scanType一定表示的是强制全量，而不是用户设置的全量
        analyzeConfigInfoVO.setScanType(null);

        Long taskId = analyzeConfigInfoVO.getTaskId();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();
        ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findFirstByTaskIdAndToolName(taskId, toolName);
        if (null == toolBuildInfoEntity) {
            return analyzeConfigInfoVO;
        }
        analyzeConfigInfoVO.setBaseBuildId(toolBuildInfoEntity.getDefectBaseBuildId());

        // 加入上次扫描的仓库列表
        CodeRepoInfoEntity codeRepoInfoEntity =
                codeRepoRepository.findFirstByTaskIdAndBuildId(taskId, toolBuildInfoEntity.getDefectBaseBuildId());
        Set<String> lastRepoWhiteList = Sets.newHashSet();
        Set<String> lastRepoIds = Sets.newHashSet();
        Set<String> lastRepoUrls = Sets.newHashSet();
        List<String> lastRepoRelativePaths = Lists.newArrayList();
        if (codeRepoInfoEntity != null) {
            if (CollectionUtils.isNotEmpty(codeRepoInfoEntity.getRepoList())) {
                analyzeConfigInfoVO.setLastCodeRepos(Lists.newArrayList());
                for (CodeRepoEntity codeRepoEntity : codeRepoInfoEntity.getRepoList()) {
                    CodeRepoVO codeRepoVO = new CodeRepoVO();
                    BeanUtils.copyProperties(codeRepoEntity, codeRepoVO);
                    log.info("set codeRepoVO in lastRepoIds taskId: {}, buildId: {}, codeRepoVO: {}", taskId, buildId, codeRepoVO);
                    analyzeConfigInfoVO.getLastCodeRepos().add(codeRepoVO);
                    lastRepoIds.add(codeRepoEntity.getRepoId());
                    lastRepoUrls.add(PathUtils.formatRepoUrlToHttp(codeRepoEntity.getUrl()));
                }
            }
            if (CollectionUtils.isNotEmpty(codeRepoInfoEntity.getRepoWhiteList())) {
                lastRepoWhiteList.addAll(codeRepoInfoEntity.getRepoWhiteList());
            }
            if (CollectionUtils.isNotEmpty(codeRepoInfoEntity.getRepoRelativePathList())) {
                lastRepoRelativePaths.addAll(codeRepoInfoEntity.getRepoRelativePathList());
                log.info("last repoRelativePathList is: {}", lastRepoRelativePaths);
            }
        }

        // 如果设置了强制全量扫描标志，则本次全量扫描
        if (ComConstants.CommonJudge.COMMON_Y.value().equals(toolBuildInfoEntity.getForceFullScan())) {
            analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
        } else { // 流水线任务增量要判断代码库和白名单是否有变化, 如果修改过代码仓库列表或修改过扫描目录白名单，则本次全量扫描
            String atomCode = analyzeConfigInfoVO.getAtomCode();
            List<String> repoWhiteList = CollectionUtils.isEmpty(analyzeConfigInfoVO.getRepoWhiteList())
                    ? Lists.newArrayList() : analyzeConfigInfoVO.getRepoWhiteList();
            List<String> repoRelativePathList = CollectionUtils.isEmpty(analyzeConfigInfoVO.getRepoRelativePathList())
                    ? Lists.newArrayList() : analyzeConfigInfoVO.getRepoRelativePathList();
            log.info("repoRelativePathList is: {}", repoRelativePathList);
            if (!CollectionUtils.isEqualCollection(lastRepoWhiteList, repoWhiteList)) {
                log.info("repoWhiteList has changed, taskId: {}, toolName: {}, buildId: {}", taskId, toolName, buildId);
                analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
            } else if (!CollectionUtils.isEqualCollection(lastRepoRelativePaths, repoRelativePathList)) {
                log.info("repoRelativeList has changed, taskId:{}, toolName:{}, buildId:{}", taskId, toolName, buildId);
                analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
            } else if (StringUtils.isEmpty(atomCode)
                    || ComConstants.AtomCode.CODECC_V2.code().equalsIgnoreCase(atomCode)) {
                // V1、V2插件通过"repoIds"参数传递"代码仓库repoId列表"
                List<String> repoIds = CollectionUtils.isEmpty(analyzeConfigInfoVO.getRepoIds())
                        ? Lists.newArrayList() : analyzeConfigInfoVO.getRepoIds();
                if (!CollectionUtils.isEqualCollection(lastRepoIds, repoIds)) {
                    log.info("repoIds has changed, taskId: {}, toolName: {}, buildId: {}", taskId, toolName, buildId);
                    analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
                }
            } else { // V3插件通过参数"codeRepos"传递"本次扫描的代码仓库列表"
                Set<String> reqRepoUrls = CollectionUtils.isEmpty(analyzeConfigInfoVO.getCodeRepos())
                        ? Sets.newHashSet() : analyzeConfigInfoVO.getCodeRepos().stream()
                        .map(codeRepoVO -> PathUtils.formatRepoUrlToHttp(codeRepoVO.getUrl()))
                        .collect(Collectors.toSet());
                if (!CollectionUtils.isEqualCollection(lastRepoUrls, reqRepoUrls)) {
                    log.info("reqRepoUrls has changed, taskId: {}, toolName: {}, buildId: {}",
                            taskId, toolName, buildId);
                    analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
                }
            }
        }

        scanType = analyzeConfigInfoVO.getScanType() == null ? scanType : analyzeConfigInfoVO.getScanType();
        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(
                taskId, toolName, analyzeConfigInfoVO.getBuildId());
        if (toolBuildStackEntity == null) {
            // 保存构建运行时栈表
            log.info("set tool build stack, taskId:{}, toolNames:{}, scanType: {}",
                    analyzeConfigInfoVO.getTaskId(), analyzeConfigInfoVO.getMultiToolType(), scanType);
            toolBuildStackEntity = new ToolBuildStackEntity();
            toolBuildStackEntity.setTaskId(taskId);
            toolBuildStackEntity.setToolName(toolName);
            toolBuildStackEntity.setBuildId(analyzeConfigInfoVO.getBuildId());
            toolBuildStackEntity.setBaseBuildId(toolBuildInfoEntity.getDefectBaseBuildId());
            toolBuildStackEntity.setFullScan(scanType == ComConstants.ScanType.FULL.code
                    || scanType == ComConstants.ScanType.DIFF_MODE.code
                    || scanType == ComConstants.ScanType.FILE_DIFF_MODE.code
                    || scanType == ComConstants.ScanType.BRANCH_DIFF_MODE.code);
            toolBuildStackDao.upsert(toolBuildStackEntity);
        }

        log.info("get build info finish, taskId: {}, toolName: {}, buildId: {}, scanType: {}",
                taskId, toolName, buildId, analyzeConfigInfoVO.getScanType());
        return analyzeConfigInfoVO;
    }

    /**
     * 更新运行时栈表中的全量扫描标志位
     */
    @Override
    public Boolean setToolBuildStackFullScan(Long taskId, ToolBuildStackReqVO toolBuildStackReqVO) {
        log.info("begin setToolBuildStackFullScan: taskId={}, {}", taskId, toolBuildStackReqVO);
        String buildId = toolBuildStackReqVO.getLandunBuildId();
        List<String> toolNames = toolBuildStackReqVO.getToolNames();
        List<ToolBuildStackEntity> toolBuildStackEntitys =
                toolBuildStackRepository.findByTaskIdAndToolNameInAndBuildId(taskId, toolNames, buildId);
        if (CollectionUtils.isNotEmpty(toolBuildStackEntitys)) {
            toolBuildStackEntitys = toolBuildStackEntitys.stream()
                    .filter(toolBuildStackEntity -> !toolBuildStackEntity.isFullScan())
                    .map(toolBuildStackEntity -> {
                        toolBuildStackEntity.setFullScan(true);
                        return toolBuildStackEntity;
                    }).collect(Collectors.toList());
            toolBuildStackDao.batchUpsert(toolBuildStackEntitys);
        }

        setForceFullScan(taskId, toolNames);

        log.info("end setToolBuildStackFullScan.");
        return true;
    }

    @Override
    public Boolean setToolBuildStackCommitSince(
            Long taskId, ToolBuildStackReqVO toolBuildStackReqVO) {
        log.info("begin setToolBuildStackCommitSince: taskId={}, {}",
                taskId, toolBuildStackReqVO);
        String buildId = toolBuildStackReqVO.getLandunBuildId();
        ToolBuildStackEntity toolBuildStackEntity =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(
                        taskId, toolBuildStackReqVO.getToolName(), buildId);
        if (toolBuildStackEntity != null) {
            toolBuildStackEntity.setCommitSince(toolBuildStackReqVO.getCommitSince());
            toolBuildStackDao.upsert(toolBuildStackEntity);
        }

        log.info("end setToolBuildStackCommitSince.");
        return true;
    }

    @Override
    public Long getToolBuildStackCommitSince(Long taskId, ToolBuildStackReqVO toolBuildStackReqVO) {
        ToolBuildInfoEntity toolBuildInfo = toolBuildInfoRepository.findFirstByTaskIdAndToolName(
                taskId, toolBuildStackReqVO.getToolName());
        if (toolBuildInfo == null || toolBuildInfo.getCommitSince() == null) {
            return 0L;
        }

        return toolBuildInfo.getCommitSince();
    }

    /**
     * 更新强制全量扫描标志位
     */
    @Override
    public Boolean setForceFullScan(Long taskId, List<String> toolNames) {
        if (CollectionUtils.isNotEmpty(toolNames)) {
            log.info("start to set full scan for task: {}, {}", taskId, toolNames);
            for (String toolName : toolNames) {
                toolBuildInfoDao.setForceFullScan(taskId, toolName);
            }
        }
        return true;
    }

    /**
     * 编辑单个工具构建信息
     *
     * @param reqVO 请求体
     * @return boolean
     */
    @Override
    public Boolean editOneToolBuildInfo(ToolBuildInfoReqVO reqVO) {
        log.info("OP editOneToolBuildInfo reqVO[{}]", reqVO);

        // 编辑单个工具构建信息
        toolBuildInfoDao.editOneToolBuildInfo(reqVO);

        return true;
    }

    /**
     * 批量编辑工具构建信息
     *
     * @param reqVO 请求体
     * @return boolean
     */
    @Override
    public Boolean editToolBuildInfo(ToolBuildInfoReqVO reqVO) {
        log.info("OP editToolBuildInfo reqVO[{}]", reqVO);
        // 根据来源获取taskId集合
        List<Long> createFromTaskIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(reqVO.getCreateFrom())) {
            List<String> createFrom = new ArrayList<>(reqVO.getCreateFrom());
            createFromTaskIdList =
                    client.get(ServiceTaskRestResource.class).queryTaskIdByCreateFrom(createFrom).getData();
        }

        // 获取所属来源taskId集合和用户输入的taskId集合的 交集
        List<Long> taskIdList;
        if (CollectionUtils.isNotEmpty(reqVO.getTaskIds())) {
            taskIdList = List2StrUtil.intersectionList(createFromTaskIdList, new ArrayList<>(reqVO.getTaskIds()));
        } else {
            assert createFromTaskIdList != null;
            taskIdList = new ArrayList<>(createFromTaskIdList);
        }
        // 根据查询条件获取批量编辑的数据
        toolBuildInfoDao.editToolBuildInfo(taskIdList, reqVO.getToolNames());

        return true;
    }

    /**
     * 批量设置强制全量扫描
     *
     * @param taskIdSet 任务id集合
     * @param toolName  工具名
     * @return Boolean
     */
    @Override
    public Boolean batchSetForceFullScan(Collection<Long> taskIdSet, String toolName) {
        log.info("batchSetForceFullScan toolName: {}, taskIdSet size: {}", toolName,
                CollectionUtils.isEmpty(taskIdSet) ? 0 : taskIdSet.size());

        if (CollectionUtils.isEmpty(taskIdSet) || StringUtils.isBlank(toolName)) {
            log.warn("batchSetForceFullScan fail! param is empty");
            return false;
        }
        toolBuildInfoDao.editToolBuildInfo(taskIdSet, Lists.newArrayList(toolName));
        return true;
    }

    /**
     * 若是重试触发的增量扫描，则设full_scan为false
     *
     * @param taskId
     * @param toolName
     * @param buildId
     */
    @Override
    public void setToolBuildStackNotFullScanIfRebuildIncr(
            Long taskId,
            String toolName,
            String buildId,
            Integer scanType
    ) {
        if (scanType == null || ScanType.INCREMENTAL.code != scanType) {
            return;
        }

        ToolBuildStackEntity toolBuildStack =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (toolBuildStack == null || !toolBuildStack.isFullScan()) {
            return;
        }

        TaskLogEntity taskLog = taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (taskLog == null || CollectionUtils.isEmpty(taskLog.getStepArray())) {
            return;
        }

        boolean isRebuild = taskLog.getStepArray().stream()
                .anyMatch(x -> x.getStepNum() == ComConstants.Step4MutliTool.COMMIT.value()
                        && x.getFlag() == ComConstants.StepFlag.SUCC.value());

        if (isRebuild) {
            String remark = StringUtils.isEmpty(toolBuildStack.getRemark())
                    ? String.format("rebuild: %d", System.currentTimeMillis())
                    : String.format("%s, rebuild: %d", toolBuildStack.getRemark(), System.currentTimeMillis());
            toolBuildStack.setRemark(remark);
            toolBuildStack.setFullScan(false);
            toolBuildStackRepository.save(toolBuildStack);

            log.info("rebuild incr, task id: {}, build id: {}, tool: {}, scan type: {}",
                    taskId, buildId, toolName, scanType);
        }
    }
}
