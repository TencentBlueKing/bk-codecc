package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.MetricsRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.DefectClusterStatisticDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.MetricsDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.SecurityClusterStatisticDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.StandardClusterStatisticDao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.DefectClusterStatisticEntity;
import com.tencent.bk.codecc.defect.model.MetricsEntity;
import com.tencent.bk.codecc.defect.model.SecurityClusterStatisticEntity;
import com.tencent.bk.codecc.defect.model.StandardClusterStatisticEntity;
import com.tencent.bk.codecc.defect.service.BuildService;
import com.tencent.bk.codecc.defect.service.MetricsService;
import com.tencent.bk.codecc.defect.vo.MetricsVO;
import com.tencent.bk.codecc.task.api.ServiceToolConfigRestResource;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetricsServiceImpl implements MetricsService {

    @Autowired
    MetricsDao metricsDao;

    @Autowired
    private MetricsRepository metricsRepository;

    @Autowired
    private Client client;

    @Autowired
    private StandardClusterStatisticDao standardClusterStatisticDao;

    @Autowired
    private SecurityClusterStatisticDao securityClusterStatisticDao;

    @Autowired
    private DefectClusterStatisticDao defectClusterStatisticDao;

    @Autowired
    private BuildService buildService;

    @Override
    public Boolean updateMetricsByTaskIdAndBuildId(MetricsEntity entity) {
        return metricsDao.upsert(entity);
    }

    @Override
    public MetricsVO getMetrics(Long taskId, String buildId) {
        MetricsEntity metricsEntity = metricsRepository.findFirstByTaskIdAndBuildIdOrderByEntityIdDesc(taskId, buildId);

        log.info("get metrics for build: {} {} {}", taskId, buildId, metricsEntity);

        MetricsVO metricsVO = new MetricsVO();
        if (metricsEntity == null) {
            return metricsVO;
        }

        BeanUtils.copyProperties(metricsEntity, metricsVO);

        return metricsVO;
    }

    @Override
    public MetricsVO getLatestMetrics(Long taskId) {
        MetricsVO metricsVO = new MetricsVO();
        if (taskId == null) {
            return metricsVO;
        }

        List<Long> taskIds = new ArrayList<>();
        taskIds.add(taskId);

        List<MetricsEntity> metricsEntityList = getLatestMetricsByTaskIdIn(taskIds);
        if (metricsEntityList.size() != 1) {
            log.info("metricsEntityList's size is {}, task id is {}", metricsEntityList.size(), taskId);
            return metricsVO;
        }

        MetricsEntity metricsEntity = metricsEntityList.get(0);
        if (metricsEntity == null) {
            return metricsVO;
        }

        BeanUtils.copyProperties(metricsEntity, metricsVO);

        return metricsVO;
    }

    @Override
    public List<MetricsVO> getMetrics(List<Long> taskIds) {
        List<MetricsEntity> metricsEntityList = getLatestMetricsByTaskIdIn(taskIds);
        Map<Long, StandardClusterStatisticEntity> standardClusterStatisticEntityMap =
                standardClusterStatisticDao.findListByTaskIdAndBuildId(metricsEntityList)
                        .stream()
                        .collect(Collectors.toMap(StandardClusterStatisticEntity::getTaskId, a -> a, (t1, t2) -> t1));
        Map<Long, SecurityClusterStatisticEntity> securityClusterStatisticEntityMap =
                securityClusterStatisticDao.findListByTaskIdAndBuildId(metricsEntityList)
                        .stream()
                        .collect(Collectors.toMap(SecurityClusterStatisticEntity::getTaskId, a -> a, (t1, t2) -> t1));
        Map<Long, DefectClusterStatisticEntity> defectClusterStatisticEntityMap =
                defectClusterStatisticDao.findListByTaskIdAndBuildId(metricsEntityList)
                        .stream()
                        .collect(Collectors.toMap(DefectClusterStatisticEntity::getTaskId, a -> a, (t1, t2) -> t1));

        List<MetricsVO> metricsVOList = new ArrayList<>();
        metricsEntityList.forEach(metricsEntity -> {
            MetricsVO metricsVO = new MetricsVO();
            metricsVO.setTaskId(metricsEntity.getTaskId());
            metricsVO.setOpenScan(metricsEntity.isOpenScan());
            metricsVO.setRdIndicatorsScore(metricsEntity.getRdIndicatorsScore());
            if (defectClusterStatisticEntityMap.get(metricsEntity.getTaskId()) != null) {
                metricsVO.setTotalDefectCount(
                        defectClusterStatisticEntityMap.get(metricsEntity.getTaskId()).getTotalCount().longValue());
            }
            if (standardClusterStatisticEntityMap.get(metricsEntity.getTaskId()) != null) {
                metricsVO.setTotalStyleDefectCount(
                        standardClusterStatisticEntityMap.get(metricsEntity.getTaskId()).getTotalCount().longValue());
            }
            if (securityClusterStatisticEntityMap.get(metricsEntity.getTaskId()) != null) {
                metricsVO.setTotalSecurityDefectCount(
                        securityClusterStatisticEntityMap.get(metricsEntity.getTaskId()).getTotalCount().longValue());
            }
            metricsVOList.add(metricsVO);
        });
        return metricsVOList;
    }

    /**
     * 查询taskIds这个list中所有任务的最新指标数据
     * @param taskIds
     * @return 各个任务的最新指标数据
     */
    private List<MetricsEntity> getLatestMetricsByTaskIdIn(List<Long> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return Collections.emptyList();
        }

        // 获取所有任务中所有工具的配置信息
        Result<List<ToolConfigInfoVO>> toolConfigInfoResult = client.get(ServiceToolConfigRestResource.class)
                .getByTaskIds(taskIds);
        if (toolConfigInfoResult == null || toolConfigInfoResult.isNotOk() || toolConfigInfoResult.getData() == null
                || CollectionUtils.isEmpty(toolConfigInfoResult.getData())) {
            return Collections.emptyList();
        }
        List<ToolConfigInfoVO> toolConfigInfoVOList = toolConfigInfoResult.getData();

        // 获取各个任务的build id, 用map<task_id, Set<build_id>>的形式保存起来(map的键是task id,
        // 值是该任务的所有工具的build id, 用set保存)
        Map<Long, Set<String>> taskIdToBuildIds = new HashMap<>();
        for (ToolConfigInfoVO toolConfigInfo : toolConfigInfoVOList) {
            // 忽略下架工具
            if (toolConfigInfo.getFollowStatus() == ComConstants.FOLLOW_STATUS.WITHDRAW.value()) {
                continue;
            }

            Long taskId = toolConfigInfo.getTaskId();
            if (!taskIdToBuildIds.containsKey(taskId)) {
                taskIdToBuildIds.put(taskId, new HashSet<>());
            }
            taskIdToBuildIds.get(taskId).add(toolConfigInfo.getCurrentBuildId());
        }

        // 对于每个任务, 只保留一个最新的 build id, 保存在 taskIdToLatestBuildId 中.
        // 具体做法就是去 t_build 这张表中查 build_time, 保留 build_time 最晚的一个 build id.
        Map<Long, String> taskIdToLatestBuildId = new HashMap<>();
        // 先把所有的 build id 存在一起, 一次性从 t_build 表中查出所有的 BuildEntity
        List<String> buildIdCheckList = new ArrayList<>();
        // 记录查表的 build id 所对应的 task id
        Map<String, Long> buildIdToTaskId = new HashMap<>();
        taskIdToBuildIds.forEach((taskId, buildIds) -> {
            if (buildIds.size() == 1) {
                // 很多时候都是只有一个 build id, 优化一下
                taskIdToLatestBuildId.put(taskId, buildIds.iterator().next());
            } else {
                buildIdCheckList.addAll(buildIds);
                buildIds.forEach(it -> buildIdToTaskId.put(it, taskId));
            }
        });

        List<BuildEntity> buildEntities = buildService.getBuildEntityInBuildIds(buildIdCheckList);
        Map<Long, Long> taskIdToLatestTime = new HashMap<>();
        for (BuildEntity buildEntity: buildEntities) {
            Long taskId = buildIdToTaskId.get(buildEntity.getBuildId());
            Long latestTime = taskIdToLatestTime.get(taskId);
            if (latestTime != null && latestTime > buildEntity.getBuildTime()) {
                continue;
            }
            taskIdToLatestTime.put(taskId, buildEntity.getBuildTime());
            taskIdToLatestBuildId.put(taskId, buildEntity.getBuildId());
        }

        List<MetricsEntity> metricsEntities = metricsDao.findByTaskAndBuildIdMap(taskIdToLatestBuildId);
        Collection<MetricsEntity> result = metricsEntities.stream().collect(
                Collectors.toMap(
                        MetricsEntity::getTaskId,
                        it -> it,
                        (o1, o2) -> o1.getEntityId().compareTo(o2.getEntityId()) < 0 ? o2 : o1
                )
        ).values();

        return new ArrayList<>(result);
    }

}
