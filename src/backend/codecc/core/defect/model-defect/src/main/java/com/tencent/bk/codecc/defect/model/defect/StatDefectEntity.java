package com.tencent.bk.codecc.defect.model.defect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.codecc.common.db.CommonEntity;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.util.CollectionUtils;

@Data
@Document(collection = "t_stat_defect")
@NoArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1_status_1", def = "{'task_id': 1, 'tool_name': 1, 'status': 1}", background = true)
})
public class StatDefectEntity extends CommonEntity {

    @Field("task_id")
    @JsonProperty("task_id")
    private long taskId;
    @Field("tool_name")
    @JsonProperty("tool_name")
    private String toolName;
    @Field("status")
    @JsonProperty("status")
    private String status;
    @Field("msg_id")
    @JsonProperty("msg_id")
    private String msgId;
    @Field("time_stamp")
    @JsonProperty("time_stamp")
    private String timeStamp;
    @Field("user_name")
    @JsonProperty("user_name")
    private String username;
    @Field("msg_body")
    @JsonProperty("msg_body")
    private String msgBody;

    /**
     * 字段赋值
     */
    public void setByToolStatInfo(Map<String, String> infoMap) {
        if (CollectionUtils.isEmpty(infoMap)) {
            return;
        }
        // 针对有效告警，默认设置标志位为enabled
        setStatus("ENABLED");
        if (infoMap.containsKey("msg_id")) {
            setMsgId(infoMap.get("msg_id"));
        }
        if (infoMap.containsKey("time_stamp")) {
            setTimeStamp(infoMap.get("time_stamp"));
        }
        if (infoMap.containsKey("user_name")) {
            setUsername(infoMap.get("user_name"));
        }
        if (infoMap.containsKey("msg_body")) {
            setMsgBody(infoMap.get("msg_body"));
        }
    }
}
