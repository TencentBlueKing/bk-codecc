package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.DefectFilePathClusterDao;
import com.tencent.bk.codecc.defect.model.DefectFilePathClusterEntity;
import com.tencent.bk.codecc.defect.service.DefectFilePathClusterService;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 告警文件存储
 */
@Slf4j
@Service
public class DefectFilePathClusterServiceImpl implements DefectFilePathClusterService {


    @Autowired
    private DefectFilePathClusterDao defectFilePathClusterDao;

    @Override
    public void saveBuildDefectFilePath(Long taskId, String toolName, String buildId, DefectStatus status,
            List<Pair<String, String>> paths) {
        if (CollectionUtils.isEmpty(paths) || status == null) {
            return;
        }
        Date now = new Date();
        List<DefectFilePathClusterEntity> entities = paths.stream().map(path -> {
            DefectFilePathClusterEntity entity = new DefectFilePathClusterEntity();
            entity.setTaskId(taskId);
            entity.setToolName(toolName);
            entity.setBuildId(buildId);
            entity.setFilePath(path.getFirst());
            entity.setRelPath(path.getSecond());
            entity.setStatus(status.value());
            entity.setCreateAt(now);
            return entity;
        }).collect(Collectors.toList());
        defectFilePathClusterDao.save(taskId, toolName, buildId, status.value(), entities);
    }

    @Override
    public List<DefectFilePathClusterEntity> getFilePathList(Long taskId, String toolName, String buildId,
            DefectStatus status, Integer pageSize, Integer pageNum) {
        return defectFilePathClusterDao.findFilePathByPage(taskId, toolName, buildId, status.value(),
                pageSize, pageNum);
    }
}
