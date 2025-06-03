package com.tencent.devops.common.constant;

import lombok.Getter;

/**
 * 工具相关的常量类
 */
public interface ToolConstants {
    /**
     * 工具集成体系版本号
     */
    @Getter
    enum ToolParamsVersion {
        V2("v2");
        final String value;

        ToolParamsVersion(String value) {
            this.value = value;
        }
    }

    /**
     * 工具注册请求的来源
     */
    @Getter
    enum RegisterRequestSource {
        BKCI("bkci"),
        BKPLUGINS("bkplugins");

        private final String name;

        RegisterRequestSource(String name) {
            this.name = name;
        }
    }
}
