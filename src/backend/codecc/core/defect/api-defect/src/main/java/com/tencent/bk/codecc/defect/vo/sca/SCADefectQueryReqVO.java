package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("SCA告警列表查询请求参数")
public class SCADefectQueryReqVO extends DefectQueryReqVO {
    @ApiModelProperty("SCA维度")
    private List<String> scaDimensionList;

    @ApiModelProperty("依赖方式")
    private Boolean direct;

    @ApiModelProperty("组件名称关键字搜索查询")
    private String keyword;

    @ApiModelProperty("语言列表")
    private List<String> languageList;

    @ApiModelProperty("处理人")
    private List<String> authors;
}
