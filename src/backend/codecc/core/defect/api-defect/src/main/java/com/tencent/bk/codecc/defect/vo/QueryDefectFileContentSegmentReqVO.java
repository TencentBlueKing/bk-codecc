package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * 查询告警代码片段查询类
 *
 * @author victorljli
 * @date 2023/06/30
 */
@Data
public class QueryDefectFileContentSegmentReqVO {

    @ApiModelProperty(value = "告警 ID", required = true)
    @NotEmpty(message = "告警 ID 不能为空")
    private String entityId;

    @ApiModelProperty(value = "工具名称", required = false)
    private String toolName;

    @ApiModelProperty(value = "工具维度", required = false)
    private String dimension;

    @ApiModelProperty(value = "文件完整路径", required = false)
    @NotNull
    private String filePath;

    @ApiModelProperty(value = "开始行", required = false)
    private int beginLine;

    @ApiModelProperty(value = "结束行", required = false)
    private int endLine;
}
