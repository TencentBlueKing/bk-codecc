/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.model.defect;

import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 可跟踪告警的实体
 *
 * @version V1.0
 * @date 2019/10/20
 */
@Data
@Document(collection = "t_defect")
@EqualsAndHashCode(callSuper = true)
@CompoundIndexes({
        @CompoundIndex(name = "taskid_tool_status_idx", def = "{'task_id': 1, 'tool_name': 1, 'status': 1}"),
        @CompoundIndex(name = "taskid_tool_id_idx", def = "{'task_id': 1, 'tool_name': 1, 'id': 1}"),
        @CompoundIndex(name = "taskid_tool_checker_idx", def = "{'task_id': 1, 'tool_name': 1, 'checker_name': 1, 'author_list': 1}")
})
public class CommonDefectEntity extends DefectEntity {

    /**
     * 告警描述
     */
    protected String message;

    /**
     * 规则类型，对应Coverity Platform中的Category(类别)
     */
    @Field("display_category")
    protected String displayCategory;
    /**
     * 类型子类，对应Coverity Platform中的Type(类型)
     */
    @Field("display_type")
    protected String displayType;
    /**
     * 告警行号
     */
    @Field("line_number")
    protected int lineNum;
    /**
     * 告警的唯一标志
     */
    @Field("id")
    private String id;
    /**
     * 流名称
     */
    @Field("stream_name")
    private String streamName;
    @Field("tool_name")
    private String toolName;
    /**
     * 规则名称
     */
    @Field("checker_name")
    private String checker;
    /**
     * 告警所在文件
     */
    @Field("file_path_name")
    private String filePath;

    /**
     * 告警所在文件名
     */
    @Field("file_name")
    private String fileName;


    @Field("file_md5")
    private String fileMd5;
    /**
     * 告警处理人
     */
    @Field("author_list")
    private Set<String> authorList;
    /**
     * 告警严重程度
     */
    @Field("severity")
    private int severity;
    /**
     * 忽略告警原因类型
     */
    @Field("ignore_reason_type")
    private int ignoreReasonType;

    /**
     * 忽略告警具体原因
     */
    @Field("ignore_reason")
    private String ignoreReason;

    /**
     * 忽略告警的作者
     */
    @Field("ignore_author")
    private String ignoreAuthor;

    /**
     * 告警创建时间
     */
    @Field("create_time")
    private long createTime;

    /**
     * 告警修复时间
     */
    @Field("fixed_time")
    private long fixedTime;

    /**
     * 告警忽略时间
     */
    @Field("ignore_time")
    private long ignoreTime;

    /**
     * 告警屏蔽时间
     */
    @Field("exclude_time")
    private long excludeTime;

    /**
     * 记录告警具体被哪个路径屏蔽
     */
    @Field("mask_path")
    private String maskPath;

    /**
     * 告警是否被标记为已修改的标志，checkbox for developer, 0 is normal, 1 is tag, 2 is prompt
     */
    @Field("mark")
    private Integer mark;

    /**
     * 告警被标记为已修改的时间
     */
    @Field("mark_time")
    private Long markTime;

    /**
     * 标记了，但是再次扫描没有修复
     */
    @Field("mark_but_no_fixed")
    private Boolean markButNoFixed;

    /**
     * 创建时的构建号
     */
    @Field("create_build_number")
    private String createBuildNumber;

    /**
     * 修复时的构建号
     */
    @Field("fixed_build_number")
    private String fixedBuildNumber;

    /**
     * 文件对应仓库版本号
     */
    @Field("file_version")
    private String fileVersion;

    /**
     * 对应第三方缺陷管理系统的ID，这里声明为字符串可以有更好的兼容性
     */
    @Field("ext_bug_id")
    private String extBugid;

    /**
     * 第三方平台的buildId
     */
    @Field("platform_build_id")
    private String platformBuildId;

    /**
     * 第三方平台的项目ID
     */
    @Field("platform_project_id")
    private String platformProjectId;

    @Field("defect_instances")
    private List<DefectInstance> defectInstances;

    @Field("rel_path")
    private String relPath;

    /**
     * 版本号
     */
    @Field("revision")
    private String revision;

    /**
     * 构建编号
     */
    @Transient
    private String buildId;

    /**
     * 告警类型：新告警NEW(1)，历史告警HISTORY(2)
     */
    @Transient
    private Integer defectType;

    @Transient
    private Long lineUpdateTime;
}
