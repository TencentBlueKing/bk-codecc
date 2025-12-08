package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.SCALicenseRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.SCASbomPackageRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.SCASbomPackageDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.SCAVulnerabilityDao;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity;
import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.service.sca.AbstractSCAQueryWarningService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.utils.SCAUtils;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCALicenseVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAPackageDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAPackageDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAPackageDetailVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAPackageVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAQueryWarningPageInitRspVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAVulnerabilityVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.GsonUtils;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("SCAPackageQueryWarningBizService")
public class SCAPackageQueryWarningBizServiceImpl extends AbstractSCAQueryWarningService {
    @Autowired
    private SCAVulnerabilityDao scaVulnerabilityDao;

    @Autowired
    private SCASbomPackageDao scaSbomPackageDao;

    @Autowired
    private SCALicenseRepository scaLicenseRepository;

    @Autowired
    private SCASbomPackageRepository scaSbomPackageRepository;

    @Autowired
    protected TaskLogService taskLogService;

    /**
     * 处理SCA组件列表查询
     *
     * @param scaQueryWarningParams
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @return
     */
    @Override
    public SCAPackageDefectQueryRspVO processQueryWarningRequest(
            SCAQueryWarningParams scaQueryWarningParams,
            int pageNum,
            int pageSize,
            String sortField,
            Sort.Direction sortType
    ) {
        SCAPackageDefectQueryRspVO scaPackageDefectQueryRspVO =
                new SCAPackageDefectQueryRspVO();
        scaPackageDefectQueryRspVO.setPackageList(
                new Page<>(0, pageNum, pageSize, 0, Lists.newArrayList()));
        SCADefectQueryReqVO request = scaQueryWarningParams.getScaDefectQueryReqVO();
        if (request == null) {
            log.info("sca package query request after processed is null");
            return scaPackageDefectQueryRspVO;
        }

        // 处理快照请求参数buildId: 筛选出执行成功的快照的告警id列表
        Map<Long, List<String>> taskToolMap = scaQueryWarningParams.getTaskToolMap();
        Map.Entry<Long, List<String>> firstEntry = taskToolMap.entrySet().iterator().next();

        Long taskId = firstEntry.getKey();
        List<String> toolNameList = firstEntry.getValue();
        String buildId = request.getBuildId();
        List<String> scaDimensionList = request.getScaDimensionList();

        // 获取快照中的所有告警id
        Set<String> defectMongoIdSet = StringUtils.isNotBlank(buildId)
                ? SCAUtils.getPackageEntityIdsByBuildId(taskId, toolNameList, buildId, scaDimensionList)
                : Sets.newHashSet();

        scaQueryWarningParams.setScaDefectMongoIdSet(defectMongoIdSet);

        // 更新请求中的状态筛选条件
        Set<String> statusProcessed = ParamUtils.getStatusProcessed(request.getStatus());
        request.setStatus(statusProcessed);

        // 查询告警
        Page<SCASbomPackageEntity> result = scaSbomPackageDao.findSCASbomPackagePageByCondition(
                scaQueryWarningParams,
                pageNum,
                pageSize,
                sortField,
                sortType
        );

        log.info("SCA package list query defectMongoIdSet size:{}, result records size:{},taskId {}, buildId {}",
                defectMongoIdSet.size(),
                JsonUtil.INSTANCE.toJson(result.getRecords().size()),
                request.getTaskIdList(),
                buildId
        );

        List<SCAPackageVO> scaPackageVOList;
        Map<Integer, Integer> severityCountMap = new HashMap<>();
        Map<Integer, Integer> statusCountMap = new HashMap<>();
        List<SCASbomPackageEntity> records = result.getRecords();
        if (CollectionUtils.isNotEmpty(records)) {
            // 1. 收集所有许可证名称
            Set<String> affectedLicenseNames = new HashSet<>();
            for (SCASbomPackageEntity record : records) {
                if (CollectionUtils.isNotEmpty(record.getLicenses())) {
                    affectedLicenseNames.addAll(record.getLicenses());
                }
            }

            // 2. 批量查询所有许可证实体
            Map<String, SCALicenseEntity> licenseMap = new HashMap<>();
            if (!affectedLicenseNames.isEmpty()) {
                List<SCALicenseEntity> licenseEntities = scaLicenseRepository.findByTaskIdAndToolNameInAndNameIn(
                        taskId,
                        toolNameList,
                        new ArrayList<>(affectedLicenseNames)
                );

                // 构建许可证名称到实体的映射
                licenseEntities.forEach(license ->
                        licenseMap.put(license.getName(), license)
                );
            }

            // 3. 批量统计所有漏洞
            List<String> packageIds = records.stream().map(SCASbomPackageEntity::getPackageId)
                    .collect(Collectors.toList());
            List<SCADefectGroupStatisticVO> vulStatistics =
                    scaVulnerabilityDao.statisticByPackageIdAndSeverity(taskId, toolNameList, packageIds);
            Map<String, Map<Integer, Integer>> vulStatisticsMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(vulStatistics)) {
                vulStatistics.forEach(statistic -> {
                    Map<Integer, Integer> severityCount = vulStatisticsMap.getOrDefault(statistic.getPackageId(),
                            new HashMap<>());
                    severityCount.put(statistic.getSeverity(), statistic.getDefectCount());
                    vulStatisticsMap.put(statistic.getPackageId(), severityCount);
                });
            }

            // 4. 使用批量查询结果处理记录
            scaPackageVOList = records.stream().map(scaSbomPackageEntity -> {
                // 实体类转视图类
                SCAPackageVO scaPackageVO = new SCAPackageVO();
                BeanUtils.copyProperties(scaSbomPackageEntity, scaPackageVO);

                // 判断是否直接依赖
                scaPackageVO.setDirect(scaSbomPackageEntity.getDepth() <= 1);

                // 设置漏洞统计信息
                if (vulStatisticsMap.containsKey(scaSbomPackageEntity.getPackageId())) {
                    Map<Integer, Integer> severityCount =
                            vulStatisticsMap.getOrDefault(scaSbomPackageEntity.getPackageId(), new HashMap<>());
                    scaPackageVO.setHighCount(severityCount.getOrDefault(ComConstants.SERIOUS, 0));
                    scaPackageVO.setMiddleCount(severityCount.getOrDefault(ComConstants.NORMAL, 0));
                    scaPackageVO.setLowCount(severityCount.getOrDefault(ComConstants.PROMPT, 0));
                    scaPackageVO.setUnknownCount(severityCount.getOrDefault(ComConstants.UNKNOWN, 0));
                }

                // 获取组件使用到的许可证列表
                List<SCALicenseVO> packageLicenseVOList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(scaSbomPackageEntity.getLicenses())) {
                    scaSbomPackageEntity.getLicenses().forEach(licenseName -> {
                        SCALicenseEntity licenseEntity = licenseMap.get(licenseName);
                        if (licenseEntity != null) {
                            SCALicenseVO licenseVO = new SCALicenseVO();
                            BeanUtils.copyProperties(licenseEntity, licenseVO);
                            packageLicenseVOList.add(licenseVO);
                        }
                    });
                }
                scaPackageVO.setLicenseList(packageLicenseVOList);

                // 风险等级字段值替换
                int severity = scaPackageVO.getSeverity() == ComConstants.PROMPT_IN_DB
                        ? ComConstants.PROMPT
                        : scaPackageVO.getSeverity();
                scaPackageVO.setSeverity(severity);

                // 更新计数
                severityCountMap.put(severity, severityCountMap.getOrDefault(severity, 0) + 1);
                int status = scaPackageVO.getStatus();
                statusCountMap.put(status, statusCountMap.getOrDefault(status, 0) + 1);
                return scaPackageVO;
            }).collect(Collectors.toList());

            Page<SCAPackageVO> pageResult = new Page<>(result.getCount(), result.getPage(),
                    result.getPageSize(), result.getTotalPages(), scaPackageVOList);
            scaPackageDefectQueryRspVO.setPackageList(pageResult);
        }

        // 统计结果
        scaPackageDefectQueryRspVO.setUnknownCount(severityCountMap.getOrDefault(ComConstants.UNKNOWN, 0));
        scaPackageDefectQueryRspVO.setLowCount(severityCountMap.getOrDefault(ComConstants.PROMPT, 0));
        scaPackageDefectQueryRspVO.setMediumCount(severityCountMap.getOrDefault(ComConstants.NORMAL, 0));
        scaPackageDefectQueryRspVO.setHighCount(severityCountMap.getOrDefault(ComConstants.SERIOUS, 0));
        scaPackageDefectQueryRspVO.setTotalCount(Integer.parseInt(String.valueOf(result.getCount())));

        int pathMaskCount = statusCountMap.getOrDefault(ComConstants.DefectStatus.PATH_MASK.value(), 0);
        int checkerMaskCount = statusCountMap.getOrDefault(ComConstants.DefectStatus.CHECKER_MASK.value(), 0);
        scaPackageDefectQueryRspVO.setExistCount(
                statusCountMap.getOrDefault(ComConstants.DefectStatus.NEW.value(), 0));
        scaPackageDefectQueryRspVO.setFixCount(
                statusCountMap.getOrDefault(ComConstants.DefectStatus.FIXED.value(), 0));
        scaPackageDefectQueryRspVO.setIgnoreCount(
                statusCountMap.getOrDefault(ComConstants.DefectStatus.IGNORE.value(), 0));
        scaPackageDefectQueryRspVO.setMaskCount(
                pathMaskCount + checkerMaskCount);

        return scaPackageDefectQueryRspVO;
    }

    /**
     * 处理组件详情查询
     *
     * @param requestVO
     * @return
     */
    @Override
    public SCAPackageDefectDetailQueryRspVO processQueryWarningDetailRequest(
            SCADefectDetailQueryReqVO requestVO
    ) {
        SCAPackageDefectDetailQueryRspVO responseVO = new SCAPackageDefectDetailQueryRspVO();
        SCAPackageDetailVO packageDetailVO = new SCAPackageDetailVO();

        Optional<SCASbomPackageEntity> entityOpt = scaSbomPackageRepository.findById(requestVO.getEntityId());
        if (!entityOpt.isPresent()) {
            log.info("package not found by entity id {}", requestVO.getEntityId());
            return responseVO;
        }
        SCASbomPackageEntity entity = entityOpt.get();
        BeanUtils.copyProperties(entity, packageDetailVO);

        // 判断是否直接依赖
        packageDetailVO.setDirect(entity.getDepth() <= 1);

        // 获取组件使用到的许可证列表
        List<SCALicenseEntity> licenseEntityList =
                scaLicenseRepository.findByTaskIdAndToolNameAndNameIn(
                        entity.getTaskId(),
                        entity.getToolName(),
                        entity.getLicenses()
                );
        if (CollectionUtils.isNotEmpty(licenseEntityList)) {
            List<SCALicenseVO> packageLicenseVOList =
                    licenseEntityList.stream()
                            .map(licenseEntity -> {
                                SCALicenseVO licenseVO = new SCALicenseVO();
                                BeanUtils.copyProperties(licenseEntity, licenseVO);
                                return licenseVO;
                            })
                            .collect(Collectors.toList());
            packageDetailVO.setLicenseList(packageLicenseVOList);
        }

        // 获取影响组件的漏洞列表
        List<SCAVulnerabilityEntity> vulEntityList =
                scaVulnerabilityDao.findByTaskIdAndToolNameAndPackageId(
                        entity.getTaskId(),
                        entity.getToolName(),
                        entity.getPackageId()
                );
        if (CollectionUtils.isNotEmpty(vulEntityList)) {
            List<SCAVulnerabilityVO> packageVulnerabilityVOList =
                    vulEntityList.stream()
                            .map(this::getVulVOFromEntity)
                            .collect(Collectors.toList());
            packageDetailVO.setVulnerabilityList(packageVulnerabilityVOList);
        }

        responseVO.setScaPackageDetailVO(packageDetailVO);

        return responseVO;
    }

    @Override
    public Object pageInit(SCAQueryWarningParams scaQueryWarningParams) {
        SCAQueryWarningPageInitRspVO response = new SCAQueryWarningPageInitRspVO();
        SCADefectQueryReqVO request = scaQueryWarningParams.getScaDefectQueryReqVO();
        // 跨任务查询时，不执行聚合统计
        if (Boolean.TRUE.equals(request.getMultiTaskQuery())) {
            return response;
        }
        String buildId = request.getBuildId();

        // 处理严重程度、告警类型条件：置null，因为需要统计所有风险等级和告警类型的数量
        request.setSeverity(null);
        request.setDefectType(null);

        // 处理快照查询条件：获取执行成功的快照的告警列表defectMongoIdSet
        Map<Long, List<String>> taskToolMap = scaQueryWarningParams.getTaskToolMap();
        Map.Entry<Long, List<String>> firstEntry = taskToolMap.entrySet().iterator().next();

        Long taskId = firstEntry.getKey();
        List<String> toolNameList = firstEntry.getValue();
        List<String> scaDimensionList = request.getScaDimensionList();

        Set<String> defectMongoIdSet = StringUtils.isNotBlank(buildId)
                ? SCAUtils.getPackageEntityIdsByBuildId(taskId, toolNameList, buildId, scaDimensionList)
                : Sets.newHashSet();
        scaQueryWarningParams.setScaDefectMongoIdSet(defectMongoIdSet);

        // 处理状态查询条件：默认添加查询待修复的告警
        Set<String> condStatusList = request.getStatus();
        if (CollectionUtils.isEmpty(condStatusList)) {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(ComConstants.DefectStatus.NEW.value()));
            request.setStatus(condStatusList);
        }

        // 将处理后的条件通过scaDefectListQueryParams参数类进行传递
        scaQueryWarningParams.setScaDefectQueryReqVO(request);

        // 获取统计类型
        String statisticType = request.getStatisticType();

        if (ComConstants.StatisticType.STATUS.name().equalsIgnoreCase(statisticType)) {
            // 1.根据快照、依赖方式、处理人、日期、组件名称过滤后，计算各状态告警数
            statisticByStatus(scaQueryWarningParams, response);
        } else if (ComConstants.StatisticType.SEVERITY.name().equalsIgnoreCase(statisticType)) {
            // 2.根据快照、依赖方式、处理人、日期、组件名称过滤后， 各严重级别告警数
            statisticBySeverity(scaQueryWarningParams, response);
        } else {
            log.error("StatisticType is invalid. {}", GsonUtils.toJson(request));
        }

        return response;
    }

    @Override
    public QueryWarningPageInitRspVO processQueryAuthorsRequest(
            SCAQueryWarningParams scaQueryWarningParams
    ) {
        SCADefectQueryReqVO request = scaQueryWarningParams.getScaDefectQueryReqVO();
        Map<Long, List<String>> taskToolMap = scaQueryWarningParams.getTaskToolMap();
        QueryWarningPageInitRspVO response = new QueryWarningPageInitRspVO();
        if (request == null) {
            log.info("internal sca package query authors request processed is null");
            return response;
        }
        if (MapUtils.isEmpty(taskToolMap)) {
            log.info("taskToolMap is empty, taskId: {}", request.getTaskIdList());
            return response;
        }

        // 1.处理Package快照查询条件，获取快照告警id列表
        String buildId = request.getBuildId();
        if (StringUtils.isNotBlank(buildId)) {
            Map.Entry<Long, List<String>> firstEntry = taskToolMap.entrySet().iterator().next();
            Set<String> defectMongoIdSet = SCAUtils.getPackageEntityIdsByBuildId(
                    firstEntry.getKey(),
                    firstEntry.getValue(),
                    buildId,
                    request.getScaDimensionList()
            );
            scaQueryWarningParams.setScaDefectMongoIdSet(defectMongoIdSet);
        }

        // 2.执行聚合查询获取作者列表
        List<String> authors = scaSbomPackageDao.findAuthorsByCondition(
                scaQueryWarningParams
        );

        // 3.构建返回结果
        response.setAuthorList(authors);
        return response;
    }


    /**
     * 统计添加筛选条件后的各状态告警数量
     *
     * @param scaQueryWarningParams
     * @param response
     */
    private void statisticByStatus(
            SCAQueryWarningParams scaQueryWarningParams,
            SCAQueryWarningPageInitRspVO response
    ) {
        SCADefectQueryReqVO request = scaQueryWarningParams.getScaDefectQueryReqVO();
        Set<String> originalStatus = request.getStatus();
        List<SCADefectGroupStatisticVO> groups =
                scaSbomPackageDao.statisticByStatus(scaQueryWarningParams);

        groups.forEach(it -> {
            int status = it.getStatus();

            if (ComConstants.DefectStatus.NEW.value() == status) {
                response.setExistCount(response.getExistCount() + it.getDefectCount());
            } else if ((ComConstants.DefectStatus.FIXED.value() & status) > 0) {
                response.setFixCount(response.getFixCount() + it.getDefectCount());
            } else if ((ComConstants.DefectStatus.IGNORE.value() & status) > 0) {
                response.setIgnoreCount(response.getIgnoreCount() + it.getDefectCount());
            } else if ((ComConstants.DefectStatus.PATH_MASK.value() & status) > 0
                    || (ComConstants.DefectStatus.CHECKER_MASK.value() & status) > 0) {
                response.setMaskCount(response.getMaskCount() + it.getDefectCount());
            }
        });

        // 若是快照查，则修正统计；快照查已移除"已修复"状态
        if (StringUtils.isNotEmpty(request.getBuildId())) {
            // 已忽略、已屏蔽在多分支下是共享的；而待修复与已修复是互斥的
            response.setExistCount(response.getExistCount() + response.getFixCount());
            response.setFixCount(0);
        }
        request.setStatus(originalStatus);
    }

    /**
     * 统计添加筛选条件后的各风险等级告警数量
     *
     * @param scaQueryWarningParams
     * @param response
     */
    private void statisticBySeverity(
            SCAQueryWarningParams scaQueryWarningParams,
            SCAQueryWarningPageInitRspVO response
    ) {
        List<SCADefectGroupStatisticVO> groups =
                scaSbomPackageDao.statisticBySeverity(scaQueryWarningParams);

        groups.forEach(it -> {
            if (ComConstants.SERIOUS == it.getSeverity()) {
                response.setSeriousCount(response.getSeriousCount() + it.getDefectCount());
            } else if (ComConstants.NORMAL == it.getSeverity()) {
                response.setNormalCount(response.getNormalCount() + it.getDefectCount());
            } else if (ComConstants.PROMPT_IN_DB == it.getSeverity() || ComConstants.PROMPT == it.getSeverity()) {
                response.setPromptCount(response.getPromptCount() + it.getDefectCount());
            } else if (ComConstants.UNKNOWN == it.getSeverity()) {
                response.setUnknownCount(response.getUnknownCount() + it.getDefectCount());
            }
        });
    }
}
