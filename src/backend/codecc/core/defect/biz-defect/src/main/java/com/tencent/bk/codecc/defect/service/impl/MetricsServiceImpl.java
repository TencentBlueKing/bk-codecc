package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.MetricsRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectClusterStatisticDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.MetricsDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.SecurityClusterStatisticDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.StandardClusterStatisticDao;
import com.tencent.bk.codecc.defect.model.DefectClusterStatisticEntity;
import com.tencent.bk.codecc.defect.model.MetricsEntity;
import com.tencent.bk.codecc.defect.model.SecurityClusterStatisticEntity;
import com.tencent.bk.codecc.defect.model.StandardClusterStatisticEntity;
import com.tencent.bk.codecc.defect.service.MetricsService;
import com.tencent.bk.codecc.defect.vo.MetricsVO;
import com.tencent.bk.codecc.task.api.ServiceToolConfigRestResource;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.Tool;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import com.tencent.devops.common.util.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Override
    public MetricsVO getMetrics(Long taskId, String buildId) {
        MetricsEntity metricsEntity = metricsRepository.findFirstByTaskIdAndBuildId(taskId, buildId);

        log.info("get metrics for build: {} {} {}", taskId, buildId, metricsEntity);

        MetricsVO metricsVO = new MetricsVO();
        if (metricsEntity == null) {
            return metricsVO;
        }

        BeanUtils.copyProperties(metricsEntity, metricsVO);

        return metricsVO;
    }

    @Override
    public List<MetricsVO> getMetrics(List<Long> taskIds) {
        List<MetricsEntity> metricsEntityList = getLastByTaskIdIn(taskIds);
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
     * 查询任务最新的指标数据
     * @param taskIds
     * @return
     */
    private List<MetricsEntity> getLastByTaskIdIn(List<Long> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return Collections.emptyList();
        }
        Result<List<ToolConfigInfoVO>> toolConfigResult =
                client.get(ServiceToolConfigRestResource.class).getByTaskIdsAndToolName(taskIds, Tool.SCC.name());
        if (toolConfigResult.isNotOk() || CollectionUtils.isEmpty(toolConfigResult.getData())) {
            return Collections.emptyList();
        }
        List<ToolConfigInfoVO> toolConfigInfoVOList = toolConfigResult.getData();
        Map<Long, String> taskIdToBuildIds = toolConfigInfoVOList.stream()
                .filter(toolConfigInfoVO -> StringUtils.isNotBlank(toolConfigInfoVO.getCurrentBuildId()))
                .collect(Collectors.toMap(ToolConfigInfoVO::getTaskId, ToolConfigInfoVO::getCurrentBuildId));
        return metricsDao.findByTaskAndBuildIdMap(taskIdToBuildIds);
    }

}
