package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildLicenseRestResource;
import com.tencent.bk.codecc.defect.model.sca.LicenseDetailEntity;
import com.tencent.bk.codecc.defect.service.sca.SCALicenseService;
import com.tencent.bk.codecc.defect.vo.sca.BatchUploadLicenseVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestResource
public class BuildLicenseRestResourceImpl implements BuildLicenseRestResource {

    @Autowired
    private SCALicenseService scaLicenseService;

    @Override
    public Result<Boolean> uploadLicense(List<BatchUploadLicenseVO> uploadLicenseVOs) {
        if (CollectionUtils.isEmpty(uploadLicenseVOs)) {
            log.error("uploadLicense is empty");
            return new Result<>(false);
        }
        try {
            List<LicenseDetailEntity> licenseDetails = uploadLicenseVOs.stream().map(it -> {
                LicenseDetailEntity licenseDetail = new LicenseDetailEntity();
                BeanUtils.copyProperties(it, licenseDetail);
                return licenseDetail;
            }).collect(Collectors.toList());
            scaLicenseService.uploadLicenses(licenseDetails);
        } catch (Exception e) {
            log.error("uploadLicense cause error.", e);
        }
        return new Result<>(true);
    }
}
