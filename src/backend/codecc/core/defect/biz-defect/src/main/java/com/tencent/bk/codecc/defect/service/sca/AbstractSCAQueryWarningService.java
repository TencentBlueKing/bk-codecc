package com.tencent.bk.codecc.defect.service.sca;

import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulnerabilityRating;
import com.tencent.bk.codecc.defect.service.ISCAQueryWarningService;
import com.tencent.bk.codecc.defect.vo.sca.SCAVulAffectPackageVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAVulAffectedPackageVersionVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAVulnerabilityDetailVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAVulnerabilityRatingVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAVulnerabilitySourceVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAVulnerabilityVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.BeanUtils;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractSCAQueryWarningService implements ISCAQueryWarningService {

    protected SCAVulnerabilityVO getVulVOFromEntity(SCAVulnerabilityEntity entity) {
        SCAVulnerabilityVO vo = new SCAVulnerabilityVO();
        covertVulEntityToVO(entity, vo);
        return vo;
    }

    protected void covertVulEntityToVO(SCAVulnerabilityEntity entity, SCAVulnerabilityVO vo) {
        if (entity == null || vo == null) {
            return;
        }
        BeanUtils.copyProperties(entity, vo);
        vo.setSeverity(vo.getSeverity() == ComConstants.PROMPT_IN_DB ? ComConstants.PROMPT : vo.getSeverity());
        if (entity.getCvssV2() != null) {
            SCAVulnerabilityRating cvssV2 = entity.getCvssV2();
            vo.setCvssV2(new SCAVulnerabilityRatingVO(cvssV2.getVector(), cvssV2.getScore(), cvssV2.getMethod()));
        }
        if (entity.getCvssV3() != null) {
            SCAVulnerabilityRating cvssV3 = entity.getCvssV3();
            vo.setCvssV3(new SCAVulnerabilityRatingVO(cvssV3.getVector(), cvssV3.getScore(), cvssV3.getMethod()));
        }
    }

    protected SCAVulnerabilityDetailVO getVulDetailVOFromEntity(SCAVulnerabilityEntity entity) {
        SCAVulnerabilityDetailVO vo = new SCAVulnerabilityDetailVO();
        covertVulEntityToDetailVO(entity, vo);
        return vo;
    }

    protected void covertVulEntityToDetailVO(SCAVulnerabilityEntity entity, SCAVulnerabilityDetailVO vo) {
        if (entity == null || vo == null) {
            return;
        }
        covertVulEntityToVO(entity, vo);
        // 转换影响包
        if (CollectionUtils.isNotEmpty(entity.getAffectedPackages())) {
            vo.setAffectedPackages(entity.getAffectedPackages().stream().filter(Objects::nonNull).map(aPackage -> {
                List<SCAVulAffectedPackageVersionVO> versions = CollectionUtils.isEmpty(aPackage.getVersions())
                        ? Collections.emptyList() : aPackage.getVersions().stream().filter(Objects::nonNull)
                        .map(version -> new SCAVulAffectedPackageVersionVO(version.getVersion(), version.getRange()))
                        .collect(Collectors.toList());
                return new SCAVulAffectPackageVO(aPackage.getPackageName(), versions, aPackage.getFixAdvice());
            }).collect(Collectors.toList()));
        }
        // 转换源信息
        if (CollectionUtils.isNotEmpty(entity.getSource())) {
            vo.setSource(entity.getSource().stream().filter(Objects::nonNull).map(source -> {
                return new SCAVulnerabilitySourceVO(source.getUrl(), source.getName());
            }).collect(Collectors.toList()));
        }
        // 转换源信息
        if (CollectionUtils.isNotEmpty(entity.getRatings())) {
            vo.setRatings(entity.getRatings().stream().filter(Objects::nonNull).map(rating -> {
                return new SCAVulnerabilityRatingVO(rating.getVector(), rating.getScore(), rating.getMethod());
            }).collect(Collectors.toList()));
        }
    }

}
