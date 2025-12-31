package com.tencent.devops.common.constant;

/**
 * 监控相关的常量
 */
public interface MonitorConstants {
    int SCAN_TIME_HISTORY_CAP = 200;

    enum TaskResult {
        SUCC("SUCC"),
        FAIL("FAIL");

        private final String code;

        TaskResult(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
