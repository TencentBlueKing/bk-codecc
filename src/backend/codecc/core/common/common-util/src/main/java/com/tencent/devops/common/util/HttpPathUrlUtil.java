package com.tencent.devops.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HttpPathUrlUtil {
    private static Logger logger = LoggerFactory.getLogger(HttpPathUrlUtil.class);
    private static Map<String, String> urlRouterMap = new HashMap<String, String>()
    {{
        put("COVERITY", "compile/");
        put("KLOCWORK", "compile/");
        put("CCN", "");
        put("DUPC", "");
    }};

    public static String getTargetUrl(String devopsHost, String projectId, long taskId, String toolName)
    {
        StringBuilder sb = new StringBuilder();
        String route = urlRouterMap.get(toolName);
        if (route == null) route = "lint/";

        sb.append("http://")
            .append(devopsHost)
            .append("/console/codecc/")
            .append(projectId)
            .append("/task/")
            .append(taskId)
            .append("/defect/")
            .append(route)
            .append(toolName)
            .append("/list");
        return sb.toString();
    }

    public static String getLintTargetUrl(String host, String projectId, long taskId, String toolName,
            String entityId, Boolean console) {
        StringBuilder sb = new StringBuilder();
        String route = urlRouterMap.get(toolName);
        if (route == null) route = "lint/";

        sb.append("http://")
            .append(host)
            .append(console ? "/console" : "")
            .append("/codecc/")
            .append(projectId)
            .append("/task/")
            .append(taskId)
            .append("/defect/")
            .append(route)
            .append(toolName)
            .append("/list")
            .append(String.format("?entityId=%s", entityId));
        logger.info("getLintTargetUrl url: [{}]", sb.toString());
        return sb.toString();
    }

    /**
     * 获取CcnTargetUrl
     *
     * @param host codeccHost/devopsHost
     * @param projectId  项目ID
     * @param taskId     任务ID
     * @param toolName   工具名
     * @param entityId   ID
     * @param filePath   文件路径
     * @return string
     */
    public static String getCcnTargetUrl(String host, String projectId, long taskId, String toolName,
            String entityId, String filePath, Boolean console) {
        StringBuilder sb = new StringBuilder();
        String route = urlRouterMap.get(toolName);
        if (route == null) route = "lint/";

        sb.append("http://")
            .append(host)
            .append(console ? "/console" : "")
            .append("/codecc/")
            .append(projectId)
            .append("/task/")
            .append(taskId)
            .append("/defect/")
            .append(route)
            .append(toolName)
            .append("/list")
            .append(String.format("?entityId=%s&filePath=%s", entityId, filePath));
        logger.info("getCcnTargetUrl url: [{}]", sb.toString());
        return sb.toString();
    }

    public static String getCodeccTargetUrl(String codeccHost, String projectId, long taskId) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://")
                .append(codeccHost)
                .append("/codecc/")
                .append(projectId)
                .append("/task/")
                .append(taskId)
                .append("/settings/code");
        return sb.toString();
    }
}
