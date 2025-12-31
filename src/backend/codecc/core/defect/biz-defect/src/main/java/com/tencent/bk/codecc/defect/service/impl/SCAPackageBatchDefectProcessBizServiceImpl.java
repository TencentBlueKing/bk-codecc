package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.BuildSCASbomPackageRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.SCASbomPackageRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoredNegativeDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.SCASbomPackageDao;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomPackageEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity;
import com.tencent.bk.codecc.defect.service.IIgnoredNegativeDefectService;
import com.tencent.bk.codecc.defect.service.ISCABatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.utils.SCAUtils;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("SCAPackageBatchDefectProcessBizService")
public class SCAPackageBatchDefectProcessBizServiceImpl implements ISCABatchDefectProcessBizService {
    @Autowired
    private SCASbomPackageRepository scasbomPackageRepository;
    @Autowired
    private SCASbomPackageDao scaSbomPackageDao;
    @Autowired
    BuildSCASbomPackageRepository buildSCASbomPackageRepository;
    @Autowired
    TaskLogService taskLogService;
    @Autowired
    private IIgnoredNegativeDefectService iIgnoredNegativeDefectService;
    @Autowired
    private Client client;

    /**
     * 根据条件分页查询SCA组件告警列表
     * @param scaQueryWarningParams
     * @param lastEntityId
     * @param pageSize
     * @return
     */
    @Override
    public List getDefectsByQueryCondWithPage(
            SCAQueryWarningParams scaQueryWarningParams,
            String lastEntityId,
            Integer pageSize
    ) {
        SCADefectQueryReqVO request = scaQueryWarningParams.getScaDefectQueryReqVO();
        if (request == null) {
            log.info("internal sca package query request after processed is null");
            return Lists.newArrayList();
        }
        // 处理快照请求参数buildId: 筛选出执行成功的快照的告警id列表
        Map<Long, List<String>> taskToolMap = scaQueryWarningParams.getTaskToolMap();
        if (MapUtils.isEmpty(taskToolMap)) {
            log.info("taskToolMap is empty, taskId: {}", request.getTaskIdList());
            return Lists.newArrayList();
        }
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

        // 查询告警
        if (pageSize == null) {
            return scaSbomPackageDao.findSCASbomPackageByCondition(scaQueryWarningParams);
        } else {
            return scaSbomPackageDao.findDefectByConditionWithEntityIdPage(
                    scaQueryWarningParams,
                    lastEntityId,
                    pageSize
            );
        }
    }

    /**
     * 获取业务处理类型需要处理的告警列表
     * @param batchDefectProcessReqVO
     * @return
     */
    @Override
    public List getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        Set<String> entityIdSet = batchDefectProcessReqVO.getDefectKeySet();
        List<SCASbomPackageEntity> defectEntityList =
                scasbomPackageRepository.findByTaskIdInAndEntityIdIn(
                        Collections.singletonList(batchDefectProcessReqVO.getTaskId()),
                        entityIdSet
                );

        if (CollectionUtils.isEmpty(defectEntityList)) {
            log.info("get task {} defect list empty, defect id set: {}",
                    batchDefectProcessReqVO.getTaskId(), entityIdSet);
            return Lists.newArrayList();
        }

        Iterator<SCASbomPackageEntity> it = defectEntityList.iterator();
        while (it.hasNext()) {
            SCASbomPackageEntity defectEntity = it.next();
            // 状态过滤
            int status = defectEntity.getStatus();
            Set<String> statusCondSet =
                    SCAUtils.getStatusConditionByBizType(batchDefectProcessReqVO.getBizType());
            boolean match = false;
            for (String statusCondStr : statusCondSet) {
                int statusCond = Integer.parseInt(statusCondStr);
                boolean notMatchNewStatus = statusCond == ComConstants.DefectStatus.NEW.value()
                        && status != ComConstants.DefectStatus.NEW.value();
                boolean notMatchIgnoreStatus = statusCond > ComConstants.DefectStatus.NEW.value()
                        && (status & statusCond) == 0;
                if (notMatchNewStatus || notMatchIgnoreStatus) {
                    continue;
                }
                match = true;
                break;
            }
            if (!match) {
                it.remove();
            }
        }

        return defectEntityList;
    }

    /**
     * 标记忽略
     * @param defectList
     * @param batchDefectProcessReqVO
     */
    @Override
    public void batchIgnoreDefect(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectList.forEach(defectEntity -> ((SCASbomPackageEntity) defectEntity).setStatus(status));
        scaSbomPackageDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getIgnoreReasonType(),
                batchDefectProcessReqVO.getIgnoreReason(),
                batchDefectProcessReqVO.getIgnoreAuthor());
    }

    @Override
    public void batchIgnoreDefectByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        batchIgnoreDefect(defectList, batchDefectProcessReqVO);
        if (batchDefectProcessReqVO.getIgnoreReasonType() == ComConstants.IgnoreReasonType.ERROR_DETECT.value()) {
            Result<TaskDetailVO> taskBaseResult = client.get(ServiceTaskRestResource.class)
                    .getTaskInfoById(batchDefectProcessReqVO.getTaskId());
            if (null == taskBaseResult || taskBaseResult.isNotOk() || null == taskBaseResult.getData()) {
                log.error("get task info fail!, task id: {}", batchDefectProcessReqVO.getTaskId());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            iIgnoredNegativeDefectService.batchInsertIgnoredDefects(defectList,
                    batchDefectProcessReqVO, taskBaseResult.getData());
        }
    }

    @Override
    public void batchUpdateIgnoreType(long taskId, List defectList, int ignoreReasonType, String ignoreReason) {
        List<SCASbomPackageEntity> entities = (List<SCASbomPackageEntity>) defectList.stream()
                .filter(Objects::nonNull)
                .filter(SCASbomPackageEntity.class::isInstance)
                .map(SCASbomPackageEntity.class::cast)
                .collect(Collectors.toList());
        scaSbomPackageDao.batchUpdateIgnoreType(taskId, entities, ignoreReasonType, ignoreReason);
    }

    /**
     * 标记处理、取消标记
     * @param defectList
     * @param batchDefectProcessReqVO
     */
    @Override
    public void batchMarkDefect(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        scaSbomPackageDao.batchMarkDefect(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getMarkFlag());
    }

    /**
     * 取消忽略
     * @param defectList
     * @param batchDefectProcessReqVO
     */
    @Override
    public void batchRevertIgnore(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        List<SCASbomPackageEntity> defects = ((List<SCASbomPackageEntity>) defectList).stream()
                .filter(it -> (it.getStatus() & ComConstants.DefectStatus.IGNORE.value()) > 0)
                .map(it -> {
                    it.setStatus(it.getStatus() - ComConstants.DefectStatus.IGNORE.value());
                    return it;
                }).collect(Collectors.toList());
        scaSbomPackageDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defects, 0,
                null, null);
    }

    /**
     * 分配处理人
     * @param taskId
     * @param defectList
     * @param newAuthor
     */
    @Override
    public void batchUpdateDefectAuthor(long taskId, List defectList, Set<String> newAuthor) {
        scaSbomPackageDao.batchUpdateDefectAuthor(taskId, defectList, newAuthor);
    }
}
