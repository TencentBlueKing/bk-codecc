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

package com.tencent.bk.codecc.defect.model.defect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.defect.model.CodeCommentEntity;
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
 * 圈复杂度告警的实体对象
 *
 * @date 2019/5/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_ccn_defect")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_pinpoint_hash_1", def = "{'task_id': 1, 'pinpoint_hash': 1}"),
        @CompoundIndex(name = "task_id_1_rel_path_1", def = "{'task_id': 1, 'rel_path': 1}", background = true),
        @CompoundIndex(name = "idx_taskid_1_status_1_ignore_reason_type_1",
                def = "{'task_id': 1,  'status': 1, 'ignore_reason_type': 1}", background = true)
})
@Sharded(shardKey = "task_id")
public class CCNDefectEntity extends DefectEntity {

    /**
     * 签名，用于唯一认证
     */
    @Field("func_signature")
    private String funcSignature;

    @Field("id")
    private String id;

    /**
     * 方法名
     */
    @Field("function_name")
    @JsonProperty("function_name")
    private String functionName;

    /**
     * 方法的完整名称
     */
    @Field("long_name")
    @JsonProperty("long_name")
    private String longName;

    /**
     * 圈复杂度
     */
    private int ccn;

    /**
     * 方法最后更新时间
     */
    @Field("latest_datetime")
    @JsonProperty("latest_datetime")
    private Long latestDateTime;

    /**
     * 方法最后更新作者
     */
    private String author;

    /**
     * 方法开始行号
     */
    @Field("start_lines")
    @JsonProperty("startLine")
    private Integer startLines;

    /**
     * 方法结束行号
     */
    @Field("end_lines")
    @JsonProperty("endLine")
    private Integer endLines;

    /**
     * 方法总行数
     */
    @Field("total_lines")
    @JsonProperty("total_lines")
    private Integer totalLines;

    /**
     * 包含圈复杂度计算节点的行号
     */
    @Field("condition_lines")
    @JsonProperty("condition_lines")
    private String conditionLines;

    /**
     * 风险系数，极高-1, 高-2，中-4，低-8
     * 该参数不入库，因为风险系数是可配置的
     */
    @Transient
    private int riskFactor;


    /**
     * 风险系数，极高, 高，中，低
     * 该参数不入库，因为风险系数是可配置的
     */
    @Transient
    private String riskFactorMsg;

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
     * 告警屏蔽时间
     */
    @Field("exclude_time")
    private Long excludeTime;

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
     * 文件相对路径
     */
    @Field("rel_path")
    @JsonProperty("rel_path")
    private String relPath;

    /**
     * 文件路径
     */
    @Field("file_path")
    private String filePath;

    /**
     * 代码仓库地址
     */
    private String url;

    /**
     * 仓库id
     */
    @Field("repo_id")
    @JsonProperty("repo_id")
    private String repoId;

    /**
     * 文件版本号
     */
    private String revision;

    /**
     * 分支名
     */
    private String branch;

    /**
     * Git子模块
     */
    @Field("sub_module")
    @JsonProperty("sub_module")
    private String subModule;

    /**
     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复，格式：
     * ANALYSIS_VERSION:projId:toolName
     */
    @Field("analysis_version")
    private String analysisVersion;

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
     * pinpoint_hash号
     */
    @Field("pinpoint_hash")
    private String pinpointHash;

    /**
     * pinpont_hash组号
     */
    @Field("pinpoint_hash_group")
    private String pinpointHashGroup;

    /**
     * 文件md5值
     */
    @Field("md5")
    private String md5;

    @DBRef
    @Field("code_comment")
    private CodeCommentEntity codeComment;

    /**
     * 是否是新告警
     */
    @Transient
    private Boolean newDefect;

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

    /**
     * 构建编号
     */
    @Transient
    private String buildId;

    /**
     * 新旧告警
     */
    @Transient
    private int defectType;

    /**
     * 记录忽略的 Build ID
     */
    @Field("ignore_build_id")
    private String ignoreBuildId;
}
