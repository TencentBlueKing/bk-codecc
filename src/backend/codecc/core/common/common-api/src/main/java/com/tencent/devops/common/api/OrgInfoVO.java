package com.tencent.devops.common.api;

import static com.tencent.devops.common.constant.ComConstants.DEFAULT_BG_ID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class OrgInfoVO {

    @Schema
    private Integer bgId;

    @Schema(description = "业务线ID")
    private Integer businessLineId;

    @Schema
    private Integer deptId;

    @Schema
    private Integer centerId;

    @Schema
    private Integer groupId;

    public OrgInfoVO() {
        this(DEFAULT_BG_ID, DEFAULT_BG_ID, DEFAULT_BG_ID, DEFAULT_BG_ID, DEFAULT_BG_ID);
    }

    public boolean checkIsNotEmpty() {
        boolean hasBgId = bgId != null && bgId > 0;
        boolean hasBusinessLineId = businessLineId != null && businessLineId > 0;
        boolean hasDeptId = deptId != null && deptId > 0;
        boolean hasCenterId = centerId != null && centerId > 0;
        boolean hasGroupId = groupId != null && groupId > 0;
        return hasBgId || hasBusinessLineId || hasDeptId || hasCenterId || hasGroupId;
    }

    public boolean checkIsEmpty() {
        return !checkIsNotEmpty();
    }

    /**
     * 所属的组织架构是否包含传入的组织架构
     *
     * @return
     */
    public boolean contains(OrgInfoVO target) {
        boolean matchBgId = bgId == null || bgId <= 0 || bgId.equals(target.getBgId());
        boolean matchBusinessLineId =
                businessLineId == null || businessLineId <= 0 || businessLineId.equals(target.getBusinessLineId());
        boolean matchDeptId = deptId == null || deptId <= 0 || deptId.equals(target.getDeptId());
        boolean matchCenterId = centerId == null || centerId <= 0 || centerId.equals(target.getCenterId());
        boolean matchGroupId = groupId == null || groupId <= 0 || groupId.equals(target.getGroupId());
        return matchBgId && matchBusinessLineId && matchDeptId && matchCenterId && matchGroupId;
    }

    /**
     *  获取组织架构的匹配度
     * @param target
     * @return
     */
    public int getMatchScore(OrgInfoVO target) {
        if (target == null) {
            return 0;
        }
        int matchBgId = (bgId != null && bgId.equals(target.getBgId())) ? 1 : 0;
        int matchBusinessLineId = (businessLineId != null && businessLineId.equals(target.getBusinessLineId())) ? 1 : 0;
        int matchDeptId = (deptId != null && deptId.equals(target.getDeptId())) ? 1 : 0;
        int matchCenterId = (centerId != null && centerId.equals(target.getCenterId())) ? 1 : 0;
        int matchGroupId = (groupId != null && groupId.equals(target.getGroupId())) ? 1 : 0;
        return matchBgId + matchBusinessLineId + matchDeptId + matchCenterId + matchGroupId;
    }

}
