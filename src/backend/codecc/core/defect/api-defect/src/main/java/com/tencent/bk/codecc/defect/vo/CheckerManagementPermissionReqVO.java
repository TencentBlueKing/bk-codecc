package com.tencent.bk.codecc.defect.vo;

import lombok.Data;


/**
 * 规则管理权限
 *
 * @version V1.0
 * @date 2024/9/11
 */
@Data
public class CheckerManagementPermissionReqVO {
    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 用户名
     */
    private String userId;

    /**
     * 工具名
     */
    private String toolName;


    /**
     * 规则key
     */
    private String checkerKey;

}
