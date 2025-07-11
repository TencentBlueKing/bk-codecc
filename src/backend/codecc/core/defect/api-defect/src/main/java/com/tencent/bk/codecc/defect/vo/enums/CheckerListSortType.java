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
 * 规则清单排序类型
 * 
 * @date 2020/1/5
 * @version V1.0
 */
public enum CheckerListSortType
{
    checkerKey("checker_key"),
    checkerLanguage("checker_language"),
    checkerCategory("checker_category"),
    toolName("tool_name"),
    checkerTag("checker_tag");

    private String name;

    CheckerListSortType(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }
}
