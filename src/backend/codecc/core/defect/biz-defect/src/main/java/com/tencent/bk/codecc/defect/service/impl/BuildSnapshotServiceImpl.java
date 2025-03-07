package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildDefectSummaryRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.BuildDefectSummaryDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.BuildDefectV2Dao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.model.BuildDefectSummaryEntity;
import com.tencent.bk.codecc.defect.model.BuildDefectSummaryEntity.CodeRepo;
import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel;
import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.pojo.HandlerDTO;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.sca.SCALicenseService;
import com.tencent.bk.codecc.defect.service.sca.SCASbomService;
import com.tencent.bk.codecc.defect.service.sca.SCAVulnerabilityService;
import com.tencent.bk.codecc.defect.vo.common.BuildWithBranchVO;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.util.BeanUtils;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BuildSnapshotServiceImpl implements BuildSnapshotService {

    @Autowired
    private BuildDefectV2Repository buildDefectV2Repository;
    @Autowired
    private BuildDefectV2Dao buildDefectV2Dao;
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;
    @Autowired
    private BuildDefectSummaryDao buildDefectSummaryDao;
    @Autowired
    private BuildDefectSummaryRepository buildDefectSummaryRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private BuildRepository buildRepository;
    @Autowired
    private CodeRepoInfoRepository codeRepoInfoRepository;
    @Autowired
    private BaseDataCacheService baseDataCacheService;

    @Autowired
    private SCASbomService scaSbomService;

    @Autowired
    private SCALicenseService scaLicenseService;

    @Autowired
    private SCAVulnerabilityService scaVulnerabilityService;

    @Override
    public void saveBuildSnapshotSummary(HandlerDTO dto) {
        RedisLock redisLock = null;

        try {
            // 增量添加工具
            BuildDefectSummaryEntity summaryEntity =
                    buildDefectSummaryRepository.findFirstByTaskIdAndBuildId(dto.getTaskId(), dto.getBuildId());
            if (summaryEntity != null) {
                buildDefectSummaryDao.addToolToSummary(dto.getTaskId(), dto.getBuildId(), dto.getToolName());
                return;
            }

            // 分布式锁仅在新增document时适用，这里锁竞争是极低的，SCC往往执行得最快；增量添加工具，$addToSet原子性操作
            String redisKey = String.format("END_REPORT_FOR_SAVE_SNAPSHOT:%s:%s", dto.getTaskId(), dto.getBuildId());
            redisLock = new RedisLock(redisTemplate, redisKey, TimeUnit.SECONDS.toSeconds(3));
            redisLock.lock();

            // double-check
            summaryEntity = buildDefectSummaryRepository.findFirstByTaskIdAndBuildId(dto.getTaskId(), dto.getBuildId());
            if (summaryEntity != null) {
                buildDefectSummaryDao.addToolToSummary(dto.getTaskId(), dto.getBuildId(), dto.getToolName());
                return;
            }

            // 新增插入记录
            summaryEntity = new BuildDefectSummaryEntity();
            summaryEntity.setTaskId(dto.getTaskId());
            summaryEntity.setBuildId(dto.getBuildId());
            summaryEntity.setToolList(Lists.newArrayList(dto.getToolName()));

            BuildEntity buildEntity = buildRepository.findFirstByBuildId(dto.getBuildId());
            if (buildEntity != null) {
                summaryEntity.setBuildNum(buildEntity.getBuildNo());
                summaryEntity.setBuildTime(buildEntity.getBuildTime());
                summaryEntity.setBuildUser(buildEntity.getBuildUser());
            }

            CodeRepoInfoEntity codeRepoInfoEntity =
                    codeRepoInfoRepository.findFirstByTaskIdAndBuildId(dto.getTaskId(), dto.getBuildId());
            if (codeRepoInfoEntity != null && CollectionUtils.isNotEmpty(codeRepoInfoEntity.getRepoList())) {
                List<CodeRepo> codeRepoList = codeRepoInfoEntity.getRepoList().stream()
                        .filter(x -> StringUtils.isNotEmpty(x.getUrl()) && StringUtils.isNotEmpty(x.getBranch()))
                        .map(x -> new CodeRepo(x.getUrl(), x.getBranch()))
                        .collect(Collectors.toList());
                summaryEntity.setRepoInfo(codeRepoList);
            }

            buildDefectSummaryRepository.save(summaryEntity);
        } finally {
            if (redisLock != null && redisLock.isLocked()) {
                redisLock.unlock();
            }
        }
    }

    @Override
    public List<BuildWithBranchVO> getRecentBuildSnapshotSummary(Long taskId) {
        int limit = baseDataCacheService.getMaxBuildListSize();
        List<BuildDefectSummaryEntity> summaryEntityList =
                buildDefectSummaryRepository.findByTaskIdOrderByBuildTimeDesc(taskId, PageRequest.of(0, limit));

        if (CollectionUtils.isEmpty(summaryEntityList)) {
            return Lists.newArrayList();
        }

        return summaryEntityList.stream().map(source -> {
            BuildWithBranchVO vo = new BuildWithBranchVO();
            BeanUtils.copyProperties(source, vo);
            vo.setBranch(CollectionUtils.isNotEmpty(source.getRepoInfo())
                    ? source.getRepoInfo().get(0).getBranch()
                    : "");

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveLintBuildDefect(
            long taskId,
            String toolName,
            BuildEntity buildEntity,
            List<LintDefectV2Entity> allNewDefectList,
            List<LintDefectV2Entity> allIgnoreDefectList
    ) {
        int size = (CollectionUtils.isNotEmpty(allNewDefectList) ? allNewDefectList.size() : 0)
                + (CollectionUtils.isNotEmpty(allIgnoreDefectList) ? allIgnoreDefectList.size() : 0);
        List<BuildDefectV2Entity> insertList = Lists.newArrayListWithExpectedSize(size);
        if (CollectionUtils.isNotEmpty(allNewDefectList)) {
            insertList.addAll(allNewDefectList.stream()
                    .filter(defect -> defect.getStatus() == DefectStatus.NEW.value())
                    .map(defect ->
                            constructBuildDefectV2Entity(taskId, toolName, buildEntity.getBuildId(),
                                    buildEntity.getBuildNo(), defect.getEntityId(), defect.getRevision(),
                                    defect.getBranch(), defect.getSubModule(), defect.getLineNum(),
                                    null, null
                            )
                    ).collect(Collectors.toList()));
        }

        if (CollectionUtils.isNotEmpty(allIgnoreDefectList)) {
            insertList.addAll(allIgnoreDefectList.stream()
                    .filter(defect -> (defect.getStatus() & DefectStatus.IGNORE.value()) > 0
                            && (defect.getStatus() & DefectStatus.FIXED.value()) == 0)
                    .map(defect ->
                            constructBuildDefectV2Entity(taskId, toolName, buildEntity.getBuildId(),
                                    buildEntity.getBuildNo(), defect.getEntityId(), defect.getRevision(),
                                    defect.getBranch(), defect.getSubModule(), defect.getLineNum(),
                                    null, null
                            )
                    ).collect(Collectors.toList()));
        }

        if (CollectionUtils.isNotEmpty(insertList)) {
            buildDefectV2Dao.save(insertList);
            insertList.clear();
            insertList = null;
        }

        // 更新下次扫描告警快照基准构建号
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildEntity.getBuildId());
    }

    @Override
    public void saveCCNBuildDefect(
            long taskId,
            String toolName,
            BuildEntity buildEntity,
            List<CCNDefectEntity> allNewDefectList,
            List<CCNDefectEntity> allIgnoreDefectList
    ) {
        int size = (allNewDefectList != null ? allNewDefectList.size() : 0)
                + (allIgnoreDefectList != null ? allIgnoreDefectList.size() : 0);
        List<BuildDefectV2Entity> insertList = Lists.newArrayListWithExpectedSize(size);

        if (CollectionUtils.isNotEmpty(allNewDefectList)) {
            insertList.addAll(allNewDefectList.stream()
                    .filter(defect -> defect.getStatus() == DefectStatus.NEW.value())
                    .map(defect ->
                            constructBuildDefectV2Entity(taskId, toolName, buildEntity.getBuildId(),
                                    buildEntity.getBuildNo(), defect.getEntityId(),
                                    defect.getRevision(), defect.getBranch(), defect.getSubModule(), null,
                                    defect.getStartLines(), defect.getEndLines()
                            )
                    ).collect(Collectors.toList())
            );
        }

        if (CollectionUtils.isNotEmpty(allIgnoreDefectList)) {
            insertList.addAll(allIgnoreDefectList.stream()
                    .filter(defect -> (defect.getStatus() & DefectStatus.IGNORE.value()) > 0
                            && (defect.getStatus() & DefectStatus.FIXED.value()) == 0)
                    .map(defect ->
                            constructBuildDefectV2Entity(taskId, toolName, buildEntity.getBuildId(),
                                    buildEntity.getBuildNo(), defect.getEntityId(),
                                    defect.getRevision(), defect.getBranch(), defect.getSubModule(), null,
                                    defect.getStartLines(), defect.getEndLines()
                            )
                    ).collect(Collectors.toList())
            );
        }

        if (insertList.size() > 0) {
            buildDefectV2Dao.save(insertList);
            insertList.clear();
            insertList = null;
        }

        // 更新下次扫描告警快照基准构建号
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildEntity.getBuildId());
    }

    @Override
    public void saveCommonBuildDefect(
            long taskId,
            String toolName,
            BuildEntity buildEntity,
            List<CommonDefectEntity> allNewDefectList,
            List<CommonDefectEntity> allIgnoreDefectList
    ) {
        if (CollectionUtils.isNotEmpty(allNewDefectList)) {
            List<BuildDefectV2Entity> insertList = allNewDefectList.stream()
                    .filter(defect -> defect.getStatus() == DefectStatus.NEW.value())
                    .map(defect ->
                            constructBuildDefectV2Entity(taskId, toolName, buildEntity.getBuildId(),
                                    buildEntity.getBuildNo(), defect.getId(),
                                    defect.getRevision(), null, null, defect.getLineNum(),
                                    null, null
                            )
                    ).collect(Collectors.toList());

            buildDefectV2Dao.save(insertList);
        }

        if (CollectionUtils.isNotEmpty(allIgnoreDefectList)) {
            List<BuildDefectV2Entity> insertList = allIgnoreDefectList.stream()
                    .filter(defect -> (defect.getStatus() & DefectStatus.IGNORE.value()) > 0
                            && (defect.getStatus() & DefectStatus.FIXED.value()) == 0)
                    .map(defect ->
                            constructBuildDefectV2Entity(taskId, toolName, buildEntity.getBuildId(),
                                    buildEntity.getBuildNo(), defect.getId(),
                                    defect.getRevision(), null, null, defect.getLineNum(),
                                    null, null
                            )
                    ).collect(Collectors.toList());

            buildDefectV2Dao.save(insertList);
        }

        // 更新下次扫描告警快照基准构建号
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildEntity.getBuildId());
    }

    @Override
    public Pair<String, Boolean> getDefectFixedStatusOnLastBuild(long taskId, String buildId, String defectId) {
        BuildDefectSummaryEntity buildSummary
                = buildDefectSummaryRepository.findFirstByTaskIdAndBuildId(taskId, buildId);
        if (buildSummary == null || CollectionUtils.isEmpty(buildSummary.getRepoInfo())) {
            log.info("get defect status on last build, repo info is empty, task id {}, build id {}", taskId, buildId);
            return Pair.of("", false);
        }

        // 多仓库的话，只取首仓库
        String branch = buildSummary.getRepoInfo().get(0).getBranch();
        BuildDefectSummaryEntity lastBuildSummary = buildDefectSummaryDao.findLastByTaskIdAndBranch(taskId, branch);
        if (lastBuildSummary == null) {
            return Pair.of("", false);
        }

        String lastBuildId = lastBuildSummary.getBuildId();
        log.info("getDefectFixedStatusOnLastBuild, taskId: {}, buildId: {}, defectId: {}, lastBuildId: {}",
                taskId,
                buildId,
                defectId,
                lastBuildId);

        // 若当前选的构建已经是最后一次扫描
        if (lastBuildId.equals(buildId)) {
            return Pair.of(lastBuildSummary.getBuildNum(), false);
        }

        boolean isFixed =
                buildDefectV2Repository.findFirstByTaskIdAndBuildIdAndDefectId(taskId, lastBuildId, defectId) == null;

        return Pair.of(lastBuildSummary.getBuildNum(), isFixed);
    }

    @Override
    public int getBuildNumOfConvertToFixed(long taskId, String selectedBuildId, String branch, String defectId) {
        List<BuildDefectSummaryEntity> buildSummaryList = buildDefectSummaryDao.findByTaskIdAndBranch(taskId, branch);
        BuildDefectSummaryEntity selectedBuildSummary = buildSummaryList.stream()
                .filter(x -> x.getBuildId().equals(selectedBuildId))
                .findFirst()
                .orElse(null);

        // 选中的快照信息都不存在，直接跳过
        if (selectedBuildSummary == null) {
            return 0;
        }

        // 同分支之后的所有构建Id
        Set<String> buildIdSet = buildSummaryList.stream()
                .filter(x -> x.getBuildTime() >= selectedBuildSummary.getBuildTime())
                .map(BuildDefectSummaryEntity::getBuildId)
                .collect(Collectors.toSet());

        // 快照信息还存在的，则为未修复
        List<BuildDefectV2Entity> buildDefectList =
                buildDefectV2Repository.findByTaskIdAndBuildIdInAndDefectId(taskId, buildIdSet, defectId);

        // 未修复状态的最大构建号
        int maxBuildNumOnUnFixed = buildDefectList.stream()
                .mapToInt(x -> Integer.parseInt(x.getBuildNum()))
                .max()
                .orElse(0);
        if (maxBuildNumOnUnFixed == 0) {
            return 0;
        }

        int minBuildNumOnFixed = buildSummaryList.stream()
                .mapToInt(x -> Integer.parseInt(x.getBuildNum()))
                .filter(x -> x > maxBuildNumOnUnFixed)
                .min()
                .orElse(0);

        return minBuildNumOnFixed;
    }

    @Override
    public BuildDefectSummaryEntity getSummary(long taskId, String buildId) {
        return buildDefectSummaryRepository.findFirstByTaskIdAndBuildId(taskId, buildId);
    }

    @Override
    public BuildDefectSummaryEntity getLatestSummaryByTaskId(long taskId) {
        return buildDefectSummaryDao.findLastByTaskId(taskId);
    }

    @Override
    public void saveSCASnapshot(long taskId, String toolName, String buildId, BuildEntity buildEntity,
            SCASbomAggregateModel aggregateModel, List<SCAVulnerabilityEntity> vulnerabilities,
            List<SCALicenseEntity> licenses) {
        // 后续SCAVulnerabilityEntity可能需要录入lint告警库，与build_defect_v2
        scaSbomService.saveSCASbomSnapshot(taskId, toolName, buildId, buildEntity, aggregateModel);
        scaVulnerabilityService.saveBuildVulnerabilities(taskId, toolName, buildId, buildEntity, vulnerabilities);
        scaLicenseService.saveBuildLicenses(taskId, toolName, buildId, buildEntity, licenses);
    }

    private BuildDefectV2Entity constructBuildDefectV2Entity(long taskId, String toolName, String buildId,
            String buildNum, String defectId, String revision,
            String branch, String subModule, Integer lineNum,
            Integer startLines, Integer endLines) {
        BuildDefectV2Entity entity = new BuildDefectV2Entity();
        entity.setTaskId(taskId);
        entity.setToolName(toolName);
        entity.setBuildId(buildId);
        entity.setBuildNum(buildNum);
        entity.setDefectId(defectId);
        entity.setRevision(revision);
        entity.setBranch(branch);
        entity.setSubModule(subModule);
        entity.setLineNum(lineNum);
        entity.setStartLines(startLines);
        entity.setEndLines(endLines);
        return entity;
    }
}
