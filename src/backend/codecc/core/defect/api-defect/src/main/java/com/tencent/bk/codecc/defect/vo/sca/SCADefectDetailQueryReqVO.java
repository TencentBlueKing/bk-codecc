package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SCA工具详情查询请求参数")
public class SCADefectDetailQueryReqVO extends CommonDefectDetailQueryReqVO {
    @Schema(description = "任务id")
    private Long taskId;

    @Schema(description = "许可证名称")
    private String licenseName;

    @Schema(description = "漏洞数据id")
    private String vulEntityId;

    @Schema(description = "组件名称")
    private String packageName;

    @Schema(description = "SCA维度")
    private List<String> scaDimensionList;
}