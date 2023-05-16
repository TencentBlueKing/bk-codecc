package com.tencent.bk.codecc.defect.service;


/**
 * 重新分配问题处理人
 */
public interface ReallocateDefectAuthorService {

    boolean isReallocate(Long taskId, String toolName);


    void updateCurrentStatus(Long taskId, String toolName);
}
