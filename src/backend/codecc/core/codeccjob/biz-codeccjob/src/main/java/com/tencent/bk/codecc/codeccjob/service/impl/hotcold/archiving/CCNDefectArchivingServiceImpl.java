package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.archiving;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataArchivingService;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CCNDefectArchivingServiceImpl
        extends AbstractDefectArchivingTemplate<CCNDefectEntity>
        implements ColdDataArchivingService {

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.CCN;
    }

    @Override
    protected List<CCNDefectEntity> getDefectListCore(long taskId, Pageable pageable) {
        return ccnDefectRepository.findByTaskId(taskId, pageable);
    }
}
