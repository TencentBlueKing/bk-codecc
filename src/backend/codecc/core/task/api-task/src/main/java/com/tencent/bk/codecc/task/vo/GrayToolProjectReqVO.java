
package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 灰度工具项目
 *
 * @version V1.0
 * @date 2021/11/12
 */
@Data
@Schema(description = "灰度项目请求体")
@EqualsAndHashCode(callSuper = true)
public class GrayToolProjectReqVO extends CommonVO {
    /**
     * 项目Id
     */
    @Schema(description = "项目Id")
    private String projectId;

    @Schema(description = "工具id")
    private String toolName;

    /**
     * 项目灰度状态
     */
    @Schema(description = "项目灰度状态")
    private Integer status;

    /**
     * 项目灰度状态
     */
    @Schema(description = "是否开源治理项目")
    private Boolean openSourceProject;

    /**
     * 配置参数
     */
    @Schema(description = "配置参数")
    private Map<String, Object> configureParam;

    /**
     * 接口人
     */
    @Schema(description = "接口人")
    private String projectOwner;

    /**
     * 原因
     */
    @Schema(description = "原因")
    private String reason;

    /**
     * 筛除机器创建项目(0:筛除 1:不筛除)
     */
    @Schema(description = "筛除机器创建项目")
    private Integer hasRobotTaskBool;
}
