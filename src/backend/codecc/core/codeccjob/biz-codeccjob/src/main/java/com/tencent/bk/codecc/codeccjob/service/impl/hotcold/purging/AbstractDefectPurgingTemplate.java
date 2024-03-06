package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.purging;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ColdDataPurgingLogRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataPurgingService;
import com.tencent.bk.codecc.defect.model.ColdDataPurgingLogEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 数据清理模板方法
 */
@Slf4j
public abstract class AbstractDefectPurgingTemplate implements ColdDataPurgingService {

    @Autowired
    private ColdDataPurgingLogRepository coldDataPurgingLogRepository;

    @Override
    public boolean purge(long taskId) {
        ColdDataPurgingLogEntity coldDataPurgingLogEntity =
                coldDataPurgingLogRepository.findFirstByTaskIdAndType(taskId, coldDataPurgingType().name());
        if (coldDataPurgingLogEntity != null && coldDataPurgingLogEntity.isSuccess()) {
            return true;
        }

        long beginTime = System.currentTimeMillis();
        boolean finalResult = false;
        long delCount = 0;
        String remark = "";

        try {
            delCount = purgeCore(taskId);
            remark = getRemark(taskId);
            finalResult = true;
        } catch (Throwable t) {
            log.error("purge cold data fail, task id: {}, type: {}", taskId, coldDataPurgingType().name(), t);
        } finally {
            long cost = System.currentTimeMillis() - beginTime;
            if (coldDataPurgingLogEntity == null) {
                coldDataPurgingLogEntity = new ColdDataPurgingLogEntity(
                        taskId,
                        coldDataPurgingType().name(),
                        finalResult,
                        delCount,
                        remark,
                        cost
                );
                coldDataPurgingLogEntity.applyAuditInfoOnCreate();
            } else {
                coldDataPurgingLogEntity.setDataCount(delCount);
                coldDataPurgingLogEntity.setSuccess(finalResult);
                coldDataPurgingLogEntity.setCost(cost);
                coldDataPurgingLogEntity.applyAuditInfoOnUpdate();
            }
            coldDataPurgingLogRepository.save(coldDataPurgingLogEntity);
        }

        return finalResult;
    }

    @Override
    public int order() {
        return 0;
    }

    protected abstract long purgeCore(long taskId);

    /**
     * 额外说明，供上层重写
     *
     * @return
     */
    protected String getRemark(long taskId) {
        return "";
    }
}
