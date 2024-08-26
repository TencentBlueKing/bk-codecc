/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.quartz.model;

import com.tencent.codecc.common.db.CommonEntity;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 分析版本持久实体类
 *
 * @version V1.0
 * @date 2019/7/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "t_job_instance")
@CompoundIndexes({
        @CompoundIndex(name = "class_name_1_shard_tag_1_next_trigger_time_1",
                def = "{'class_name': 1, 'shard_tag': 1, 'next_trigger_time': 1}", background = true)
})
public class JobInstanceEntity extends CommonEntity {

    /**
     * 用于远程加载类
     */
    @Field("class_url")
    private String classUrl;

    /**
     * job对应实现类名字，也是容器中bean的名字
     */
    @Field("class_name")
    private String className;

    /**
     * job名字
     */
    @Indexed
    @Field("job_name")
    private String jobName;

    /**
     * 触发器名字
     */
    @Field("trigger_name")
    private String triggerName;


    /**
     * 定时表达式
     */
    @Field("cron_expression")
    private String cronExpression;

    /**
     * job自定义参数
     */
    @Field("job_param")
    private Map<String, Object> jobParam;

    /**
     * job所属分片数
     */
    @Field("shard_tag")
    private String shardTag;

    /**
     * job状态, 1无效，0有效
     */
    @Field("status")
    private Integer status;

    /**
     * 最后一次执行的时间
     */
    @Field("last_trigger_time")
    private Long lastTriggerTime;

    /**
     * 下次执行时间
     */
    @Field("next_trigger_time")
    private Long nextTriggerTime;
}
