package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * 查询告警代码片段查询类
 *
 * @date 2023/06/30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QueryDefectFileContentSegmentReqVO {

    @Schema(description = "告警 ID", required = true)
    @NotEmpty(message = "告警 ID 不能为空")
    private String entityId;

    @Schema(description = "工具名称", required = false)
    private String toolName;

    @Schema(description = "工具维度", required = false)
    private String dimension;

    @Schema(description = "文件完整路径", required = true)
    @NotNull
    private String filePath;

    @Schema(description = "开始行", required = false)
    private int beginLine;

    @Schema(description = "结束行", required = false)
    private int endLine;

    /**
     * 是否要 "尽最大努力" 查看私有库代码
     */
    private boolean tryBestForPrivate = true;
}
