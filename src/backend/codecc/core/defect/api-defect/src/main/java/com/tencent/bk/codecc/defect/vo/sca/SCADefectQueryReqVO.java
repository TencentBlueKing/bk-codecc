package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SCA告警列表查询请求参数")
public class SCADefectQueryReqVO extends DefectQueryReqVO {
    @Schema(description = "SCA维度")
    private List<String> scaDimensionList;

    @Schema(description = "依赖方式")
    private Boolean direct;

    @Schema(description = "组件名称关键字搜索查询")
    private String keyword;

    @Schema(description = "语言列表")
    private List<String> languageList;

    @Schema(description = "处理人")
    private List<String> authors;
}
