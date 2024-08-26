package com.tencent.bk.codecc.defect.service;


import com.tencent.bk.codecc.defect.model.TaskInvalidToolDefectLog;

public interface TaskInvalidToolDefectService {

    void excludeToolDefect(Long taskId, String createFrom, String buildId, String toolName, String toolType);

    TaskInvalidToolDefectLog getLatestToolLog(Long taskId, String toolName);

}
