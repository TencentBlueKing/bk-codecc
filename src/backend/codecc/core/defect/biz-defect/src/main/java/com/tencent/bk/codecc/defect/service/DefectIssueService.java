package com.tencent.bk.codecc.defect.service;


import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DefectIssueService {

    Set<String> getDefectIdByTaskIdAndToolMap(Map<Long, List<String>> taskToolMap);
}
