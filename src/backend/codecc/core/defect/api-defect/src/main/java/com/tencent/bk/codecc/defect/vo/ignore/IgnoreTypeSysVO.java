package com.tencent.bk.codecc.defect.vo.ignore;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统默认的告警忽略类型
 *
 * @date 2022/7/7
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IgnoreTypeSysVO extends CommonVO {
    /**
     * 名字
     */
    @ApiModelProperty("name")
    private String name;
    /**
     * 忽略类型的ID
     */
    @ApiModelProperty("ignore_type_id")
    private Integer ignoreTypeId;
    /**
     * 状态： 0启用，1不启用
     */
    @ApiModelProperty("status")
    private Integer status;
}
