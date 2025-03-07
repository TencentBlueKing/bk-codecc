package com.tencent.bk.codecc.defect.vo.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.devops.common.api.checkerset.CheckerSetsVersionInfoVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 构建视图
 *
 * @version V1.0
 * @date 2019/12/23
 */
@Data
@ApiModel("构建视图")
public class BuildVO {
    @ApiModelProperty("构建ID")
    private String buildId;

    @ApiModelProperty("构建号")
    private String buildNum;

    @ApiModelProperty("启动构建时间")
    private Long buildTime;

    @ApiModelProperty("启动构建的用户")
    private String buildUser;

    @ApiModelProperty("任务Id")
    private Long taskId;

    @ApiModelProperty("项目Id")
    private String projectId;

    @ApiModelProperty("流水线Id")
    private String pipelineId;

    @ApiModelProperty("任务白名单列表")
    private List<String> whitePaths;

    /**
     * 保存当次构建使用的规则集及版本
     */
    @ApiModelProperty("构建使用的规则集及版本")
    @JsonProperty("checker_sets_version")
    private List<CheckerSetsVersionInfoVO> checkerSetsVersion;
    /**
     * 是否重新分配处理人
     */
    @ApiModelProperty("是否重新分配处理人")
    @JsonProperty("reallocate")
    private Boolean reallocate;
}
