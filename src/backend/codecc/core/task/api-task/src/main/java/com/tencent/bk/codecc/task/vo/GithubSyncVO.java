package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "GitHub同步配置")
@Data
@EqualsAndHashCode(callSuper = true)
public class GithubSyncVO extends CommonVO {

    @Schema(description = "value")
    private String value;

    @Schema(description = "paramType")
    private String paramType;

}
