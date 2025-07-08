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

package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.SummaryDefectGatherEntity;
import com.tencent.bk.codecc.defect.model.SummaryGatherInfo;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * lint类工具持久层代码
 *
 * @version V1.0
 * @date 2019/5/10
 */
@Slf4j
@Repository
public class SummaryDefectGatherDao {
    @Autowired
    private MongoTemplate defectMongoTemplate;


    /**
     * 更新告警状态
     *
     * @param taskId
     * @param toolName
     * @param status
     */
    public void updateStatus(Long taskId, String toolName, Integer status) {
        Query query = getTaskIdAndToolNameQuery(taskId, toolName);
        Long curTime = System.currentTimeMillis();
        Update update = Update.update("status", status)
                .set("updated_date", curTime);
        if (ComConstants.DefectStatus.FIXED.value() == status) {
            update.set("fixed_time", curTime);
        }
        defectMongoTemplate.updateFirst(query, update, SummaryDefectGatherEntity.class);
    }

    /**
     * 更新告警信息
     *
     * @param taskId
     * @param toolName
     * @param gatherInfo
     */
    public void upsertByGatherInfo(Long taskId, String toolName, SummaryGatherInfo gatherInfo) {
        Query query = getTaskIdAndToolNameQuery(taskId, toolName);
        Long curTime = System.currentTimeMillis();
        Update update = Update.update("task_id", taskId)
                .set("tool_name", toolName)
                .set("defect_count", gatherInfo.getDefectCount())
                .set("file_count", gatherInfo.getFileCount())
                .set("file_name", gatherInfo.getFileName())
                .set("status", ComConstants.DefectStatus.NEW.value())
                .set("createdDate", curTime)
                .set("updatedDate", curTime);
        defectMongoTemplate.upsert(query, update, SummaryDefectGatherEntity.class);
    }


    private Query getTaskIdAndToolNameQuery(Long taskId, String toolName) {
        return Query.query(Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName));
    }
}
