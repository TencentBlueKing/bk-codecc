package com.tencent.bk.codecc.task.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 随机测试开始接口的请求视图
 *
 * @date 2024/04/09
 */
@Data
@Schema(description = "随机测试开始接口的请求视图")
public class StartRandomTestReqVO {

    @NotBlank
    @Schema(description = "工具名")
    private String toolName;
    @NotBlank
    @Schema(description = "版本号")
    private String version;
    @Schema(description = "用户名")
    private String userName;
    @Schema(description = "代码库数量")
    private Integer need;
    @Schema(description = "语言信息数位码")
    private Long langDigit;
    @Schema(description = "代码库体量属性的id")
    private String repoScaleId;

}
