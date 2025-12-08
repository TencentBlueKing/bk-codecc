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
 
package com.tencent.bk.codecc.defect.model.ignore;

import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectSubModel;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

/**
 * 忽略告警模型
 * 
 * @date 2021/6/30
 * @version V1.0
 */
@Data
@Document(collection = "t_ignore_comment_defect_model")
public class IgnoreCommentDefectModel {
    @Field("task_id")
    @Indexed(background = true)
    private Long taskId;

    @Field("ignore_defect_map")
    private Map<String, List<IgnoreCommentDefectSubModel>> ignoreDefectMap;

    @Field("migrated")
    private Boolean migrated;
}
