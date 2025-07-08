/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
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
 * 规则集类型枚举
 *
 * @version V1.0
 * @date 2020/1/6
 */
public enum CheckerSetCategory {

    OPENSOURCE("开源", "CHECKER_SET_CATEGORY_OPENSOURCE"),

    DEFECT("通用", "CHECKER_SET_CATEGORY_DEFECT"),

    SECURITY("安全", "CHECKER_SET_CATEGORY_SECURITY"),

    FORMAT("规范", "CHECKER_SET_CATEGORY_FORMAT");

    private String name;

    private String i18nResourceCode;

    CheckerSetCategory(String name, String i18nResourceCode) {
        this.name = name;
        this.i18nResourceCode = i18nResourceCode;
    }

    /**
     * 原存在的中文名，后新增字段i18nResourceCode作中英表达
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    public String getI18nResourceCode() {
        return this.i18nResourceCode;
    }


    public static CheckerSetCategory getByName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        for (CheckerSetCategory value : CheckerSetCategory.values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return null;
    }
}
