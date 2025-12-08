package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 这张表保存大模型误报过滤功能相关的缓存信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_llm_negative_defect_info")
@CompoundIndexes({
        @CompoundIndex(
                name = "task_id_1_file_path_1_llm_judge_1",
                def = "{'task_id': 1, 'file_path': 1, 'llm_judge': 1}",
                background = true
        ),
        @CompoundIndex(name = "defect_id_1_task_id_1", def = "{'defect_id': 1, 'task_id': 1}", background = true)
})
public class LLMNegativeDefectFilterInfoEntity extends CommonEntity {
    @Field("task_id")
    private Long taskId;
    @Field("defect_id")
    private String defectId;
    @Field("checker_name")
    private String checkerName;
    @Field("file_path")
    private String filePath;
    @Field("line_num")
    private Long lineNum;
    @Field("pinpoint_hash")
    private String pinpointHash;
    // [startLine, endLine] 由扫描工具输出, 表示工具认为这个区间内的代码跟该告警有关. 具体用途:
    // 1. 用户如果标记该告警为误报并加以忽略, 后台可能会将这个区间内的代码取出来, 塞到 LLM 的知识库中学习, 以提升模型判断误报的能力.
    // 2. 大模型误报过滤时, 后台会将这个区间内的代码取出来, 帮助大模型判断该告警是否误报
    @Field("start_line")
    private Long startLine;
    @Field("end_line")
    private Long endLine;

    // 是否已经作为误报上报给 AI 服务的知识库.
    // true, AI 服务的知识库中(应该)已保存该误报告警;
    // false, 一般是用户误报忽略后, 又取消忽略, 后台将这个告警从知识库中删掉;
    // 有可能为空.
    @Field("is_negative")
    Boolean isNegative;
    @Field("is_negative_update_time")
    Long isNegativeUpdateTime;

    // 用户驳回 LLM 误报过滤的结果
    @Field("is_refuted")
    Boolean isRefuted;
    @Field("refute_update_time")
    Long refuteUpdateTime;

    // 保存大模型的判断结果.
    // true, 大模型认为该告警是误报; false, 大模型认为不是误报; 有可能为空.
    @Field("llm_judge")
    Boolean llmJudge;
    // 判断依据
    @Field("reason")
    String reason;
    @Field("judge_update_time")
    Long judgeUpdateTime;
}
