/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.mongodb.client.result.UpdateResult;
import com.tencent.bk.codecc.defect.dto.CodeLineModel;
import com.tencent.bk.codecc.defect.model.defect.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.vo.ClocStatisticInfoVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.util.ThreadUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * cloc信息持久类
 *
 * @version V1.0
 * @date 2019/9/29
 */
@Component
public class CLOCDefectDao
{
    @Autowired
    private MongoTemplate defectMongoTemplate;


    public UpdateResult upsertCLOCInfoByFileName(CLOCDefectEntity clocDefectEntity)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(clocDefectEntity.getTaskId()))
                .addCriteria(Criteria.where("file_name").is(clocDefectEntity.getFileName()));

        Update update = new Update();
        update.set("task_id", clocDefectEntity.getTaskId())
                .set("stream_name", clocDefectEntity.getStreamName())
                .set("file_name", clocDefectEntity.getFileName())
                .set("tool_name", clocDefectEntity.getToolName())
                .set("blank", clocDefectEntity.getBlank())
                .set("code", clocDefectEntity.getCode())
                .set("comment", clocDefectEntity.getComment())
                .set("language", clocDefectEntity.getLanguage());
        return defectMongoTemplate.upsert(query, update, CLOCDefectEntity.class);
    }

    public List<CLOCDefectEntity> findAllInTaskIds(List<Long> taskIds) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("task_id").in(taskIds)
                        .and("tool_name").is("SCC")
                        .and("status").is("ENABLED")
        );

        return defectMongoTemplate.find(query, CLOCDefectEntity.class);
    }

    /**
     * 批量查询 cloc 信息
     *
     * @date 2024/7/11
     * @param taskIds
     * @param langs
     * @return java.util.List<com.tencent.bk.codecc.defect.model.statistic.CLOCStatisticEntity>
     */
    public List<ClocStatisticInfoVO> batchQueryClocInfo(List<Long> taskIds, List<String> langs) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("task_id").in(taskIds)
                        .and("language").in(langs)
                        .and("tool_name").is("SCC")
                        .and("status").is("ENABLED")
        );

        List<CLOCDefectEntity> origin = defectMongoTemplate.find(query, CLOCDefectEntity.class);
        Map<Long, ClocStatisticInfoVO> clocMap = new HashMap<>();
        for (CLOCDefectEntity entity : origin) {
            long sumCode = (entity.getCode() == null) ? 0 : entity.getCode();
            long sumBlank = (entity.getBlank() == null) ? 0 : entity.getBlank();
            long sumComment = ((entity.getComment() == null) ? 0 : entity.getComment())
                    + ((entity.getEfficientComment() == null) ? 0 : entity.getEfficientComment());

            ClocStatisticInfoVO tmp;
            if (clocMap.containsKey(entity.getTaskId())) {
                tmp = clocMap.get(entity.getTaskId());
                tmp.setSumCode(tmp.getSumCode() + sumCode);
                tmp.setSumBlank(tmp.getSumBlank() + sumBlank);
                tmp.setSumComment(tmp.getSumComment() + sumComment);
            } else {
                tmp = new ClocStatisticInfoVO();
                tmp.setTaskId(entity.getTaskId());
                tmp.setSumCode(sumCode);
                tmp.setSumBlank(sumBlank);
                tmp.setSumComment(sumComment);
            }
            clocMap.put(entity.getTaskId(), tmp);
        }

        return new ArrayList<>(clocMap.values());
    }

    /**
     * 批量失效
     * @param taskId
     */
    public void batchDisableClocInfo(Long taskId, String toolName) {
        Query query = new Query();
        if (Tool.SCC.name().equals(toolName)) {
            query.addCriteria(
                    Criteria.where("task_id").is(taskId)
                            .and("tool_name").is(toolName)
                            .and("status").is("ENABLED")
            );
        } else {
            query.addCriteria(
                    Criteria.where("task_id").is(taskId)
                            .and("tool_name").in(Arrays.asList(toolName, null))
                            .and("status").is("ENABLED")
            );
        }
        Update update = new Update();
        update.set("status", "DISABLED");
        defectMongoTemplate.updateMulti(query, update, CLOCDefectEntity.class);
    }

    /**
     * 批量失效指定文件告警
     *
     * @param taskId 任务ID
     * @param fileNames 文件全路径
     * */
    public void batchDisableClocInfoByFileName(Long taskId, String toolName, List<String> fileNames) {

        List<List<String>> filePartition = Lists.partition(fileNames, 100000);

        for (List<String> stringList : filePartition) {
            Query query = new Query();
            if (Tool.SCC.name().equals(toolName)) {
                query.addCriteria(
                        Criteria.where("task_id").is(taskId)
                                .and("tool_name").is(toolName)
                                .and("file_name").in(stringList)
                                .and("status").is("ENABLED")
                );
            } else {
                query.addCriteria(Criteria.where("task_id").is(taskId)
                        .and("tool_name").in(Arrays.asList(toolName, null))
                        .and("file_name").in(stringList)
                        .and("status").is("ENABLED")
                );
            }
            Update update = new Update();
            update.set("status", "DISABLED");
            defectMongoTemplate.updateMulti(query, update, CLOCDefectEntity.class);
            ThreadUtils.sleep(100);
        }
    }

    /**
     * 批量更新写入指定文件告警
     * @param clocDefectEntityList 告警列表
     * */
    public void batchUpsertClocInfo(List<CLOCDefectEntity> clocDefectEntityList) {
        if (CollectionUtils.isNotEmpty(clocDefectEntityList))
        {
            BulkOperations operations =
                    defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CLOCDefectEntity.class);
            clocDefectEntityList.forEach(clocDefectEntity -> {
                Query query = new Query();
                String toolName = clocDefectEntity.getToolName();
                if (Tool.SCC.name().equals(toolName)) {
                    query.addCriteria(Criteria.where("task_id").is(clocDefectEntity.getTaskId()))
                            .addCriteria(Criteria.where("tool_name").is(toolName))
                            .addCriteria(Criteria.where("file_name").is(clocDefectEntity.getFileName()));
                } else {
                    query.addCriteria(Criteria.where("task_id").is(clocDefectEntity.getTaskId()))
                            .addCriteria(Criteria.where("tool_name").in(Arrays.asList(toolName, null)))
                            .addCriteria(Criteria.where("file_name").is(clocDefectEntity.getFileName()));
                }

                Update update = new Update();
                update.set("task_id", clocDefectEntity.getTaskId())
                        .set("stream_name", clocDefectEntity.getStreamName())
                        .set("file_name", clocDefectEntity.getFileName())
                        .set("rel_path", clocDefectEntity.getRelPath())
                        .set("tool_name", toolName)
                        .set("blank", clocDefectEntity.getBlank())
                        .set("code", clocDefectEntity.getCode())
                        .set("comment", clocDefectEntity.getComment())
                        .set("efficient_comment", clocDefectEntity.getEfficientComment())
                        .set("language", clocDefectEntity.getLanguage())
                        .set("status", "ENABLED");
                operations.upsert(query, update);
            });
            operations.execute();
        }
    }

    /**
     * 查询代码行数信息
     * @param taskId
     * @return
     */
    public List<CodeLineModel> getCodeLineInfo(Long taskId, String toolName) {
        //以taskid进行过滤
        MatchOperation match;
        if (Tool.SCC.name().equals(toolName)) {
            match = Aggregation.match(Criteria.where("task_id").is(taskId)
                    .and("tool_name").is(toolName).and("status").ne("DISABLED"));
        } else {
            match = Aggregation.match(Criteria.where("task_id").is(taskId)
                    .and("tool_name").in(Arrays.asList(toolName, null)).and("status").ne("DISABLED"));
        }
        //以toolName进行分组，并且取第一个的endTime字段
        GroupOperation group = Aggregation.group("language")
                .sum("code").as("codeLine")
                .sum("comment").as("commentLine")
                .sum("efficient_comment").as("efficientCommentLine");

        ProjectionOperation project = Aggregation.project()
                .andExpression("_id").as("language")
                .andExpression("codeLine").as("codeLine")
                .andExpression("commentLine").as("commentLine")
                .andExpression("efficientCommentLine").as("efficientCommentLine");
        
        //聚合配置
        Aggregation agg = Aggregation.newAggregation(match, group, project);

        AggregationResults<CodeLineModel> queryResult = defectMongoTemplate.aggregate(agg,
                "t_cloc_defect", CodeLineModel.class);
        return queryResult.getMappedResults();
    }

}
