package com.tencent.bk.codecc.defect.utils;

import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

public class BKToolUtils {

    private static final RedisTemplate<String, String> redisTemplate;

    static {
        redisTemplate = SpringContextUtil.Companion.getBean(RedisTemplate.class, "stringRedisTemplate");
    }

    public static Set<String> getBKToolNameSet() {
        return redisTemplate.opsForSet().members(RedisKeyConstants.BK_TOOL_NAME_SET);
    }

    public static boolean isBKTool(String toolName) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(RedisKeyConstants.BK_TOOL_NAME_SET, toolName));
    }
}
