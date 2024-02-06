package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.purging;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.BuildDefectSummaryRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.BuildDefectV2Repository;
import com.tencent.devops.common.constant.ComConstants.ColdDataPurgingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 快照清理
 */
@Service
public class SnapshotPurgingServiceImpl extends AbstractDefectPurgingTemplate {

    @Autowired
    private BuildDefectSummaryRepository buildDefectSummaryRepository;
    @Autowired
    private BuildDefectV2Repository buildDefectV2Repository;

    @Override
    protected long purgeCore(long taskId) {
        return buildDefectSummaryRepository.deleteByTaskId(taskId) + buildDefectV2Repository.deleteByTaskId(taskId);
    }

    @Override
    public ColdDataPurgingType coldDataPurgingType() {
        return ColdDataPurgingType.SNAPSHOT;
    }
}
