package com.tencent.bk.codecc.defect.vo.ignore;

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.annotation.I18NFieldMarker;
import com.tencent.devops.common.api.annotation.I18NModuleCode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class IgnoreTypeProjectConfigVO extends CommonVO {

    /**
     * 名字
     */
    @I18NFieldMarker(keyFieldHolder = "entityId", moduleCode = I18NModuleCode.IGNORE_TYPE_SYS_NAME)
    @ApiModelProperty("name")
    private String name;
    /**
     * 忽略类型的ID
     */
    @ApiModelProperty("ignore_type_id")
    private Integer ignoreTypeId;
    /**
     * 创建来源 codecc ， project
     */
    @ApiModelProperty("create_from")
    private String createFrom;
    /**
     * 创建来源为 project 时,有值
     */
    @ApiModelProperty("project_id")
    private String projectId;
    /**
     * 状态： 0启用，1不启用
     */
    @ApiModelProperty("status")
    private Integer status;
    /**
     * 通知启用状态： 0启用，1不启用
     */
    @ApiModelProperty("notify_status")
    private Integer notifyStatus;
    /**
     * 通知相关配置
     */
    @ApiModelProperty("notify")
    private IgnoreTypeNotifyVO notify;
    /**
     * 下一次通知的日期
     */
    @ApiModelProperty("nextNotifyTime")
    private Long nextNotifyTime;

    /**
     * 是否可以编辑
     */
    @ApiModelProperty("edit")
    private Boolean edit;

}
