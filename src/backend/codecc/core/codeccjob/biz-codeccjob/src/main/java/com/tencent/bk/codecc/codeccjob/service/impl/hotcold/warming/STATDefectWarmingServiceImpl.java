package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.warming;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.StatDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataWarmingService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.ArchivingFileModel;
import com.tencent.bk.codecc.defect.model.defect.StatDefectEntity;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class STATDefectWarmingServiceImpl
        extends AbstractDefectWarmingTemplate<StatDefectEntity>
        implements ColdDataWarmingService {

    @Autowired
    private StatDefectRepository statDefectRepository;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.STAT;
    }

    @Override
    protected List<StatDefectEntity> getDefectList(String json) {
        return JSONObject.parseObject(json, new TypeReference<ArchivingFileModel<StatDefectEntity>>() {
        }).getDefectList();
    }

    @Override
    protected String getCollectionName() {
        return defectMongoTemplate.getCollectionName(StatDefectEntity.class);
    }

    @Override
    protected void preHandle(long taskId) {
        StatDefectEntity statDefectEntity = statDefectRepository.findFirstByTaskId(taskId);
        if (statDefectEntity != null) {
            long delCount = statDefectRepository.deleteByTaskId(taskId);
            log.info("defect warming preHandle, task id: {}, del count: {}, stat", taskId, delCount);
        }
    }
}
