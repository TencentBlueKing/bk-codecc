package com.tencent.bk.codecc.codeccjob.service;

import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;

/**
 * 冷数据加热
 */
public interface ColdDataWarmingService {

    void warm(long taskId);

    ColdDataArchivingType coldDataArchivingType();
}
