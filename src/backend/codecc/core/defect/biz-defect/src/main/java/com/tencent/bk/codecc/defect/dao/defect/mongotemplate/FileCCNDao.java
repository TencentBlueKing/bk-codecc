package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.FileCCNEntity;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/2/18
 */
@Repository
public class FileCCNDao {

    private static Logger logger = LoggerFactory.getLogger(FileCCNDao.class);

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 更新或插入文件圈复杂度列表
     *
     * @param fileCCNList
     */
    public void upsertFileCCNList(List<FileCCNEntity> fileCCNList) {
        if (CollectionUtils.isEmpty(fileCCNList)) {
            return;
        }
        if (fileCCNList.size() <= ComConstants.COMMON_BATCH_PAGE_SIZE) {
            doUpsertFileCCNList(fileCCNList);
        } else {
            List<List<FileCCNEntity>> subLists = Lists.partition(fileCCNList, ComConstants.COMMON_BATCH_PAGE_SIZE);
            for (List<FileCCNEntity> subList : subLists) {
                doUpsertFileCCNList(subList);
            }
        }
    }

    private void doUpsertFileCCNList(List<FileCCNEntity> fileCCNList) {
        BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, FileCCNEntity.class);
        if (CollectionUtils.isNotEmpty(fileCCNList)) {
            for (FileCCNEntity fileCCN : fileCCNList) {
                Query query = new Query(
                        Criteria.where("task_id").is(fileCCN.getTaskId())
                                .and("file_rel_path").is(fileCCN.getFileRelPath())
                );
                Update update = new Update();
                update.set("total_ccn_count", fileCCN.getTotalCCNCount())
                        .set("file_path", fileCCN.getFilePath())
                        .set("file_rel_path", fileCCN.getFileRelPath())
                        .set("task_id", fileCCN.getTaskId());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }
}
