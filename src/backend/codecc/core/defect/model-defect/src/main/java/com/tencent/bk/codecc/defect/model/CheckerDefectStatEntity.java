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

package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 规则告警统计
 *
 * @version V1.0
 * @date 2020/11/13
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_checker_defect_stat")
@CompoundIndexes({
        @CompoundIndex(name = "toolname_1_datafrom_1_statdate_1",
                def = "{'tool_name': 1, 'data_from': 1, 'stat_date': 1}", background = true)
})
public class CheckerDefectStatEntity extends CommonEntity {

    @Field("tool_name")
    @Indexed
    private String toolName;

    @Field("checker_name")
    private String checkerName;

    @Field("open_checker_task_count")
    private int openCheckerTaskCount;

    @Field("defect_total_count")
    private int defectTotalCount;

    @Field("exist_count")
    private int existCount;

    @Field("fixed_count")
    private int fixedCount;

    @Field("ignore_count")
    private int ignoreCount;

    @Field("excluded_count")
    private int excludedCount;

    @Field("checker_create_date")
    private Long checkerCreatedDate;

    /**
     * 数据来源按开源、非开源的维度统计 enum DefectStatType
     */
    @Field("data_from")
    @Indexed
    private String dataFrom;

    /**
     * 统计时间
     */
    @Field("stat_date")
    @Indexed(background = true)
    private Long statDate;

}
