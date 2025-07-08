/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一线程池工具类
 *
 * @version V4.0
 * @date 2019/3/4
 */
public class ThreadPoolUtil {

    private static Logger logger = LoggerFactory.getLogger(ThreadPoolUtil.class);

    /**
     * 核心线程数
     */
    private static int CORE_POOL_SIZE = 0;

    /**
     * 线程池容量
     */
    private static int MAXIMUM_POOL_SIZE = 1024;

    /**
     * 空闲线程等待时间
     */
    private static long KEEP_ALIVE_TIME = 0;

    /**
     * 线程工厂
     */
    private static ThreadFactory NAMED_THREAD_FACTORY = new ThreadFactoryBuilder().setNameFormat(
            "codecc-business-pool-%d").build();

    /**
     * 执行器（由于目前都是能快速完成的小任务，因此不需要维持阻塞队列，使用SynchronousQueue）
     */
    private static ExecutorService EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(), NAMED_THREAD_FACTORY, new ThreadPoolExecutor.AbortPolicy());

    /**
     * 禁用构造方法
     */
    private ThreadPoolUtil() {
    }

    /**
     * 通用异步执行任务，防止新线程中获取Redis连接没有释放，这里统一释放
     *
     * @param task
     */
    public static void addRunnableTask(Runnable task) {
        // 封装自动的Runnable，可以增加切面
        ThreadRunnable runnable = new ThreadRunnable(TraceBuildIdThreadCacheUtils.INSTANCE.getBuildId(), task);
        EXECUTOR.execute(runnable);
    }


    /**
     * 调用callable进行异步处理
     *
     * @version V3.5.0
     * @date 2019/3/8
     */
    public static <T> Future<T> addCallableTask(Callable<T> task) {
        // 封装自动的ThreadCallable，可以增加切面
        ThreadCallable callable = new ThreadCallable(TraceBuildIdThreadCacheUtils.INSTANCE.getBuildId(), task);
        return EXECUTOR.submit(callable);
    }
}
