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

package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;

/**
 * 告警基础信息VO
 *
 * @version V1.0
 * @date 2019/10/18
 */
@Data
@Schema(description = "告警基础信息视图")
public class DefectBaseVO extends CommonVO {

    @Schema(description = "告警描述", required = true)
    protected String message;
    /**
     * 规则类型，对应Coverity Platform中的Category(类别)
     */
    @Schema(description = "规则类型", required = true)
    protected String displayCategory;
    /**
     * 类型子类，对应Coverity Platform中的Type(类型)
     */
    @Schema(description = "类型子类", required = true)
    protected String displayType;
    @Schema(description = "告警行号", required = true)
    protected int lineNum;
    @Schema(description = "告警的唯一标志", required = true)
    private String id;
    @Schema(description = "哈希Id", required = true)
    private String hashId;
    @Schema(description = "任务ID", required = true)
    private long taskId;
    @Schema(description = "流名称", required = true)
    private String streamName;
    @Schema(description = "工具名", required = true)
    private String toolName;
    @Schema(description = "规则名称", required = true)
    private String checker;
    @Schema(description = "规则中文名称", required = true)
    private String checkerNameCn;
    @Schema(description = "描述", required = true)
    private String description;
    /**
     * 用户给告警标志的状态,这个状态采用自定义的状态，而不使用klocwork的状态
     * 1:待处理(默认)，4:已忽略，8:路径屏蔽，16:规则屏蔽，32:标志位已修改
     */
    @Schema(description = "状态", required = true)
    private int status;
    @Schema(description = "告警所在文件", required = true)
    private String filePath;
    @Schema(description = "文件MD5")
    private String fileMd5;
    @Schema(description = "文件名", required = true)
    private String fileName;
    @Schema(description = "告警处理人", required = true)
    private Set<String> authorList;
    @Schema(description = "告警严重程度", required = true)
    private int severity;
    @Schema(description = "忽略告警原因类型")
    private int ignoreReasonType;

    @Schema(description = "忽略告警具体原因")
    private String ignoreReason;

    @Schema(description = "忽略告警的作者")
    private String ignoreAuthor;

    @Schema(description = "告警创建时间")
    private long createTime;

    @Schema(description = "代码行提交时间")
    private long lineUpdateTime;

    @Schema(description = "告警修复时间")
    private long fixedTime;

    @Schema(description = "告警忽略时间")
    private long ignoreTime;

    @Schema(description = "告警屏蔽时间")
    private long excludeTime;

    /**
     * 告警是否被标记为已修改的标志，checkbox for developer, 0 is normal, 1 is tag, 2 is prompt
     */
    @Schema(description = "告警是否被标记为已修改的标志")
    private Integer mark;

    @Schema(description = "告警被标记为已修改的时间")
    private Long markTime;

    @Schema(description = "标记了，但是再次扫描没有修复")
    private Boolean markButNoFixed;

    @Schema(description = "被处理的时间，包括closed,excluded,ignore")
    private String offTime;

    @Schema(description = "创建时的构建号")
    private String createBuildNumber;

    @Schema(description = "修复时的构建号")
    private String fixedBuildNumber;

    @Schema(description = "文件对应仓库版本号")
    private String fileVersion;

    /**
     * 对应第三方缺陷管理系统的ID，这里声明为字符串可以有更好的兼容性
     */
    private String extBugId;

    /*--------------添加代码库信息 start----------------*/

    @Schema(description = "代码库id")
    private String repoId;

    @Schema(description = "版本号")
    private String revision;

    @Schema(description = "分支")
    private String branch;

    @Schema(description = "相对路径")
    private String relPath;

    @Schema(description = "代码库路径")
    private String url;

    /*--------------添加代码库信息 end----------------*/

}
