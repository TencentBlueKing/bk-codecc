package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo;

import java.util.concurrent.TimeUnit;

public class HotColdConstants {

    public static final String COS_PREFIX_FORMATTER = "/task_id_%d/%s/";

    public static final String COS_PREFIX_FORMATTER_WITH_SHARD = COS_PREFIX_FORMATTER + "shard_%d.xz";

    /**
     * 数据降冷时，分批读取大小
     */
    public static final int BATCH_SIZE_FOR_ARCHIVING = 5_0000;

    /**
     * 数据加热时，分批插入大小
     */
    public static final int BATCH_SIZE_FOR_WARMING = 5000;

    /**
     * 降冷时间差，1年
     */
    public static final long TO_COLD_MILLIS_DIFF = TimeUnit.DAYS.toMillis(365);

    /**
     * 降冷过程上锁时间，跟提单的锁互斥
     */
    public static final long TO_COLD_COMMIT_LOCK_EXPIRE_TIME = TimeUnit.MINUTES.toSeconds(30);

    public static final String DATA_SEPARATION_TRIGGER_LOCK_KEY = "DATA_SEPARATION_TRIGGER_LOCK_KEY";
}
