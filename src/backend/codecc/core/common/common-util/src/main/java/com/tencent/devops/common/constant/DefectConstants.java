package com.tencent.devops.common.constant;

/**
 * 告警相关的常量类
 */
public interface DefectConstants {
    enum DefectStatus {
        NEW(1, 0),
        FIXED(2, 1),
        IGNORE(4, 2),
        PATH_MASK(8, 3),
        CHECKER_MASK(16, 4);

        private final int value;
        private final int exp;  // value = 1 << exp

        DefectStatus(int value, int exp) {
            this.value = value;
            this.exp = exp;
        }

        public int value() {
            return value;
        }

        public int exp() {
            return exp;
        }
    }

    /**
     * 忽略原因类型
     */
    enum IgnoreReasonType {
        UNDEFINED(0),                // 由于 int 字段 unset 后 mongo 会置为 0, 故保留该字段作为无定义忽略类型
        USER_NEGATIVE_JUDGE(1),     // 检查工具规则误报 (用户标注)
        LLM_NEGATIVE_JUDGE(147);    // 检查工具规则误报 (LLM 标注), 即 LLM 误报忽略

        private final int value;

        IgnoreReasonType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    enum SCADefectSeverity {
        UNKNOWN(0),
        HIGH(1),
        MEDIUM(2),
        LOW(3),
        CRITICAL(1);

        private final int value;

        SCADefectSeverity(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static int getSeverityByValue(String name) {
            for (SCADefectSeverity severity : SCADefectSeverity.values()) {
                if (severity.name().equalsIgnoreCase(name)) {
                    return severity.value;
                }
            }
            return UNKNOWN.value();
        }
    }

    enum SCASbomType {
        SPDX("spdx"),
        CYCLONEDX("cyclonedx");

        private final String value;

        SCASbomType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
