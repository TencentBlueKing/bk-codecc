package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;

import java.util.List;
import java.util.Set;

public interface CommonFilterCheckerService {
    /**
     * 过滤项目不可见规则:
     * 根据projectId查询项目关联可见的用户自定义规则列表
     * 筛选查询结果中的规则是否在项目可见的用户自定义规则列表中，如果不包含则进行过滤
     *
     * @param checkerDetailEntityList
     * @param projectId
     * @return
     */
    public List<CheckerDetailEntity> filterInvisibleChecker(
        List<CheckerDetailEntity> checkerDetailEntityList,
        String projectId);
}
