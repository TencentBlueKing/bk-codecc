package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 告警代码文件片段查询返回视图
 * @author victorljli
 * @date 2023/07/03
 */
@Data
@ApiModel("告警代码文件片段查询返回视图")
public class DefectFileContentSegmentQueryRspVO extends CommonDefectDetailQueryRspVO {
    @ApiModelProperty("片段开始行号")
    private long beginLine;

    @ApiModelProperty("片段结束行号")
    private long endLine;

    @ApiModelProperty("代码版本号")
    private String revision;

    @ApiModelProperty("代码分支号")
    private String branch;
}
