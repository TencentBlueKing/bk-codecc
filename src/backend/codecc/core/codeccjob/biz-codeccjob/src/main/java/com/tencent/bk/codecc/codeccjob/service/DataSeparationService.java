package com.tencent.bk.codecc.codeccjob.service;

public interface DataSeparationService {

    void coolDownTrigger();

    void coolDown(long taskId);

    void warmUp(long taskId);

    void upsertPurgingLog(long taskId, long delCount, long cost, boolean finalResult);

    boolean purgeColdData(long taskId);
}
