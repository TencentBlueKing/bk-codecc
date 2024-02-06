package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.DefectFilePathClusterEntity;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import java.util.List;
import org.springframework.data.util.Pair;

public interface DefectFilePathClusterService {


    /**
     * 保存构建后仍然存在的告警文件路径
     * @param taskId
     * @param toolName
     * @param buildId
     * @param paths
     */
    void saveBuildDefectFilePath(Long taskId, String toolName, String buildId, DefectStatus status,
            List<Pair<String,String>> paths);



    List<DefectFilePathClusterEntity> getFilePathList(Long taskId, String toolName, String buildId, DefectStatus status,
            Integer pageSize, Integer pageNum);
}
