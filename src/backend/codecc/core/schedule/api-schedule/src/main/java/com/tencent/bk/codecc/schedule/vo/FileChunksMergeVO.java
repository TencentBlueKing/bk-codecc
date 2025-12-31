package com.tencent.bk.codecc.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import jakarta.validation.constraints.NotNull;

/**
 * 文件分片合并
 *
 * @author zuihou
 * @date 2018/08/28
 */
@Data
@ToString
@Schema(description = "文件合并实体")
public class FileChunksMergeVO
{
    @NotNull(message = "文件名不能为空")
    @Schema(description = "文件名", required = true)
    private String fileName;

    @Schema(description = "分片总数")
    private Integer chunks;

    @NotNull(message = "上传类型不能为空")
    @Schema(description = "上传类型")
    private String uploadType;

    @NotNull(message = "构建ID不能为空")
    @Schema(description = "构建ID")
    private String buildId;
}
