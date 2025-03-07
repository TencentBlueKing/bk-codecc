/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.vo.enums;

import org.apache.commons.lang.StringUtils;

/**
 * 规则类型
 *
 * @version V1.0
 * @date 2019/12/26
 */
public enum CheckerCategory {

    /**
     * 代码缺陷
     */
    CODE_DEFECT("代码缺陷", "Code Defects"),

    /**
     * 安全漏洞
     */
    SECURITY_RISK("安全漏洞", "Security Vulnerabilities"),

    /**
     * 代码规范
     */
    CODE_FORMAT("代码规范", "Code Style Issues"),

    /**
     * 圈复杂度
     */
    COMPLEXITY("圈复杂度", "CCN"),

    /**
     * 重复率
     */
    DUPLICATE("重复率", "Duplication Rate"),

    /**
     * 通用
     */
    CODE_STATISTIC("代码统计", "Code Metrics"),

    /**
     * 软件成分
     */
    SOFTWARE_COMPOSITION("软件成分", "Software Composition");

    private String name;

    private String enName;

    CheckerCategory(String name, String enName) {
        this.name = name;
        this.enName = enName;
    }

    public String getName() {
        return this.name;
    }

    public String getEnName() {
        return this.enName;
    }

    public static CheckerCategory getByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        for (CheckerCategory value : CheckerCategory.values()) {
            if (value.name().equals(name) || value.name.equals(name)) {
                return value;
            }
        }
        return null;
    }
}
