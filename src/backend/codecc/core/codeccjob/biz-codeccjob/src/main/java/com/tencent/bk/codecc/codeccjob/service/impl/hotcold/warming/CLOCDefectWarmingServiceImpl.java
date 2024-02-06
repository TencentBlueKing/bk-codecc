package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.warming;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CLOCDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataWarmingService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.ArchivingFileModel;
import com.tencent.bk.codecc.defect.model.defect.CLOCDefectEntity;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CLOCDefectWarmingServiceImpl
        extends AbstractDefectWarmingTemplate<CLOCDefectEntity>
        implements ColdDataWarmingService {

    @Autowired
    private CLOCDefectRepository clocDefectRepository;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.CLOC;
    }

    @Override
    protected List<CLOCDefectEntity> getDefectList(String json) {
        return JSONObject.parseObject(json, new TypeReference<ArchivingFileModel<CLOCDefectEntity>>() {
        }).getDefectList();
    }

    @Override
    protected String getCollectionName() {
        return defectMongoTemplate.getCollectionName(CLOCDefectEntity.class);
    }

    @Override
    protected void preHandle(long taskId) {
        CLOCDefectEntity clocDefectEntity = clocDefectRepository.findFirstByTaskId(taskId);
        if (clocDefectEntity != null) {
            long delCount = clocDefectRepository.deleteByTaskId(taskId);
            log.info("defect warming preHandle, task id: {}, del count: {}, cloc", taskId, delCount);
        }
    }
}
