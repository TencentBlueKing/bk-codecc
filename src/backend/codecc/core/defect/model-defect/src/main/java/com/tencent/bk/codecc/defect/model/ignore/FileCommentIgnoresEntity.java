package com.tencent.bk.codecc.defect.model.ignore;

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

import java.util.List;

/**
 * 记录一个文件中所有的注释忽略信息
 *
 * @date 2025/05/27
 */
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_file_path_1", def = "{'task_id': 1,'file_path': 1}", background = true)
})
@Document(collection = "t_file_comment_ignores")
public class FileCommentIgnoresEntity extends CommonEntity {
    @Field("task_id")
    private Long taskId;
    @Field("file_path")
    private String filePath;
    @Field("line_ignore_infos")
    private List<LineIgnoreInfo> lineIgnoreInfos;

    @Data
    public static class LineIgnoreInfo {
        @Field("line_num")
        private Long lineNum;
        @Field("ignore_infos")
        private List<IgnoreInfo> ignoreInfos;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class IgnoreInfo {
        @Field("checker")
        private String checker;
        @Field("ignore_reason")
        private String ignoreReason;
    }
}
