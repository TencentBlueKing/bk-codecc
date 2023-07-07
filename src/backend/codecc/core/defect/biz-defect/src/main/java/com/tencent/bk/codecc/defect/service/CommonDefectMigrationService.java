package com.tencent.bk.codecc.defect.service;

import java.util.Set;

public interface CommonDefectMigrationService {

    /**
     * 是否已执行过迁移，不管失败还是成功
     *
     * @param taskId
     * @return
     */
    boolean isMigrationDone(long taskId);

    /**
     * 数据迁移，内部逻辑不含锁
     *
     * @param taskId
     * @param toolName
     * @param triggerUser
     */
    void dataMigration(long taskId, String toolName, String triggerUser);

    /**
     * 是否迁移成功
     *
     * @param taskId
     * @return
     */
    boolean isMigrationSuccessful(long taskId);

    /**
     * 迁移涉及到的工具，注：均为大写
     *
     * @return
     */
    Set<String> matchToolNameSet();

    /**
     * 打开开关：单任务日常模式
     * （与批量模式互斥，自动关闭）
     */
    void switchOnSingleMigrationMode();

    /**
     * 打开开关：批量迁移模式
     * （与单任务模式互斥，自动关闭）
     */
    void switchOnBatchMigrationMode();

    /**
     * 单任务模式是否开启
     *
     * @return true为开启
     */
    boolean isOnSingleMigrationMode();

    /**
     * 批量模式是否开启
     *
     * @return true为开启
     */
    boolean isOnBatchMigrationMode();

    /**
     * 关闭所有开关
     */
    void switchOffAll();
}
