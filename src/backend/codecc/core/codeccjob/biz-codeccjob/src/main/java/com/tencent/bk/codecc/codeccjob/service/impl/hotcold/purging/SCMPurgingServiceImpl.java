package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.purging;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ScmFileInfoCacheRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ScmFileInfoSnapshotRepository;
import com.tencent.devops.common.constant.ComConstants.ColdDataPurgingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * scm相关数据清理
 */
@Service
public class SCMPurgingServiceImpl extends AbstractDefectPurgingTemplate {

    @Autowired
    private ScmFileInfoSnapshotRepository scmFileInfoSnapshotRepository;
    @Autowired
    private ScmFileInfoCacheRepository scmFileInfoCacheRepository;

    @Override
    protected long purgeCore(long taskId) {
        return scmFileInfoCacheRepository.deleteByTaskId(taskId) + scmFileInfoSnapshotRepository.deleteByTaskId(taskId);
    }

    @Override
    public ColdDataPurgingType coldDataPurgingType() {
        return ColdDataPurgingType.SCM;
    }
}
