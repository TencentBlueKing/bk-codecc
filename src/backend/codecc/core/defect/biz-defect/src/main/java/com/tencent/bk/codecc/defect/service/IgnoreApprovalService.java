package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.ignore.IgnoreApprovalConfigVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreApprovalVO;
import com.tencent.devops.common.api.pojo.Page;

import java.util.List;

/**
 * 告警忽略审批业务接口
 */
public interface IgnoreApprovalService {

    /**
     * 新增或者更新配置
     *
     * @param projectId
     * @param userName
     * @param ignoreApprovalConfigVO
     * @return
     */
    boolean savaApprovalConfig(String projectId, String userName, IgnoreApprovalConfigVO ignoreApprovalConfigVO);


    /**
     * 获取配置列表
     *
     * @param projectId
     * @param userName
     * @return
     */
    Page<IgnoreApprovalConfigVO> projectConfigList(String projectId, String userName, Integer pageNum,
            Integer pageSize);


    /**
     * 获取配置详情
     *
     * @param projectId
     * @param userName
     * @param ignoreApprovalConfigId
     * @return
     */
    IgnoreApprovalConfigVO approvalConfigDetail(String projectId, String userName, String ignoreApprovalConfigId);

    /**
     * 获取配置详情
     *
     * @param projectId
     * @param userName
     * @param ignoreApprovalConfigId
     * @return
     */
    boolean approvalConfigDelete(String projectId, String userName, String ignoreApprovalConfigId);


    /**
     * 通过ID获取审批详情
     *
     * @param ignoreApprovalIds
     * @return
     */
    List<IgnoreApprovalVO> getApprovalListByIds(List<String> ignoreApprovalIds);


    /**
     * 通过ID获取审批详情
     *
     * @param ignoreApprovalId
     * @return
     */
    IgnoreApprovalVO getApprovalById(String ignoreApprovalId);


    /**
     * 获取符合条件的配置列表
     *
     * @param projectId
     * @param ignoreTypeId
     * @param dimensions
     * @param severities
     * @return
     */
    List<IgnoreApprovalConfigVO> getProjectMatchConfig(String projectId, Integer ignoreTypeId, List<String> dimensions,
            List<Integer> severities);


    /**
     *  回调更新状态
     * @param approvalId
     * @param status
     * @param sn
     * @param url
     * @param username
     */
    void updateApprovalAndDefectWhenCallback(String approvalId, Integer status, String sn, String url, String username);


    /**
     * op分页获取忽略审批配置
     *
     * @param taskScopeType 任务范围类型
     * @param projectId 项目编号
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param sortField 排序字段
     * @param sortType 排序类型
     * @return page
     */
    Page<IgnoreApprovalConfigVO> getIgnoreApprovalConfigList(String taskScopeType, String projectId, Integer pageNum,
                                                                 Integer pageSize, String sortField, String sortType);

    /**
     * 插入/更新忽略审批配置
     *
     * @param reqVO 请求体
     * @return boolean
     */
    Boolean upsertIgnoreApprovalConfig(IgnoreApprovalConfigVO reqVO);

    /**
     * 删除忽略审批配置
     * @param entityId 实体id
     * @return boolean
     */
    Boolean deleteIgnoreApprovalConfig(String entityId, String userId);
}
