package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.BuildEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 描述
 *
 * @version V1.0
 * @date 2019/12/18
 */
@Slf4j
@Repository
public class BuildDao
{

    @Autowired
    private MongoTemplate defectMongoTemplate;


    /**
     * 更新并返回构建信息
     * @param buildEntity
     * @return
     */
    public BuildEntity upsertBuildInfo(BuildEntity buildEntity) {
        if (StringUtils.isBlank(buildEntity.getBuildId())) {
            return null;
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("build_id").is(buildEntity.getBuildId()));
        Update update = new Update();
        update.set("build_num", buildEntity.getBuildNo());
        update.set("build_time", buildEntity.getBuildTime());
        update.set("build_user", buildEntity.getBuildUser());
        FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions();
        findAndModifyOptions.upsert(true);
        findAndModifyOptions.returnNew(true);
        return defectMongoTemplate.findAndModify(query, update, findAndModifyOptions, BuildEntity.class);
    }
}
