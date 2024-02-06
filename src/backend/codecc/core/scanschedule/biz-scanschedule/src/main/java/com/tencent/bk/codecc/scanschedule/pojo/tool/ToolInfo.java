package com.tencent.bk.codecc.scanschedule.pojo.tool;

import lombok.Data;

/**
 * 工具信息类
 * @author jimxzcai
 */
@Data
public class ToolInfo {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具显示名称
     */
    private String displayName = "";

    /**
     * 工具支持语言
     */
    private int lang;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具属性
     */
    private String params = "";

    /**
     * 工具运行命令
     */
    private String dockerTriggerShell = "";

    /**
     * 工具镜像路径
     */
    private String dockerImageURL = "";

    /**
     * 工具镜像版本
     */
    private String dockerImageVersion = "";

    /**
     * 工具镜像hash值
     */
    private String toolImageRevision = "";

    /**
     * 二进制对象
     */
    private Binary binary;
}
