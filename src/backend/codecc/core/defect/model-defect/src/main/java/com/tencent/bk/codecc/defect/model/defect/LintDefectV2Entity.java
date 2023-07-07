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

package com.tencent.bk.codecc.defect.model.defect;

import com.alibaba.fastjson.annotation.JSONField;
import com.tencent.bk.codecc.defect.model.CodeCommentEntity;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

/**
 * lint类告警持久实体类
 * 变更此Entity后，需要注意：
 * 1、执行代码生成器以生成新的实体跟踪代码，请查看模块：common:common-code-generator
 * 2、数据迁移字段映射更新(common to lint), com.tencent.bk.codecc.defect.component.DefectConverter
 *
 * @version V1.0
 * @date 2019/5/9
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_lint_defect_v2")
@CompoundIndexes({
        @CompoundIndex(name = "idx_sort_by_filename", def = "{'task_id': 1, 'tool_name': 1, 'status': 1, 'file_name': 1, 'line_num': 1}", background = true),
        @CompoundIndex(name = "idx_sort_by_severity", def = "{'task_id': 1, 'tool_name': 1, 'status': 1, 'severity': 1}", background = true),
        @CompoundIndex(name = "idx_sort_by_create_build_number", def = "{'task_id': 1, 'tool_name': 1, 'status': 1, 'create_build_number': 1}", background = true),
        @CompoundIndex(name = "idx_sort_by_line_update_time", def = "{'task_id': 1, 'tool_name': 1, 'status': 1, 'line_update_time': -1}", background = true),
        @CompoundIndex(name = "idx_taskid_1_toolname_1_status_1_checker_1", def = "{'task_id': 1, 'tool_name': 1, 'status': 1, 'checker': 1}", background = true),
        @CompoundIndex(name = "idx_taskid_1_toolname_1_status_1_filepath_1", def = "{'task_id': 1, 'tool_name': 1, 'status': 1, 'file_path': 1}", background = true),
        @CompoundIndex(name = "idx_taskid_1_toolname_1_status_1_author_1_severity_1", def = "{'task_id': 1, 'tool_name': 1, 'status': 1, 'author': 1, 'severity': 1}", background = true),
        @CompoundIndex(name = "idx_taskid_1_toolname_1_status_1_ignore_reason_type_1",
                def = "{'task_id': 1, 'tool_name': 1, 'status': 1, 'ignore_reason_type': 1}", background = true),
        @CompoundIndex(
                name = "idx_task_id_1_tool_name_1_status_1_checker_1_file_name_1_line_num_1",
                def = "{'task_id': 1, 'tool_name': 1, 'status': 1, 'checker': 1, 'file_name': 1, 'line_num': 1}",
                background = true
        )
})
@Sharded(shardKey = "task_id")
public class LintDefectV2Entity extends CommonSpecialEntity {

    @Field("id")
    private String id;

    @Field("tool_name")
    private String toolName;

    /**
     * 文件名(不包含路径)
     */
    @Field("file_name")
    private String fileName;

    /**
     * 告警行号
     */
    @Field("line_num")
    @JSONField(name = "line")
    private int lineNum;

    /**
     * 告警作者
     * 注：
     * 1、数据库lint表中author字段会同时存在String、List两种数据类型
     * 2、谨慎留意当前模块是否有用第三方库重写过mongoConverter， mongodb原生驱动提供的mongoConverter是支持双向兼容，如下：
     * database  |    java    |   result
     * List     |   String   |   "author1,author2"
     * String   |   List     |   ["author1"]
     * 3、java层面Lint实体原类型是String，现变更为List，以兼容Common
     * 4、业务层面Common可能有多个作者；Lint只有一个作者，只取First()
     */
    private List<String> author;

    /**
     * 告警规则
     */
    @JSONField(name = "checkerName")
    private String checker;

    /**
     * 严重程度
     */
    private int severity;

    /**
     * 告警描述
     */
    @JSONField(name = "description")
    private String message;

    /**
     * 告警类型：新告警NEW(1)，历史告警HISTORY(2)
     */
    @Field("defect_type")
    private int defectType;

    /**
     * 告警行的变更时间，用于跟工具接入时间对比，早于接入时间则判断告警是历史告警，晚于等于接入时间则为新告警
     */
    @Field("line_update_time")
    private long lineUpdateTime;

    /**
     * 相对路径，相对路径的MD5是文件的唯一标志，是除去文件在服务器上存在的根目录后的路径
     * rel_path，file_path，url三者的区别：
     * rel_path: src/crypto/block.go,
     * file_path: /data/iegci/multi_tool_code_resource_5/maoyan0417001_dupc/src/crypto/block.go,
     * url: http://svn.xxx.com/codecc/test_project_proj/branches/test/Go/go-master/src/crypto/block.go,
     */
    @Field("rel_path")
    private String relPath;

    /**
     * 代码下载到服务器上的存放路径
     */
    @Field("file_path")
    private String filePath;

    @Field("pinpoint_hash")
    private String pinpointHash;

    @Field("pinpoint_hash_group")
    private String pinpointHashGroup;

    /**
     * 告警创建时间
     */
    @Field("create_time")
    private Long createTime;

    /**
     * 告警修复时间
     */
    @Field("fixed_time")
    private Long fixedTime;

    /**
     * 告警屏蔽时间
     */
    @Field("exclude_time")
    private Long excludeTime;

    /**
     * 告警忽略时间
     */
    @Field("ignore_time")
    private Long ignoreTime;

    /**
     * 告警忽略原因类型
     */
    @Field("ignore_reason_type")
    private Integer ignoreReasonType;

    /**
     * 告警忽略原因
     */
    @Field("ignore_reason")
    private String ignoreReason;

    /**
     * 告警忽略操作人
     */
    @Field("ignore_author")
    private String ignoreAuthor;

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
     * 代码库路径
     */
    private String url;

    /**
     * 代码仓库id
     */
    @Field("repo_id")
    private String repoId;

    /**
     * 版本号
     */
    private String revision;

    /**
     * 分支名称
     */
    private String branch;

    /**
     * 代码库子模块
     */
    @Field("sub_module")
    private String subModule;

    /**
     * 文件的最近修改时间
     */
    @Field("file_update_time")
    private long fileUpdateTime;

    @Field("file_md5")
    private String fileMd5;

    /**
     * 代码评论
     */
    @DBRef
    @Field("code_comment")
    private CodeCommentEntity codeComment;

    @Transient
    private Boolean newDefect;

    /**
     * 语言
     */
    private String language;

    /**
     * 语言值
     */
    @Field("lang_value")
    private Long langValue;

    /**
     * 是否忽略本告警
     */
    @Field("ignore_comment_defect")
    private Boolean ignoreCommentDefect;

    /**
     * 记录告警具体被哪个路径屏蔽
     */
    @Field("mask_path")
    private String maskPath;

    /**
     * 忽略规则
     */
    @Transient
    private String ignoreCommentReason;

    @Field("defect_instances")
    private List<DefectInstance> defectInstances;

    /**
     * 构建编号
     */
    @Transient
    private String buildId;

    /**
     * 记录忽略的 Build ID
     */
    @Field("ignore_build_id")
    private String ignoreBuildId;
}
