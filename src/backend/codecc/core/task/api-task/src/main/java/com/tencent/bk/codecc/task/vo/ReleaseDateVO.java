package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布日期视图
 *
 * @version V1.0
 * @date 2021/8/18
 */

@Data
@Schema(description = "发布日期视图")
public class ReleaseDateVO {

    @Schema(description = "管理类型")
    private String manageType;

    @Schema(description = "版本类型")
    private String versionType;

    @Schema(description = "正式版日期")
    private Long prodDate;

    @Schema(description = "预发布版日期")
    private Long preProdDate;

}
