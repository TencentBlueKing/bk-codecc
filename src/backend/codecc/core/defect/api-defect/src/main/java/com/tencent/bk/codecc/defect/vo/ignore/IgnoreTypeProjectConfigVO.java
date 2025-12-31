package com.tencent.bk.codecc.defect.vo.ignore;

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.annotation.I18NFieldMarker;
import com.tencent.devops.common.api.annotation.I18NModuleCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class IgnoreTypeProjectConfigVO extends CommonVO {

    /**
     * 名字
     */
    @I18NFieldMarker(keyFieldHolder = "entityId", moduleCode = I18NModuleCode.IGNORE_TYPE_SYS_NAME)
    @Schema(description = "name")
    private String name;
    /**
     * 忽略类型的ID
     */
    @Schema(description = "ignore_type_id")
    private Integer ignoreTypeId;
    /**
     * 创建来源 codecc ， project
     */
    @Schema(description = "create_from")
    private String createFrom;
    /**
     * 创建来源为 project 时,有值
     */
    @Schema(description = "project_id")
    private String projectId;
    /**
     * 状态:
     * 0 启用
     * 1 不启用
     * 2 后台启用 (该忽略类型不能被用户选中, 只能由后台直接赋值)
     */
    @Schema(description = "status")
    private Integer status;
    /**
     * 通知启用状态： 0启用，1不启用
     */
    @Schema(description = "notify_status")
    private Integer notifyStatus;
    /**
     * 通知相关配置
     */
    @Schema(description = "notify")
    private IgnoreTypeNotifyVO notify;
    /**
     * 下一次通知的日期
     */
    @Schema(description = "nextNotifyTime")
    private Long nextNotifyTime;

    /**
     * 是否可以编辑
     */
    @Schema(description = "edit")
    private Boolean edit;

}
