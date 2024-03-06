package com.tencent.bk.codecc.codeccjob.service;

import com.tencent.devops.common.constant.ComConstants.ColdDataPurgingType;

/**
 * 冷数据清理
 */
public interface ColdDataPurgingService {

    boolean purge(long taskId);

    /**
     * 执行优先级，数字越小越先执行
     *
     * @return
     */
    int order();

    ColdDataPurgingType coldDataPurgingType();
}
