package com.tencent.bk.codecc.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolBinaryEntity {

    /**
     * win二进制的下载路径
     */
    @Field("win_url")
    private String winUrl;

    /**
     * linux二进制的下载路径
     */
    @Field("linux_url")
    private String linuxUrl;

    /**
     * mac二进制的下载路径
     */
    @Field("mac_url")
    private String macUrl;

    /**
     * 二进制工具版本
     */
    @Field("url_version")
    private String urlVersion;

    /**
     * win环境下命令行
     */
    @Field("win_command")
    private String winCommand;

    /**
     * linux环境下命令行
     */
    @Field("linux_command")
    private String linuxCommand;

    /**
     * mac环境下命令行
     */
    @Field("mac_command")
    private String macCommand;

    /**
     * 二进制版本
     */
    @Field("bin_version")
    private String binaryVersion;

    /**
     * 二进制依赖环境
     */
    @Field("tool_envs")
    private List<ToolEnvEntity> toolEnvs;
}