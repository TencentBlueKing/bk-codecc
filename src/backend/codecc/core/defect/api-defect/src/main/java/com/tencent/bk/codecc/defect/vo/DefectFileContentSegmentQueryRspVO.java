package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 告警代码文件片段查询返回视图
 *
 * @date 2023/07/03
 */
@Data
@Schema(description = "告警代码文件片段查询返回视图")
public class DefectFileContentSegmentQueryRspVO extends CommonDefectDetailQueryRspVO {
    @Schema(description = "片段开始行号")
    private long beginLine;

    @Schema(description = "片段结束行号")
    private long endLine;

    @Schema(description = "代码版本号")
    private String revision;

    @Schema(description = "代码分支号")
    private String branch;
}
