package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.SCALicenseRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoredNegativeDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.SCALicenseDao;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.service.ISCABatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.utils.SCAUtils;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("SCALicenseBatchDefectProcessBizService")
public class SCALicenseBatchDefectProcessBizServiceImpl implements ISCABatchDefectProcessBizService {
    @Autowired
    private SCALicenseRepository scaLicenseRepository;
    @Autowired
    private SCALicenseDao scaLicenseDao;
    @Autowired
    private IgnoredNegativeDefectDao ignoredNegativeDefectDao;
    @Autowired
    private Client client;

    @Override
    public List getDefectsByQueryCondWithPage(
            SCAQueryWarningParams scaQueryWarningParams,
            String entityId,
            Integer pageSize
    ) {
        return Collections.emptyList();
    }

    @Override
    public List getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        Set<String> entityIdSet = batchDefectProcessReqVO.getDefectKeySet();
        List<SCALicenseEntity> defectEntityList =
                scaLicenseRepository.findByTaskIdInAndEntityIdIn(
                        Collections.singletonList(batchDefectProcessReqVO.getTaskId()),
                        entityIdSet
                );

        if (CollectionUtils.isEmpty(defectEntityList)) {
            return Lists.newArrayList();
        }

        Iterator<SCALicenseEntity> it = defectEntityList.iterator();
        while (it.hasNext()) {
            SCALicenseEntity defectEntity = it.next();
            // 状态过滤
            int status = defectEntity.getStatus();
            Set<String> statusCondSet =
                    SCAUtils.getStatusConditionByBizType(batchDefectProcessReqVO.getBizType());;
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
     * @Deprecated 许可证暂无需标记忽略
     */
    @Deprecated
    @Override
    public void batchIgnoreDefect(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectList.forEach(defectEntity -> ((SCALicenseEntity) defectEntity).setStatus(status));
        scaLicenseDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defectList,
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

            ignoredNegativeDefectDao.batchInsert(defectList, batchDefectProcessReqVO, taskBaseResult.getData());
        }
    }

    @Override
    public void batchUpdateIgnoreType(
            long taskId, List<LintDefectV2Entity> defectList,
            int ignoreReasonType,
            String ignoreReason
    ) {
    }

    /**
     * 标记处理、取消标记
     * @param defectList
     * @param batchDefectProcessReqVO
     * @Deprecated 许可证暂无需标记处理
     */
    @Deprecated
    @Override
    public void batchMarkDefect(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        scaLicenseDao.batchMarkDefect(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getMarkFlag());
    }

    /**
     * 取消忽略
     * @param defectList
     * @param batchDefectProcessReqVO
     * @Deprecated 许可证暂无需取消忽略
     */
    @Deprecated
    @Override
    public void batchRevertIgnore(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        List<SCALicenseEntity> defects = ((List<SCALicenseEntity>) defectList).stream()
                .filter(it -> (it.getStatus() & ComConstants.DefectStatus.IGNORE.value()) > 0)
                .map(it -> {
                    it.setStatus(it.getStatus() - ComConstants.DefectStatus.IGNORE.value());
                    return it;
                }).collect(Collectors.toList());
        scaLicenseDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defects, 0,
                null, null);
    }

    /**
     * 分配处理人
     * @param taskId
     * @param defectList
     * @param newAuthor
     * @Deprecated 许可证暂无需分配处理人
     */
    @Override
    public void batchUpdateDefectAuthor(long taskId, List defectList, Set<String> newAuthor) {
    }
}
