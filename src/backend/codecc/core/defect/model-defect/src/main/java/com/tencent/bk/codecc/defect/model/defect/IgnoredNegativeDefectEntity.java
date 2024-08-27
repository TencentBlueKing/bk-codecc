package com.tencent.bk.codecc.defect.model.defect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 这张表保存用户以 “误报” 为理由忽略的告警
 *
 * @date 2024/01/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_ignored_negative_defect")
@CompoundIndexes({
        @CompoundIndex(
                name = "idx_tool_name_1_ignore_time_-1_severity_1_id_-1",
                def = "{'tool_name' : 1, 'ignore_time' : -1, 'severity' : 1, '_id' : -1}",
                background = true
        )
})
public class IgnoredNegativeDefectEntity extends DefectEntity {

    /**
     * 告警 ID
     */
    @Field("defect_id")
    @Indexed
    private String defectId;

    @Field("tool_name")
    private String toolName;

    /**
     * 项目名
     */
    @Field("project_name")
    private String projectName;

    /**
     * 任务名 (中文)
     */
    @Field("task_name_cn")
    private String taskNameCn;

    /**
     * 任务名 (英文)
     */
    @Field("task_name_en")
    private String taskNameEn;

    /**
     * 代码库路径
     */
    @Field("url")
    private String url;

    /**
     * 告警行号
     */
    @Field("line_number")
    private int lineNum;

    /**
     * 规则名称
     */
    @Field("checker_name")
    private String checker;

    /**
     * 规则描述
     */
    @Field("description")
    private String message;

    /**
     * 规则严重程度，1=>严重，2=>一般，3=>提示
     */
    @Field("severity")
    private int severity;

    /**
     * 告警忽略人
     */
    @Field("ignore_author")
    private String ignoreAuthor;

    /**
     * 告警忽略时间
     */
    @Field("ignore_time")
    private Long ignoreTime;

    /**
     * 告警忽略原因
     */
    @Field("ignore_reason")
    private String ignoreReason;

    /**
     * 告警忽略原因类型
     */
    @Field("ignore_reason_type")
    private Integer ignoreReasonType;

    /**
     * 误报的处理进展:
     * 0, 待处理;
     * 1, 已优化工具;
     * 2, 非工具原因;
     * 3, 其他.
     */
    @Field("process_progress")
    private int processProgress;

    /**
     * 误报的处理原因类型:
     * 1. 用户误操作
     * 2. 用户不配合
     * 3. 无法查看问题代码
     * 4. 受限于技术架构
     * 5. 修复成本过高
     * 6. 其他（需要输入原因）
     * 其中, 1 是 "非工具原因" 的误报的原因, 2-5 是 "其他" 的误报的原因, 6 是通用的原因.
     */
    @Field("process_reason_type")
    private int processReasonType;

    /**
     * 误报的处理原因
     */
    @Field("process_reason")
    private String processReason;

    /**
     * 规则标签
     */
    @Field("checker_tag")
    private List<String> checkerTag;

    /**
     *  误报的告警所属任务的创建来源
     */
    @Field("create_from")
    private String createFrom;

    /**
     * 告警所在文件
     */
    @Field("file_path_name")
    private String filePath;

    /**
     * 文件链接
     */
    @Field("file_link")
    private String fileLink;

    /**
     * 告警所在文件名
     */
    @Field("file_name")
    private String fileName;

    /**
     * trace link 类型的告警需要用该字段记录告警链
     */
    @Field("defect_instances")
    private List<DefectInstance> defectInstances;

    /**
     * 相对路径
     */
    @Field("rel_path")
    private String relPath;

    /**
     * 分支名
     */
    @Field("branch")
    private String branch;

    /**
     * 事业群id
     */
    @Field("bg_id")
    private int bgId;

    /**
     * 部门id
     */
    @Field("dept_id")
    private int deptId;

    /**
     * 中心id
     */
    @Field("center_id")
    private int centerId;

    /**
     * TAPD/Github Issue链接
     */
    @Field("issue_link")
    private String issueLink;
}
