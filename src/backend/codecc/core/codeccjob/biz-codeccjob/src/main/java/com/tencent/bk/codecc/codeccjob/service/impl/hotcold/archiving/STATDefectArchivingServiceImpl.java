package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.archiving;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.StatDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataArchivingService;
import com.tencent.bk.codecc.defect.model.defect.StatDefectEntity;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class STATDefectArchivingServiceImpl
        extends AbstractDefectArchivingTemplate<StatDefectEntity>
        implements ColdDataArchivingService {

    @Autowired
    private StatDefectRepository statDefectRepository;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.STAT;
    }

    @Override
    protected List<StatDefectEntity> getDefectListCore(long taskId, Pageable pageable) {
        return statDefectRepository.findByTaskId(taskId, pageable);
    }
}
