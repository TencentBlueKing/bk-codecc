package com.tencent.devops.common.constant;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 告警模块的常量
 *
 * @version V1.0
 * @date 2019/4/23
 */
public interface IgnoreApprovalConstants {


    String DEFAULT_ID_SUFFIX = "0";


    /**
     * 每小时检查一次
     */
    Long CHECK_STATUS_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);

    /**
     * 检查过期时间
     */
    Long IGNORE_APPROVAL_EXPIRED_MS = TimeUnit.HOURS.toMillis(48);

    /**
     * 任务范围类型, ALL,INCLUDE,EXCLUDE
     */
    enum TaskScopeType {
        ALL("ALL"),
        EXCLUDE("EXCLUDE"),
        INCLUDE("INCLUDE");

        private String type;

        TaskScopeType(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        public static TaskScopeType getByType(String type) {
            if (StringUtils.isBlank(type)) {
                return null;
            }
            for (TaskScopeType value : TaskScopeType.values()) {
                if (value.type.equals(type)) {
                    return value;
                }
            }
            return null;
        }
    }

    /**
     * 项目范围类型, SINGLE
     */
    enum ProjectScopeType {
        SINGLE("SINGLE", new String[]{}),

        GONGFENG_PUBLIC("GONGFENG_PUBLIC", new String[]{ComConstants.GONGFENG_PROJECT_ID_PREFIX,
                ComConstants.OTEAM_PROJECT_ID}),

        GONGFENG_PRIVATE("GONGFENG_PRIVATE", new String[]{ComConstants.GONGFENG_PRIVATYE_PROJECT_PREFIX}),

        GITHUB("GITHUB", new String[]{ComConstants.GITHUB_PROJECT_PREFIX});

        private final String type;

        private final String[] projectPrefixes;

        ProjectScopeType(String type, String[] projectPrefixes) {
            this.type = type;
            this.projectPrefixes = projectPrefixes;
        }

        public String type() {
            return type;
        }

        public String[] projectPrefixes() {
            return projectPrefixes;
        }


        public static ProjectScopeType getByProjectId(String projectId) {
            if (StringUtils.isBlank(projectId)) {
                return SINGLE;
            }
            for (ProjectScopeType value : ProjectScopeType.values()) {
                String[] projectPrefixes = value.projectPrefixes;
                for (String projectPrefix : projectPrefixes) {
                    if (projectId.startsWith(projectPrefix)) {
                        return value;
                    }
                }
            }
            return SINGLE;
        }
    }

    /**
     * 审核人类型
     */
    enum ApproverType {
        /**
         * 项目管理员
         */
        PROJECT_MANAGER("PROJECT_MANAGER", "项目管理员"),
        /**
         * 任务管理员
         */
        TASK_MANAGER("TASK_MANAGER", "任务管理员"),
        /**
         * 规则发布者
         */
        CHECKER_PUBLISHER("CHECKER_PUBLISHER", "规则发布者"),
        /**
         * 忽略人LEADER
         */
        IGNORE_AUTHOR_LEADER("IGNORE_AUTHOR_LEADER", "忽略人直属Leader"),
        /**
         * 自定义忽略人
         */
        CUSTOM_APPROVER("CUSTOM_APPROVER", "自定义"),
        /**
         * BG安全负责人
         */
        BG_SECURITY_MANAGER("BG_SECURITY_MANAGER", "BG安全负责人");

        private final String type;

        @Getter
        private final String cnName;

        ApproverType(String type, String cnName) {
            this.type = type;
            this.cnName = cnName;
        }

        public String type() {
            return type;
        }

        public static ApproverType getByType(String type) {
            if (StringUtils.isBlank(type)) {
                return null;
            }
            for (ApproverType value : ApproverType.values()) {
                if (value.type.equals(type)) {
                    return value;
                }
            }
            return null;
        }

        public static String getCnNameByType(String type) {
            if (StringUtils.isBlank(type)) {
                return ComConstants.EMPTY_STRING;
            }
            for (ApproverType value : ApproverType.values()) {
                if (value.type.equals(type)) {
                    return value.cnName;
                }
            }
            return ComConstants.EMPTY_STRING;
        }
    }

    enum ApproverStatus {
        /**
         * 等待提交审批
         */
        SEND_TO_QUEUE(1),
        /**
         * 开始审批（审批中）
         */
        START_TO_APPROVAL(2),
        /**
         * 审批通过
         */
        SUBMIT_SUCC(3),
        /**
         * 审批拒绝
         */
        SUBMIT_FAIL(4);

        private final int status;

        ApproverStatus(int status) {
            this.status = status;
        }

        public int status() {
            return status;
        }

        /**
         * 正在审核(审核未完成的状态列表)
         */
        public static final List<Integer> UNDER_APPROVAL_STATUS = Arrays.asList(
                SEND_TO_QUEUE.status, START_TO_APPROVAL.status
        );

        /**
         * 审核完成
         */
        public static final List<Integer> APPROVAL_FINISH_STATUS = Arrays.asList(
                SUBMIT_SUCC.status, SUBMIT_FAIL.status
        );
    }

    enum DisableConfigEditReason {
        /**
         * 只有管理员
         */
        ONLY_PROJECT_MANAGER,
        /**
         * 统一配置
         */
        UNIFIED_CONFIG;
    }

}
