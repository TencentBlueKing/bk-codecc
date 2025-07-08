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

package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * Platform迁移记录实体类
 *
 * @version V1.0
 * @date 2021/7/16
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_platform_migrate_log")
public class PlatformMigrateLogEntity extends CommonEntity {

    @Field("tool_name")
    private String toolName;

    /**
     * 需要迁移的platform ip
     */
    @Field("source_ip")
    private String sourceIp;

    /**
     * 迁移到目的IP
     */
    @Field("target_ip")
    private String targetIp;

    /**
     * 是否可回退  0-未回退，1-已回退
     */
    @Field
    private Integer status;

    /**
     * 迁移任务数
     */
    @Field("migrate_task_count")
    private Integer migrateTaskCount;

    /**
     * 本次迁移的任务id集合
     */
    @Field("migrate_task_id_set")
    private Set<Long> migratedTaskIdSet;

    /**
     * 备注
     */
    @Field
    private String remarks;

    /**
     * 记录本次迁移信息
     */
    public void migrateLog(String toolName, String sourceIp, String targetIp) {
        this.toolName = toolName;
        this.sourceIp = sourceIp;
        this.targetIp = targetIp;
    }
}
