package com.tencent.bk.codecc.defect.pojo.statistic;

import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel;
import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.model.statistic.SCAStatisticEntity;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用于保存SCA工具在统计流程中需要的初始化指标和计算出的中间结果和最终结果
 * 计算完成后通过 builder convert 为表实体类保存
 *
 * @author ruitaoyuan
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SCADefectStatisticModel extends AbstractDefectStatisticModel {

    protected SCADefectStatisticModel() {
    }

    private long newVulCount;

    private long newHighVulCount;

    private long newMediumVulCount;

    private long newLowVulCount;

    private long newUnknownVulCount;

    private long licenseCount;

    private long highLicenseCount;

    private long mediumLicenseCount;

    private long lowLicenseCount;

    private long unknownLicenseCount;

    private long highPackageCount;

    private long mediumPackageCount;

    private long lowPackageCount;

    private long unknownPackageCount;

    private SCASbomAggregateModel aggregateModel;

    private List<SCAVulnerabilityEntity> vulnerabilities;

    private SCAStatisticEntity scaStatisticEntity;

    private SCADefectStatisticModelBuilder builder;

    public void incNewVulCount() {
        newVulCount++;
    }

    public void incNewHighVulCount() {
        newHighVulCount++;
    }

    public void incNewMediumVulCount() {
        newMediumVulCount++;
    }

    public void incNewLowVulCount() {
        newLowVulCount++;
    }

    public void incNewUnknownVulCount() {
        newUnknownVulCount++;
    }

    public void incLicenseCount() {
        licenseCount++;
    }

    public void incHighLicenseCount() {
        highLicenseCount++;
    }

    public void incMediumLicenseCount() {
        mediumLicenseCount++;
    }

    public void incLowLicenseCount() {
        lowLicenseCount++;
    }

    public void incUnknownLicenseCount() {
        unknownLicenseCount++;
    }

    public void incHighPackageCount() {
        highPackageCount++;
    }

    public void incMediumPackageCount() {
        mediumPackageCount++;
    }

    public void incLowPackageCount() {
        lowPackageCount++;
    }

    public void incUnknownPackageCount() {
        unknownPackageCount++;
    }

}
