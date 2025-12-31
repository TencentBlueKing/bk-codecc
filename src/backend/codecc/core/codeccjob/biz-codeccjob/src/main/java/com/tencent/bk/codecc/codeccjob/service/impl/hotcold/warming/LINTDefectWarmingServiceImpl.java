package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.warming;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataWarmingService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.ArchivingFileModel;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LINTDefectWarmingServiceImpl
        extends AbstractDefectWarmingTemplate<LintDefectV2Entity>
        implements ColdDataWarmingService {

    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.LINT;
    }

    @Override
    protected List<LintDefectV2Entity> getDefectList(String json) {
        return JSONObject.parseObject(json, new TypeReference<ArchivingFileModel<LintDefectV2Entity>>() {
        }).getDefectList();
    }

    @Override
    protected String getCollectionName() {
        return defectMongoTemplate.getCollectionName(LintDefectV2Entity.class);
    }

    @Override
    protected void preHandle(long taskId) {
        LintDefectV2Entity lintDefectV2Entity = lintDefectV2Repository.findFirstByTaskId(taskId);
        if (lintDefectV2Entity != null) {
            long delCount = lintDefectV2Repository.deleteByTaskId(taskId);
            log.info("defect warming preHandle, task id: {}, del count: {}, lint", taskId, delCount);
        }
    }
}
