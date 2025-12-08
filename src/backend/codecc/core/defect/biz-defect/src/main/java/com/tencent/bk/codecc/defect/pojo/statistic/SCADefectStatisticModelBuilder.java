package com.tencent.bk.codecc.defect.pojo.statistic;

import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel;
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity;
import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.model.statistic.SCAStatisticEntity;
import com.tencent.devops.common.constant.ComConstants;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

/**
 * 告警统计模版类的抽象构造器
 * 用于构建告警统计所用的重复率类model类
 *
 */
public class SCADefectStatisticModelBuilder extends
        AbstractDefectStatisticModelBuilder<SCADefectStatisticModel, SCAStatisticEntity, SCAVulnerabilityEntity> {

    {
        defectStatisticModel = new SCADefectStatisticModel();
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @return 统计数据记录实体类
     */
    @Override
    public SCADefectStatisticModel build() {
        defectStatisticModel.setBuilder(this);
        return defectStatisticModel;
    }

    /**
     * 根据现有数据，初始化统计数据记录实体类
     *
     * @return 告警统计持久化类
     */
    @Override
    public SCAStatisticEntity convert(SCAStatisticEntity scaStatisticEntity) {
        if (scaStatisticEntity == null) {
            scaStatisticEntity = new SCAStatisticEntity();
        }
        scaStatisticEntity.setTaskId(defectStatisticModel.getTaskId());
        scaStatisticEntity.setBuildId(defectStatisticModel.getBuildId());
        scaStatisticEntity.setTime(System.currentTimeMillis());
        scaStatisticEntity.setToolName(defectStatisticModel.getToolName());
        scaStatisticEntity.setDefectCount((int) defectStatisticModel.getNewVulCount());
        scaStatisticEntity.setPackageCount(defectStatisticModel.getAggregateModel().getPackages().size());
        scaStatisticEntity.setHighPackageCount(defectStatisticModel.getHighPackageCount());
        scaStatisticEntity.setMediumPackageCount(defectStatisticModel.getMediumPackageCount());
        scaStatisticEntity.setLowPackageCount(defectStatisticModel.getLowPackageCount());
        scaStatisticEntity.setUnknownPackageCount(defectStatisticModel.getUnknownPackageCount());
        scaStatisticEntity.setNewVulCount(defectStatisticModel.getNewVulCount());
        scaStatisticEntity.setNewHighVulCount(defectStatisticModel.getNewHighVulCount());
        scaStatisticEntity.setNewMediumVulCount(defectStatisticModel.getNewMediumVulCount());
        scaStatisticEntity.setNewLowVulCount(defectStatisticModel.getNewLowVulCount());
        scaStatisticEntity.setNewUnknownVulCount(defectStatisticModel.getNewUnknownVulCount());
        scaStatisticEntity.setLicenseCount(defectStatisticModel.getLicenseCount());
        scaStatisticEntity.setHighLicenseCount(defectStatisticModel.getHighLicenseCount());
        scaStatisticEntity.setMediumLicenseCount(defectStatisticModel.getMediumLicenseCount());
        scaStatisticEntity.setLowLicenseCount(defectStatisticModel.getLowLicenseCount());
        scaStatisticEntity.setUnknownLicenseCount(defectStatisticModel.getUnknownLicenseCount());

        return scaStatisticEntity;
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @param defectList 告警列表
     * @return 统计数据记录实体类
     */
    @Override
    public AbstractDefectStatisticModelBuilder<SCADefectStatisticModel, SCAStatisticEntity,
            SCAVulnerabilityEntity> allDefectList(List<SCAVulnerabilityEntity> defectList) {
        defectStatisticModel.setVulnerabilities(defectList);
        return this;
    }


    /**
     * 设置SBOM聚合模型并统计各严重等级的组件包数量
     *
     * @param aggregateModel SBOM组件包聚合模型（包含所有扫描到的组件包信息）
     * @return 当前构建器实例（支持链式调用）
     */
    public SCADefectStatisticModelBuilder sbomAggregateModel(SCASbomAggregateModel aggregateModel) {
        defectStatisticModel.setAggregateModel(aggregateModel);
        // 遍历所有组件包进行严重等级统计
        if (CollectionUtils.isNotEmpty(aggregateModel.getPackages())) {
            for (SCASbomPackageEntity sbomPackage : aggregateModel.getPackages()) {
                // 根据组件包的严重等级递增对应计数器
                if (sbomPackage.getSeverity() == ComConstants.SERIOUS) {
                    defectStatisticModel.incHighPackageCount();
                } else if (sbomPackage.getSeverity() == ComConstants.NORMAL) {
                    defectStatisticModel.incMediumPackageCount();
                } else if (sbomPackage.getSeverity() == ComConstants.PROMPT
                        || sbomPackage.getSeverity() == ComConstants.PROMPT_IN_DB) {
                    // PROMPT和PROMPT_IN_DB都归类为低风险
                    defectStatisticModel.incLowPackageCount();
                } else {
                    // 未知风险等级的组件包统计
                    defectStatisticModel.incUnknownPackageCount();
                }
            }
        }
        // 遍历所有证书进行严重等级统计
        if (CollectionUtils.isNotEmpty(aggregateModel.getLicenses())) {
            for (SCALicenseEntity license : aggregateModel.getLicenses()) {
                defectStatisticModel.incLicenseCount();
                if (license.getSeverity() == ComConstants.SERIOUS) {
                    defectStatisticModel.incHighLicenseCount();
                } else if (license.getSeverity() == ComConstants.NORMAL) {
                    defectStatisticModel.incMediumLicenseCount();
                } else if (license.getSeverity() == ComConstants.PROMPT
                        || license.getSeverity() == ComConstants.PROMPT_IN_DB) {
                    defectStatisticModel.incLowLicenseCount();
                } else {
                    defectStatisticModel.incUnknownLicenseCount();
                }
            }
        }
        return this;
    }
}
