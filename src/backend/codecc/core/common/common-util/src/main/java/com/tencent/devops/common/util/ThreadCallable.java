package com.tencent.devops.common.util;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CodeCC 线程回调自动以封装
 *
 * @param <T>
 */
public class ThreadCallable<T> implements Callable<T> {

    private static final Logger logger = LoggerFactory.getLogger(ThreadCallable.class);

    private String traceBuildId;

    private Callable<T> task;

    public ThreadCallable(String traceBuildId, Callable<T> task) {
        this.traceBuildId = traceBuildId;
        this.task = task;
    }

    @Override
    public T call() throws Exception {
        if (traceBuildId != null) {
            TraceBuildIdThreadCacheUtils.INSTANCE.setBuildId(traceBuildId);
        }
        try {
            logger.info("@@@@@@@task start");
            return task.call();
        } finally {
            TraceBuildIdThreadCacheUtils.INSTANCE.removeBuildId();
            logger.info("@@@@@@@task end");
        }
    }
}
