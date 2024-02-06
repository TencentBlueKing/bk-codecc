package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.archiving;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataArchivingService;
import com.tencent.bk.codecc.defect.model.CodeBlockEntity;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DUPCDefectArchivingServiceImpl
        extends AbstractDefectArchivingTemplate<DUPCDefectEntity>
        implements ColdDataArchivingService {

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.DUPC;
    }

    @Override
    protected List<DUPCDefectEntity> getDefectListCore(long taskId, Pageable pageable) {
        List<DUPCDefectEntity> defectList = dupcDefectRepository.findByTaskId(taskId, pageable);

        // mock提单逻辑，防止历史数据有超大blocklist导致OutOfMemoryError
        for (DUPCDefectEntity entity : defectList) {
            if (entity != null
                    && entity.getBlockList() != null
                    && entity.getBlockList().size() > ComConstants.DUPC_DEFECT_BLOCK_LIST_LIMIT) {

                List<CodeBlockEntity> limitedBlockList = entity.getBlockList()
                        .subList(0, ComConstants.DUPC_DEFECT_BLOCK_LIST_LIMIT);
                entity.setBlockList(limitedBlockList);
            }
        }

        return defectList;
    }
}
