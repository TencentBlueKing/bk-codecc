package com.tencent.bk.codecc.defect.model.sca

class SCASbomAggregateModel(
    /**
     * 任务ID
     */
    var taskId: Long,
    /**
     * 工具名称
     */
    var toolName: String,
    /**
     * 组件信息
     */
    var packages: List<SCASbomPackageEntity>,
    /**
     * 漏洞信息
     */
    var vulnerabilities: List<SCAVulnerabilityEntity>,
    /**
     * 许可证信息
     */
    var licenses: List<SCALicenseEntity>,
)
