package com.tencent.codecc.common.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgInfoEntity {

    @Field("bg_id")
    private Integer bgId;

    @Field("business_line_id")
    private Integer businessLineId;

    @Field("dept_id")
    private Integer deptId;


    @Field("center_id")
    private Integer centerId;

    @Field("group_id")
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
    public boolean contains(OrgInfoEntity target) {
        boolean matchBgId = bgId == null || bgId == 0 || bgId.equals(target.getBgId());
        boolean matchBusinessLineId =
                businessLineId == null || businessLineId == 0 || businessLineId.equals(target.getBusinessLineId());
        boolean matchDeptId = deptId == null || deptId == 0 || deptId.equals(target.getDeptId());
        boolean matchCenterId = centerId == null || centerId == 0 || centerId.equals(target.getCenterId());
        boolean matchGroupId = groupId == null || groupId == 0 || groupId.equals(target.getGroupId());
        return matchBgId && matchBusinessLineId && matchDeptId && matchCenterId && matchGroupId;
    }

}
