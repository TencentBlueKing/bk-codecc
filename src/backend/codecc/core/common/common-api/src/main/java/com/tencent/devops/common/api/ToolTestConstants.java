package com.tencent.devops.common.api;

/**
 * 工具上架模块常量类
 */
public interface ToolTestConstants {

    /**
     * 插件开发者中心的角色 ID
     */
    enum BKRoleId {
        MANAGER(2),
        DEVELOPER(3);

        final Integer value;

        BKRoleId(Integer v) {
            this.value = v;
        }

        public Integer getValue() {
            return this.value;
        }
    }
}
