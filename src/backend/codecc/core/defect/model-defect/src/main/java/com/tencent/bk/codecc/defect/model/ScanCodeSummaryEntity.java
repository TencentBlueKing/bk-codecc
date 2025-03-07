package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 扫描代码统计信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_scan_code_summary")
@CompoundIndexes({
        @CompoundIndex(
                name = "createfrom_1_bgid_1_projectid_1_scanfinishtime_1",
                def = "{'create_from': 1, 'bg_id': 1, 'project_id': 1, 'scan_finish_time': 1}",
                background = true
        ),
        @CompoundIndex(name = "task_id_1_build_id_1", def = "{'task_id': 1, 'build_id': 1}", background = true)
})
public class ScanCodeSummaryEntity extends CommonEntity {

    @Field("task_id")
    private Long taskId;

    @Field("project_id")
    private String projectId;

    @Field("bg_id")
    private Integer bgId;

    @Field("create_from")
    private String createFrom;

    /**
     * 扫描构建ID
     */
    @Field("build_id")
    private String buildId;

    /**
     * 扫描类型 enum ScanStatType
     */
    @Field("scan_type")
    private String scanType;

    /**
     * 扫描结束时间
     */
    @Field("scan_finish_time")
    private Long scanFinishTime;

    /**
     * 扫描空白行数
     */
    @Field("total_blank")
    private Long totalBlank;

    /**
     * 扫描代码行数
     */
    @Field("total_code")
    private Long totalCode;

    /**
     * 扫描注释行数
     */
    @Field("total_comment")
    private Long totalComment;

    /**
     * 扫描总行数
     */
    @Field("total_line")
    private Long totalLine;
}
