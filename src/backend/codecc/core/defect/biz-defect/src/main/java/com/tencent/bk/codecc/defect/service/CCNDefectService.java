package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.component.DefectIdGenerator;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.devops.common.constant.ComConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CCNDefectService {

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private DefectIdGenerator defectIdGenerator;

    public Map<Long, Integer> genId(Set<Long> taskIdSet) {
        Map<Long, Integer> countMap = new HashMap<>();
        String toolName = ComConstants.Tool.CCN.name();
        for (Long taskId: taskIdSet) {
            List<CCNDefectEntity> defectList = ccnDefectRepository.findByTaskIdAndIdIsNull(taskId);
            Long currMaxId = defectIdGenerator.generateDefectId(taskId, toolName, defectList.size());
            long startIndex = currMaxId - defectList.size() + 1;
            for (CCNDefectEntity entity : defectList) {
                entity.setId(String.valueOf(startIndex));
                startIndex++;
            }
            ccnDefectRepository.saveAll(defectList);
            countMap.put(taskId, defectList.size());
        }
        return countMap;
    }
}
