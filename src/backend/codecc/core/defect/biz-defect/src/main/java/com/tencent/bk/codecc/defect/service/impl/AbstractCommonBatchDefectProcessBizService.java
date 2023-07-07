package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractBatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
public abstract class AbstractCommonBatchDefectProcessBizService extends AbstractBatchDefectProcessBizService {

    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private DefectDao defectDao;
    @Autowired
    @Qualifier("CommonQueryWarningBizService")
    private CommonQueryWarningBizServiceImpl commonQueryWarningBizService;

    /**
     * 根据前端传入的条件查询告警键值
     *
     * @param taskId
     * @param defectQueryReqVO
     * @return
     */
    @Deprecated
    @Override
    protected List getDefectsByQueryCond(long taskId, DefectQueryReqVO defectQueryReqVO) {
        throw new UnsupportedOperationException();
    }


    @Override
    protected List<Long> getTaskIdsByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        Set<String> entityIdSet = batchDefectProcessReqVO.getDefectKeySet();
        return defectDao.findTaskIdsByEntityIds(entityIdSet);
    }

    @Deprecated
    @Override
    protected List getDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO defectQueryReqVO,
            String startFilePath, Long skip, Integer pageSize) {
        // 缺陷类目前走Lint类
        return null;
    }

    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        // 缺陷类目前走Lint类
    }

    @Override
    protected void processCustomizeOpsAfterEachPageDone(List defectList,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        // 缺陷类目前走Lint类
    }

    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        // 缺陷类目前走Lint类
    }

    /**
     * 根据前端传入的告警key，查询有效的告警
     * 过滤规则是：忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     *
     * @param batchDefectProcessReqVO
     */
    @Deprecated
    @Override
    protected List<CommonDefectEntity> getEffectiveDefectByDefectKeySet(
            BatchDefectProcessReqVO batchDefectProcessReqVO
    ) {
        Long taskId = batchDefectProcessReqVO.getTaskId();
        Set<String> entityIdSet = batchDefectProcessReqVO.getDefectKeySet();
        List<CommonDefectEntity> defectEntityList = defectRepository.findNoneInstancesFieldByTaskIdAndEntityIdIn(taskId,
                entityIdSet);
        if (CollectionUtils.isEmpty(defectEntityList)) {
            return Lists.newArrayList();
        }

        Iterator<CommonDefectEntity> it = defectEntityList.iterator();
        while (it.hasNext()) {
            CommonDefectEntity commonDefectEntity = it.next();
            int status = commonDefectEntity.getStatus();
            Set<String> statusCondSet = getStatusCondition(null);
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
}
