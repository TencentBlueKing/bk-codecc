package com.tencent.bk.codecc.scanschedule.pojo.input;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.scanschedule.utils.EnvUtils;
import com.tencent.devops.common.api.enums.OSType;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class InputVO {
    private String toolImageType = ""; //镜像类型(P正式，G灰度，T测试)
    private String projName = ""; //项目名称
    private String projectId = ""; //项目ID
    private String scanPath = ""; //扫描路径
    private int language; //扫描语言
    private List<String> whitePathList = Lists.newArrayList(); //白名单
    private List<ToolOptions> toolOptions = Lists.newArrayList(); //工具子选项
    private String buildScript = ""; //编译内容
    private String scanType = "increment"; //扫描类型
    private List<String> skipPaths = Lists.newArrayList(); //黑名单
    private List<String> incrementalFiles = Lists.newArrayList(); //增量文件列表
    private List<OpenCheckers> openCheckers; //规则列表
    private List<Repos> repos = Lists.newArrayList(); //scm repo信息
    private String params = ""; //工具属性
    private String dockerTriggerShell = ""; //docker触发命令
    private String dockerImageUrl = ""; //docker镜像路径
    private String dockerImageVersion = ""; //docker镜像版本
    private String dockerImageHash = ""; //docker镜像hash值
    private String dockerContainerId = ""; //docker容器id
    private String commentTurnOn = ""; //是否开启注释忽略
    private String noLarge = "true"; //scc添加大文件扫描开关
    private String winBinPath = ""; //二进制windows版本安装路径
    private String linuxBinPath = ""; //二进制Linux版本安装路径
    private String macBinPath = ""; //二进制mac版本安装路径
    private String winCommand = ""; //二进制Windows运行命令
    private String linuxCommand = ""; //二进制linux运行命令
    private String macCommand = ""; //二进制mac运行命令
    private List<ToolEnv> depEnv = Lists.newArrayList(); //二进制工具依赖环境

    public void setIncrementalFiles(List<String> incrementalFiles) {
        this.incrementalFiles = incrementalFiles.stream()
                .map(it -> verifyFilePath(it)).collect(Collectors.toList());
    }

    public void setScanPath(String scanPath) {
        this.scanPath = verifyFilePath(scanPath);
    }

    private String verifyFilePath(String path) {
        if (path.contains(" ") && !OSType.WINDOWS.equals(EnvUtils.getOS())) {
            return path.replace(" ", "\\ ");
        }
        return path;
    }
}
