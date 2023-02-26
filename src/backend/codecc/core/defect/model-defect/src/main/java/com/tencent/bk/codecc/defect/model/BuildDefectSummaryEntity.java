package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_build_defect_summary")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_build_id_1", def = "{'task_id':1, 'build_id':1}"),
        @CompoundIndex(name = "task_id_1_build_time_1", def = "{'task_id':1, 'build_time':1}"),
        @CompoundIndex(name = "task_id_1_repo_info_branch_1", def = "{'task_id':1, 'repo_info.branch':1}")
})
public class BuildDefectSummaryEntity extends CommonEntity {

    @Field("task_id")
    private Long taskId;

    @Field("tool_list")
    private List<String> toolList;

    @Field("build_id")
    private String buildId;

    @Field("build_num")
    private String buildNum;

    @Field("build_time")
    private Long buildTime;

    @Field("repo_info")
    private List<CodeRepo> repoInfo;

    @Field("build_name")
    private String buildUser;

    @Data
    public static class CodeRepo {

        public CodeRepo(String url, String branch) {
            this.url = url;
            this.branch = branch;
        }

        @Field("url")
        private String url;

        @Field("branch")
        private String branch;
    }
}
