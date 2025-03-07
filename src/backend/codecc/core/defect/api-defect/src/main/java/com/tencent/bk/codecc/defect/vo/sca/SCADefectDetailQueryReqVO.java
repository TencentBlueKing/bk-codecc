package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("SCA工具详情查询请求参数")
public class SCADefectDetailQueryReqVO extends CommonDefectDetailQueryReqVO {
    @ApiModelProperty(value = "任务id")
    private Long taskId;

    @ApiModelProperty(value = "许可证名称")
    private String licenseName;

    @ApiModelProperty(value = "漏洞数据id")
    private String vulEntityId;

    @ApiModelProperty(value = "组件名称")
    private String packageName;

    @ApiModelProperty(value = "SCA维度")
    private List<String> scaDimensionList;
}