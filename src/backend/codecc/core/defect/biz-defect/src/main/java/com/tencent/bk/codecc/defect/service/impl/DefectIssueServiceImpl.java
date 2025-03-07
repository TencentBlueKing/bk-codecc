package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.service.DefectIssueService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DefectIssueServiceImpl implements DefectIssueService {
    @Override
    public Set<String> getDefectIdByTaskIdAndToolMap(Map<Long, List<String>> taskToolMap) {
        return null;
    }
}
