package com.tencent.devops.common.util;

import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

public class NotifyUtils {

    private static final String STREAM_2_0_FLAG = "git_";

    private static final String CODE_ISSUE_FMT = "http://%s/codecc/%s/task/%s/defect/%s/list?dimension=%s";

    private static final String CODE_CCN_FMT = "http://%s/codecc/%s/task/%s/defect/ccn/list";

    public static String getTargetUrl(String projectId, String nameCn, long taskId, String codeccHost,
            String devopsHost, String createFrom) {
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom)) {
            return String.format("http://%s/codecc/%s/task/%s/detail", codeccHost, projectId, taskId);
        }

        if (isGitCi(projectId, nameCn) || isStream2_0(projectId)) {
            return String.format("http://%s/codecc/%s/task/%s/detail", codeccHost, projectId, taskId);
        }

        return String.format("http://%s/console/codecc/%s/task/%s/detail", devopsHost, projectId, taskId);
    }

    public static String getResolveHtmlEmail(String projectId, String nameCn,
            String codeccHost, String devopsHost, String htmlEmail) {
        if (isGitCi(projectId, nameCn) || isStream2_0(projectId)) {
            return htmlEmail.replaceAll(devopsHost + "/console", codeccHost);
        }

        return htmlEmail;
    }

    @Deprecated
    public static String getBotTargetUrl(String projectId, String nameCn, long taskId,
            String toolName, String codeccHost, String devopsHost) {
        if (isGitCi(projectId, nameCn)) {
            return "http://" + codeccHost + String.format("/codecc/%s/task/%s/defect/compile/%s/list?dimension=DEFECT",
                    projectId, taskId, toolName);
        }
        return "http://" + devopsHost + String.format(
                "/console/codecc/%s/task/%s/defect/compile/%s/list?dimension=DEFECT",
                projectId, taskId, toolName);
    }

    public static boolean isGitCi(String projectId, String nameCn) {
        return projectId.startsWith("git_") && nameCn.startsWith(projectId);
    }

    /**
     * 是否来自Stream2.0创建的任务
     * 注：projectId格式为"git_"开头，后面跟一串数字
     *
     * @param projectId
     * @return
     */
    public static boolean isStream2_0(String projectId) {
        if (StringUtils.isNotEmpty(projectId) && projectId.startsWith(STREAM_2_0_FLAG)) {
            String subStr = StringUtils.substringAfter(projectId, STREAM_2_0_FLAG);
            // StringUtils.isNumeric()对于空串""会返回true，必须先判空处理
            return StringUtils.isNotEmpty(subStr) && StringUtils.isNumeric(subStr);
        }

        return false;
    }

    /**
     * 判定这两个前缀开头的项目都是来自gongfeng_scan
     * @param projectId 项目id
     * @return boolean
     */
    public static boolean isGongfengScanProject(@NotNull String projectId) {
        return projectId.startsWith(ComConstants.GONGFENG_PROJECT_ID_PREFIX)
                || projectId.startsWith(ComConstants.CUSTOMPROJ_ID_PREFIX);
    }

    /**
     * 跳转到代码问题页面
     */
    public static String getCodeIssueUrl(String projectId, String nameCn, long taskId, String codeccHost,
            String devopsHost, String createFrom, String dimension) {
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom) || isGitCi(projectId, nameCn)
                || isStream2_0(projectId)) {
            return String.format(CODE_ISSUE_FMT, codeccHost, projectId, taskId, dimension.toLowerCase(), dimension);
        }
        return String
                .format("http://%s/console/codecc/%s/task/%s/defect/%s/list?dimension=%s", devopsHost, projectId,
                        taskId, dimension.toLowerCase(), dimension);
    }

    /**
     * 跳转到圈复杂度页面
     */
    public static String getCodeCcnUrl(String projectId, String nameCn, long taskId, String codeccHost,
            String devopsHost, String createFrom) {
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom) || isGitCi(projectId, nameCn)
                || isStream2_0(projectId)) {
            return String.format(CODE_CCN_FMT, codeccHost, projectId, taskId);
        }
        return String.format("http://%s/console/codecc/%s/task/%s/defect/ccn/list", devopsHost, projectId, taskId);
    }
}
