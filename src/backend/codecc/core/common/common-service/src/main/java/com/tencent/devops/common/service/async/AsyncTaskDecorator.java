package com.tencent.devops.common.service.async;

import com.tencent.devops.common.util.ThreadRunnable;
import com.tencent.devops.common.util.TraceBuildIdThreadCacheUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.task.TaskDecorator;

/**
 * Runnable 装饰器 @Async 注解增强
 */
public class AsyncTaskDecorator implements TaskDecorator {

    @NotNull
    @Override
    public Runnable decorate(@NotNull Runnable runnable) {
        return new ThreadRunnable(TraceBuildIdThreadCacheUtils.INSTANCE.getBuildId(), runnable);
    }
}
