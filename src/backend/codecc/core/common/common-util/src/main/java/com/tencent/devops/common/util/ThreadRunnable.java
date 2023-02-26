package com.tencent.devops.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CodeCC 多线程Runnable 封装
 */
public class ThreadRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolUtil.class);

    private String traceBuildId;

    private Runnable task;

    public ThreadRunnable(String traceBuildId, Runnable task) {
        this.traceBuildId = traceBuildId;
        this.task = task;
    }

    @Override
    public void run() {

        if (traceBuildId != null) {
            TraceBuildIdThreadCacheUtils.INSTANCE.setBuildId(traceBuildId);
        }
        try {
            logger.info("@@@@@@@task start");
            task.run();
            logger.info("@@@@@@@task end");
        } finally {
            TraceBuildIdThreadCacheUtils.INSTANCE.removeBuildId();
        }
    }

}
