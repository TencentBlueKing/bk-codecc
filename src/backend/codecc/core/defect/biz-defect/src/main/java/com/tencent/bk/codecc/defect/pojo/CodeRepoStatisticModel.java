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

package com.tencent.bk.codecc.defect.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * 代码仓库总表配置
 *
 * @version V1.0
 * @date 2021/5/31
 */
@Data
public class CodeRepoStatisticModel {
    /**
     * 来源
     */
    @JsonProperty("data_from")
    private String dataFrom;

    /**
     * 代码仓库地址
     */
    @JsonProperty("url")
    private String url;

    /**
     * 分支名称
     */
    @JsonProperty("branch")
    private String branch;

    /**
     * 代码库创建时间
     */
    @JsonProperty("url_first_scan")
    private Long urlFirstScan;

    /**
     * 分支创建日期
     */
    @JsonProperty("branch_first_scan")
    private Long branchFirstScan;

    /**
     * 分支最近修改日期
     */
    @JsonProperty("branch_last_scan")
    private Long branchLastScan;

    /**
     * 关联的任务id集合
     */
    @JsonProperty("task_id_set")
    private Set<Long> taskIdSet;

    /**
     * 代码库数量
     */
    @Field("url_count")
    private Integer urlCount;
}