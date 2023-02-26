package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * 规则维度告警误报统计
 *
 * @version 1.0
 * @date 2022/4/18
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_checker_misreport_stat")
@CompoundIndexes({
        @CompoundIndex(name = "datafrom_1_statdate_1_toolname_1",
                def = "{'data_from': 1, 'stat_date': 1, 'tool_name': 1}", background = true)
})
public class CheckerMisreportStatEntity extends CommonEntity {

    public CheckerMisreportStatEntity() {
    }

    public CheckerMisreportStatEntity(String dataFrom, Long statDate, String toolName, String checkerName) {
        this.dataFrom = dataFrom;
        this.statDate = statDate;
        this.toolName = toolName;
        this.checkerName = checkerName;
    }

    @Field("tool_name")
    private String toolName;

    @Field("checker_name")
    private String checkerName;

    @Field("exist_count")
    private int existCount;

    @Field("fixed_count")
    private int fixedCount;

    @Field("excluded_count")
    private int excludedCount;

    @Field("ignore_count")
    private int ignoreCount;

    @Field("ignore_error_defect_count")
    private int ignoreErrorDefectCount;

    @Field("comment_count")
    private int commentCount;

    /**
     * 数据来源按开源、非开源的维度统计 enum DefectStatType
     */
    @Field("data_from")
    private String dataFrom;

    /**
     * 统计告警所属时间
     */
    @Field("stat_date")
    private Long statDate;

    /**
     * 该数据涉及的任务id
     */
    @Field("task_id_set")
    private Set<Long> taskIdSet;
}
