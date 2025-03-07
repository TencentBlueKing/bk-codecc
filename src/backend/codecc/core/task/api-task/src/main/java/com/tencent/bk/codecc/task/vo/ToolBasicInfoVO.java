package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 工具的基础信息，目前包括工具名、支持语言、默认全量规则集信息
 *
 * @date 2024/03/20
 */
@Data
@ApiModel("工具基础信息视图")
public class ToolBasicInfoVO {

    private String toolName;

    private List<CheckerSetVO> checkerSetList;

}
