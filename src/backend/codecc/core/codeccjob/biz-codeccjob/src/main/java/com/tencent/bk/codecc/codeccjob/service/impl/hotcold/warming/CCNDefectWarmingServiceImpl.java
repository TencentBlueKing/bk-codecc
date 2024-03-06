package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.warming;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataWarmingService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.ArchivingFileModel;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CCNDefectWarmingServiceImpl
        extends AbstractDefectWarmingTemplate<CCNDefectEntity>
        implements ColdDataWarmingService {

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.CCN;
    }

    @Override
    protected List<CCNDefectEntity> getDefectList(String json) {
        return JSONObject.parseObject(json, new TypeReference<ArchivingFileModel<CCNDefectEntity>>() {
        }).getDefectList();
    }

    @Override
    protected String getCollectionName() {
        return defectMongoTemplate.getCollectionName(CCNDefectEntity.class);
    }

    @Override
    protected void preHandle(long taskId) {
        CCNDefectEntity ccnDefectEntity = ccnDefectRepository.findFirstByTaskId(taskId);
        if (ccnDefectEntity != null) {
            long delCount = ccnDefectRepository.deleteByTaskId(taskId);
            log.info("defect warming preHandle, task id: {}, del count: {}, ccn", taskId, delCount);
        }
    }
}
