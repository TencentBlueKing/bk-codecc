package com.tencent.bk.codecc.defect.vo.ignore;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "name")
    private String name;
    /**
     * 忽略类型的ID
     */
    @Schema(description = "ignore_type_id")
    private Integer ignoreTypeId;
    /**
     * 状态： 0启用，1不启用
     */
    @Schema(description = "status")
    private Integer status;
}
