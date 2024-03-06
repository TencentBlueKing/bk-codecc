package com.tencent.bk.codecc.codeccjob.service;

import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;

/**
 * 冷数据归档
 */
public interface ColdDataArchivingService {

    boolean archive(long taskId);

    ColdDataArchivingType coldDataArchivingType();
}
