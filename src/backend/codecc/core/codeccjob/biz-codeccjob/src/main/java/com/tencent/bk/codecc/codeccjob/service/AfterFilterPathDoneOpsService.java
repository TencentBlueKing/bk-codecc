package com.tencent.bk.codecc.codeccjob.service;

import java.util.List;

/**
 * 过滤路径设置结束后的操作类
 */
public interface AfterFilterPathDoneOpsService {

    void doAfterFilterPathDone(long taskId, String toolName, List defectList);

}
