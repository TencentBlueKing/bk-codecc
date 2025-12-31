package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.ToolMemberInfoVO;
import com.tencent.bk.codecc.defect.vo.developer.OpToolDeveloperInfoReqVO;
import com.tencent.bk.codecc.defect.vo.developer.ToolDeveloperInfoVO;

import java.util.List;

/**
 * 工具开发者服务层
 *
 * @version V1.0
 * @date 2025/8/6
 */
public interface ToolDeveloperInfoService {


    /**
     * 获取工具权限信息
     * @param toolName 工具名称
     * @return ToolDeveloperInfoVO 工具权限信息
     */
    ToolDeveloperInfoVO getPermissionInfo(String toolName);


    /**
     * 同步工具成员
     *
     * @param toolName 工具名称
     * @param toolMemberInfoList 工具成员信息列表
     * @return BOOLEAN
     */
    Boolean syncToolMembers(String toolName, List<ToolMemberInfoVO> toolMemberInfoList);

    /**
     * 初始化工具开发者
     * @return BOOLEAN
     */
    Boolean initializationToolDeveloper();

    /**
     * 更新或插入工具开发者
     * @param adminName 管理员id
     * @param reqVO 请求参数
     * @return BOOLEAN
     */
    Boolean upsertToolDeveloper(String adminName, OpToolDeveloperInfoReqVO reqVO);

    /**
     * 删除工具开发者
     * @param adminName 管理员id
     * @param reqVO 请求参数
     * @return BOOLEAN
     */
    Boolean deleteToolDeveloper(String adminName, OpToolDeveloperInfoReqVO reqVO);
}
