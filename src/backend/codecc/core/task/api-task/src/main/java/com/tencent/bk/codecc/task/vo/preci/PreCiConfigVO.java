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

package com.tencent.bk.codecc.task.vo.preci;

import lombok.Data;

import java.util.List;

@Data
public class PreCiConfigVO {

    private String entityId;

    private List<Long> langList;

    private Integer isInstall;

    private List<String> projectIdList;

    private List<OrganizationShow> organizationShowList;

    private List<Organization> organizationList;

    private List<String> createFrom;

    private String createdBy;

    private Long createDate;

    private Integer taskCount;

    @Data
    public static class OrganizationShow {
        private String bgName;

        private String deptName;

        private String centerName;
    }

    @Data
    public static class Organization {
        private Integer bg;

        private Integer dept;

        private Integer center;
    }
}
