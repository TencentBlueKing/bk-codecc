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

/**
 * 规则集来源
 *
 * @version V1.0
 * @date 2020/1/6
 */
public enum CheckerSetSource {

    DEFAULT("精选", "TOP-RATED"),

    RECOMMEND("推荐", "RECOMMENDED"),

    SELF_DEFINED("自定义", "CUSTOM");

    private String nameCn;
    private String nameEn;

    CheckerSetSource(String nameCn, String nameEn) {
        this.nameCn = nameCn;
        this.nameEn = nameEn;
    }

    public String getNameCn() {
        return this.nameCn;
    }

    public String getNameEn() {
        return this.nameEn;
    }
}
