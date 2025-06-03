package com.tencent.devops.common.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class MultenantUtils {
    /**
     * 在多租户模式下, 蓝盾的项目 id 变成 [租户id].[项目id]
     */
    public static Pair<String, String> splitProjectId(String projectId) {
        if (StringUtils.isEmpty(projectId) || !projectId.contains(".")) {
            return null;
        }

        String[] sub = projectId.split("\\.");
        if (sub.length != 2) {
            return null;
        }

        return Pair.of(sub[0], sub[1]);
    }
}
