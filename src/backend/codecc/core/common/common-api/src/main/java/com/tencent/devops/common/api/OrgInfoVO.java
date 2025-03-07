package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgInfoVO {

    @ApiModelProperty
    private Integer bgId;

    @ApiModelProperty("业务线ID")
    private Integer businessLineId;

    @ApiModelProperty
    private Integer deptId;

    @ApiModelProperty
    private Integer centerId;

    @ApiModelProperty
    private Integer groupId;


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

}
