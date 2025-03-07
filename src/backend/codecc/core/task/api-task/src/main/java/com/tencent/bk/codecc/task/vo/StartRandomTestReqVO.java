package com.tencent.bk.codecc.task.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 随机测试开始接口的请求视图
 *
 * @date 2024/04/09
 */
@Data
@ApiModel("随机测试开始接口的请求视图")
public class StartRandomTestReqVO {

    @NotBlank
    @ApiModelProperty("工具名")
    private String toolName;
    @NotBlank
    @ApiModelProperty("版本号")
    private String version;
    @ApiModelProperty("用户名")
    private String userName;
    @ApiModelProperty("代码库数量")
    private Integer need;
    @ApiModelProperty("语言信息数位码")
    private Long langDigit;
    @ApiModelProperty("代码库体量属性的id")
    private String repoScaleId;

}
