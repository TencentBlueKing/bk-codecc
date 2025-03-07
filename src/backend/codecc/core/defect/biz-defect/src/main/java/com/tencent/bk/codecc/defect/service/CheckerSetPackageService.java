package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetPackageEntity;
import com.tencent.bk.codecc.task.vo.checkerset.OpenSourceCheckerSetVO;
import com.tencent.codecc.common.db.OrgInfoEntity;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import java.util.List;
import java.util.Set;

public interface CheckerSetPackageService {

    boolean updateByLang(OpenSourceCheckerSetVO reqVO, String userName);

    boolean deleteByLang(OpenSourceCheckerSetVO reqVO, String userName);

    List<CheckerSetPackageEntity> getByLangValueAndTypeAndEnvTypeAndOrgInfoAndCreateFrom(Long langValue, String type,
            String envType, OrgInfoEntity orgInfo, BsTaskCreateFrom createFrom);

    List<CheckerSetPackageEntity> getByTypeAndEnvTypeAndOrgInfoAndCreateFrom(String type, String envType,
            OrgInfoEntity orgInfo, BsTaskCreateFrom createFrom);

    List<CheckerSetPackageEntity> getByLangValue(Long langValue);

    Set<String> updateLangPreProdConfig(List<CheckerSetVO> checkerSetVOS);

}
