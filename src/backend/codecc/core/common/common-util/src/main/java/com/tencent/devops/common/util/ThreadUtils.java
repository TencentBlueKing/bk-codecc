package com.tencent.devops.common.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadUtils {

    /**
     * 线程休眠
     *
     * @param millis 毫秒
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Throwable t) {
            log.error("thread sleep error, id: {}", Thread.currentThread().getId(), t);
        }
    }
}
