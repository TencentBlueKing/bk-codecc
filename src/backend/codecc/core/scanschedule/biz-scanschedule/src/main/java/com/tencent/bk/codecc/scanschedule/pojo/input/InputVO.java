package com.tencent.bk.codecc.scanschedule.pojo.input;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

/**
 * 输入对象类
 * @author jimxzcai
 */
@Data
public class InputVO {

    /**
     * 工具名称
     */
    public String toolName = "";

    /**
     * 项目名称
     */
    public String projName = "";

    /**
     * 项目ID
     */
    public String projectId = "";

    /**
     * 扫描路径
     */
    public String scanPath = "";

    /**
     * 扫描语言
     */
    public int language;

    /**
     * 白名单
     */
    public List<String> whitePathList = Lists.newArrayList();

    /**
     * 工具子选项
     */
    public List<ToolOptions> toolOptions = Lists.newArrayList();

    /**
     * 编译内容
     */
    public String buildScript = "";

    /**
     * 扫描类型
     */
    public String scanType = "increment";

    /**
     * 黑名单
     */
    public List<String> skipPaths = Lists.newArrayList();

    /**
     * 增量文件列表
     */
    public List<String> incrementalFiles = Lists.newArrayList();

    /**
     * 规则列表
     */
    public List<OpenCheckers> openCheckers;

    /**
     * 工具属性
     */
    public String params = "";
}
