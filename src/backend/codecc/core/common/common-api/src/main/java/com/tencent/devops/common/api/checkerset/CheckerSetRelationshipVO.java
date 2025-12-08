package com.tencent.devops.common.api.checkerset;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import jakarta.ws.rs.PathParam;
import java.util.Set;

/**
 * 规则集与其他对象关系视图
 *
 * @version V1.0
 * @date 2020/1/5
 */
@Data
@Schema(description = "规则集与其他对象关系视图")
public class CheckerSetRelationshipVO
{
    @Schema(description = "关系类型", required = true)
    private String type;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "规则集版本号")
    Integer version;

    @Schema(description = "规则集集合")
    Set<String> checkerSetIds;
}
