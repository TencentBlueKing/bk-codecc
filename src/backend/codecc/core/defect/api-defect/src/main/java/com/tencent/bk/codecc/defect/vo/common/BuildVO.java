package com.tencent.bk.codecc.defect.vo.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.devops.common.api.checkerset.CheckerSetsVersionInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 构建视图
 *
 * @version V1.0
 * @date 2019/12/23
 */
@Data
@Schema(description = "构建视图")
public class BuildVO {
    @Schema(description = "构建ID")
    private String buildId;

    @Schema(description = "构建号")
    private String buildNum;

    @Schema(description = "启动构建时间")
    private Long buildTime;

    @Schema(description = "启动构建的用户")
    private String buildUser;

    @Schema(description = "任务Id")
    private Long taskId;

    @Schema(description = "项目Id")
    private String projectId;

    @Schema(description = "流水线Id")
    private String pipelineId;

    @Schema(description = "任务白名单列表")
    private List<String> whitePaths;

    /**
     * 保存当次构建使用的规则集及版本
     */
    @Schema(description = "构建使用的规则集及版本")
    @JsonProperty("checker_sets_version")
    private List<CheckerSetsVersionInfoVO> checkerSetsVersion;
    /**
     * 是否重新分配处理人
     */
    @Schema(description = "是否重新分配处理人")
    @JsonProperty("reallocate")
    private Boolean reallocate;
}
