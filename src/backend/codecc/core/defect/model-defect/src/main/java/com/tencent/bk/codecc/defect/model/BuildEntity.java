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

package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 构建信息表
 *
 * @version V1.0
 * @date 2019/5/10
 */
@Data
@Document(collection = "t_build")
@CompoundIndex(name = "build_id_1_task_id_1",  def = "{'build_id': 1, 'task_id': 1}", background = true)
public class BuildEntity {
    @Id
    private String entityId;

    @Field("task_id")
    private Long taskId;

    @Field("build_id")
    private String buildId;

    @Field("build_num")
    private String buildNo;

    @Field("build_time")
    private long buildTime;

    @Field("build_user")
    private String buildUser;

    @Field("white_paths")
    private List<String> whitePaths;

    /**
     * 保存当次构建各工具使用的规则集及版本
     */
    @Field("checker_sets_version")
    private List<CheckerSetsVersionInfoEntity> checkerSetsVersion;
    /**
     * 是否重新分配处理人
     */
    @Field("reallocate")
    private Boolean reallocate;
}
