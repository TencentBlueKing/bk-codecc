package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.archiving;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CLOCDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataArchivingService;
import com.tencent.bk.codecc.defect.model.defect.CLOCDefectEntity;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CLOCDefectArchivingServiceImpl
        extends AbstractDefectArchivingTemplate<CLOCDefectEntity>
        implements ColdDataArchivingService {

    @Autowired
    private CLOCDefectRepository clocDefectRepository;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.CLOC;
    }

    @Override
    protected List<CLOCDefectEntity> getDefectListCore(long taskId, Pageable pageable) {
        return clocDefectRepository.findByTaskId(taskId, pageable);
    }
}
