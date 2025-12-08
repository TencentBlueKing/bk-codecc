package com.tencent.bk.codecc.task.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 工具的单个自定义参数
 *
 * @date 2025/02/17
 */
@Data
public class ToolOptionEntity {
    @Field("var_name")
    private String varName;
    @Field("var_type")
    private String varType;
    @Field("label_name")
    private String labelName;
    @Field("var_default")
    private String varDefault;
    @Field("var_tips")
    private String varTips;
    @Field("var_required")
    private Boolean varRequired;
    @Field("full_scan_on_change")
    private Boolean fullScanOnChange;
    @Field("var_option_list")
    private List<VarOptionEntity> varOptionList;
}
