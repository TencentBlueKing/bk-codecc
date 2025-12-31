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

    /**
     * 插件开发者中心的角色名
     */
    enum BKRoleType {
        MANAGER("管理员"),
        DEVELOPER("开发者");

        final String value;

        BKRoleType(String v) {
            this.value = v;
        }

        public String getValue() {
            return this.value;
        }
    }
}
