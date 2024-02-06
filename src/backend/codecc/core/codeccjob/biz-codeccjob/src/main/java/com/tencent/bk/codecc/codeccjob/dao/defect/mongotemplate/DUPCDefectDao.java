package com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.codecc.common.db.MongoPageHelper;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 重复率持久代码
 *
 * @version V1.0
 */
@Repository
public class DUPCDefectDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 批量更新告警状态的exclude位
     *
     * @param taskId
     * @param defectList
     */
    public void batchUpdateDefectStatusExcludeBit(long taskId, List<DUPCDefectEntity> defectList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DUPCDefectEntity.class);
            defectList.forEach(defectEntity -> {
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("exclude_time", defectEntity.getExcludeTime());
                update.set("mask_path", defectEntity.getMaskPath());

                Query query = new Query(
                        Criteria.where("task_id").is(taskId)
                                .and("_id").is(new ObjectId(defectEntity.getEntityId()))
                );

                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 查询符合条件的重复率告警
     * 仅返回：status、exclude_time
     *
     * @param taskId
     * @param excludeStatusSet
     * @param filterPaths
     * @param pageSize
     * @param lastId
     * @return
     */
    public List<DUPCDefectEntity> findDefectsByFilePath(Long taskId,
            Set<Integer> excludeStatusSet,
            Set<String> filterPaths,
            int pageSize,
            String lastId) {
        Document fieldsObj = new Document();
        fieldsObj.put("status", true);
        fieldsObj.put("exclude_time", true);

        Query query = new BasicQuery(new Document(), fieldsObj);

        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("status").nin(excludeStatusSet));

        Criteria orOperator = new Criteria();
        orOperator.orOperator(
                filterPaths.stream().map(file -> Criteria.where("file_path").regex(file)).toArray(Criteria[]::new));
        query.addCriteria(orOperator);

        if (StringUtils.isNotEmpty(lastId)) {
            query.addCriteria(Criteria.where(MongoPageHelper.ID).gt(new ObjectId(lastId)));
        }
        query.with(Sort.by(Sort.Direction.ASC, MongoPageHelper.ID)).limit(pageSize);
        return defectMongoTemplate.find(query, DUPCDefectEntity.class);
    }
}
