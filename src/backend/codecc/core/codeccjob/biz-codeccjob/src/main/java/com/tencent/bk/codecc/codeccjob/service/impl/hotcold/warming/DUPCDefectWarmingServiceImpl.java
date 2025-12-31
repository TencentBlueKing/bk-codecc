package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.warming;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataWarmingService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.ArchivingFileModel;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DUPCDefectWarmingServiceImpl
        extends AbstractDefectWarmingTemplate<DUPCDefectEntity>
        implements ColdDataWarmingService {

    private static final int BATCH_PAGE_SIZE = 500;

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.DUPC;
    }

    @Override
    protected List<DUPCDefectEntity> getDefectList(String json) {
        return JSONObject.parseObject(json, new TypeReference<ArchivingFileModel<DUPCDefectEntity>>() {
        }).getDefectList();
    }

    @Override
    protected String getCollectionName() {
        return defectMongoTemplate.getCollectionName(DUPCDefectEntity.class);
    }

    @Override
    protected int getBatchPageSize() {
        // 由于内嵌block_list不可控，保守点
        return BATCH_PAGE_SIZE;
    }

    @Override
    protected void preHandle(long taskId) {
        DUPCDefectEntity dupcDefectEntity = dupcDefectRepository.findFirstByTaskId(taskId);
        if (dupcDefectEntity != null) {
            long delCount = dupcDefectRepository.deleteByTaskId(taskId);
            log.info("defect warming preHandle, task id: {}, del count: {}, dupc", taskId, delCount);
        }
    }
}
