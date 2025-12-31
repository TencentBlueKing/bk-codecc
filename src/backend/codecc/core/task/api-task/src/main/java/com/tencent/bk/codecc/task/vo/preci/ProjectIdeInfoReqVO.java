package com.tencent.bk.codecc.task.vo.preci;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "项目IDE信息")
public class ProjectIdeInfoReqVO {

    @Schema(description = "本地系统类型")
    private String osType;

    @Schema(description = "本地IP")
    private String hostIp;

    @Schema(description = "本地机器名称")
    private String hostName;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目路径")
    private String projectPath;

    @Schema(description = "IDE类型")
    private String ideType;

    @Schema(description = "IDE名称")
    private String ideName;

    @Schema(description = "IDEA程序启动路径")
    private String idePath;

    @Schema(description = "git路径")
    private String gitUrl;

    @Schema(description = "注册表名称")
    private String register;
}