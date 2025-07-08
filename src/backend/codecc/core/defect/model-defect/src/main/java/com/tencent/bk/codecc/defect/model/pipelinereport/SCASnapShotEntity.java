/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 *  Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.model.pipelinereport;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * SCA工具快照实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SCASnapShotEntity extends ToolSnapShotEntity {

    /**
     * 组件数量
     */
    @Field("package_count")
    @JsonProperty("package_count")
    private Long packageCount;
    /**
     * 高风险组件数量
     */
    @Field("high_package_count")
    @JsonProperty("high_package_count")
    private Long highPackageCount;
    /**
     * 中风险组件数量
     */
    @Field("medium_package_count")
    @JsonProperty("medium_package_count")
    private Long mediumPackageCount;
    /**
     * 低风险组件数量
     */
    @Field("low_package_count")
    @JsonProperty("low_package_count")
    private Long lowPackageCount;
    /**
     * 未知风险组件数量
     */
    @Field("unknown_package_count")
    @JsonProperty("unknown_package_count")
    private Long unknownPackageCount;
    /**
     * 漏洞数量
     */
    @Field("new_vul_count")
    @JsonProperty("new_vul_count")
    private Long newVulCount;

    /**
     * 高风险漏洞数量
     */
    @Field("new_high_vul_count")
    @JsonProperty("new_high_vul_count")
    private Long newHighVulCount;

    /**
     * 中风险漏洞数量
     */
    @Field("new_medium_vul_count")
    @JsonProperty("new_medium_vul_count")
    private Long newMediumVulCount;

    /**
     * 低风险漏洞数量
     */
    @Field("new_low_vul_count")
    @JsonProperty("new_low_vul_count")
    private Long newLowVulCount;

    /**
     * 未知风险漏洞数量
     */
    @Field("new_unknown_vul_count")
    @JsonProperty("new_unknown_vul_count")
    private Long newUnknownVulCount;


    /**
     * 证书数量
     */
    @Field("license_count")
    @JsonProperty("license_count")
    private Long licenseCount;

    /**
     * 高风险证书数量
     */
    @Field("high_license_count")
    @JsonProperty("high_license_count")
    private Long highLicenseCount;

    /**
     * 中风险证书数量
     */
    @Field("medium_license_count")
    @JsonProperty("medium_license_count")
    private Long mediumLicenseCount;

    /**
     * 低风险证书数量
     */
    @Field("low_license_count")
    @JsonProperty("low_license_count")
    private Long lowLicenseCount;

    /**
     * 未知风险证书数量
     */
    @Field("unknown_license_count")
    @JsonProperty("unknown_license_count")
    private Long unknownLicenseCount;
}
