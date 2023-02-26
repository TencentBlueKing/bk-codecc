package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeDefectStatResponse;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeSysVO;
import java.util.List;
import java.util.Set;

/**
 * 告警忽略类型业务接口
 */
public interface IIgnoreTypeService {

    Boolean ignoreTypeSysUpdate(String userName, IgnoreTypeSysVO reqVO);

    List<IgnoreTypeSysVO> queryIgnoreTypeSysList();

    Boolean ignoreTypeProjectSave(String projectId, String userName, IgnoreTypeProjectConfigVO projectConfig);

    List<IgnoreTypeProjectConfigVO> queryIgnoreTypeProjectList(String projectId, String userName);

    IgnoreTypeProjectConfigVO ignoreTypeProjectDetail(String projectId, String userName, Integer ignoreTypeId);

    Boolean updateIgnoreTypeProjectStatus(String projectId, String userName, IgnoreTypeProjectConfigVO projectConfig);


    void triggerProjectStatisticAndSend(String projectId, String ignoreTypeName,
            Integer ignoreTypeId, String createFrom);

    List<IgnoreTypeDefectStatResponse> getIgnoreTypeDefectStat(String projectId, String userName,
            Set<Integer> ignoreTypeIds);

}
