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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 描述
 *
 * @version V1.0
 * @date 2019/12/18
 */
@Slf4j
@Repository
public class BuildDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 根据 task id 和 build id 在 t_build 查构建信息, 然后从中取 white_paths 字段的值.
     * 如果在 t_build 中找不到对应的 entity, 或者找到的 entity 中没有 white_paths 字段, 返回 null.
     * 否则返回 white_paths 字段中的值 (有可能是一个空集合)
     *
     * @param buildId
     * @return java.util.List
     * @date 2024/10/17
     */
    public Set<String> getWhitePathsByTaskIdAndBuildId(Long taskId, String buildId) {
        Criteria criteria = Criteria.where("build_id").is(buildId);
        if (taskId != null) {
            criteria.and("task_id").is(taskId);
        }
        Query query = new Query(criteria);
        query.fields().include("white_paths");
        BuildEntity entity = defectMongoTemplate.findOne(query, BuildEntity.class);
        if (entity == null || entity.getWhitePaths() == null) {
            return null;
        }

        return new HashSet<>(entity.getWhitePaths());
    }

    public List<BuildEntity> findByBuildId(String buildId) {
        Query query = new Query(Criteria.where("build_id").is(buildId));
        return defectMongoTemplate.find(query, BuildEntity.class);
    }

    /**
     * 更新并返回构建信息
     *
     * @param buildEntity
     * @return
     */
    public BuildEntity upsertBuildInfo(BuildEntity buildEntity) {
        if (StringUtils.isBlank(buildEntity.getBuildId())) {
            log.error("build id is necessary.");
            return null;
        }
        Criteria criteria;
        if (buildEntity.getTaskId() != null) {
            criteria = new Criteria().andOperator(
                    Criteria.where("build_id").is(buildEntity.getBuildId()),
                    // 如果TaskId存在，除了需要匹配TaskId还需要匹配TaskId未生成的情况
                    new Criteria().orOperator(
                            Criteria.where("task_id").is(buildEntity.getTaskId()),
                            Criteria.where("task_id").exists(false),
                            Criteria.where("task_id").is(null)
                    )
            );
        } else {
            criteria = Criteria.where("build_id").is(buildEntity.getBuildId());
        }

        Update update = new Update();
        update.set("build_num", buildEntity.getBuildNo());
        update.set("build_time", buildEntity.getBuildTime());
        update.set("build_user", buildEntity.getBuildUser());
        update.set("task_id", buildEntity.getTaskId());
        if (buildEntity.getWhitePaths() != null) {
            update.set("white_paths", buildEntity.getWhitePaths());
        }
        if (buildEntity.getCheckerSetsVersion() != null) {
            update.set("checker_sets_version", buildEntity.getCheckerSetsVersion());
        }
        if (buildEntity.getReallocate() != null) {
            update.set("reallocate", buildEntity.getReallocate());
        }

        FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions();
        findAndModifyOptions.upsert(true);
        findAndModifyOptions.returnNew(true);
        Query query = new Query(criteria);
        return defectMongoTemplate.findAndModify(query, update, findAndModifyOptions, BuildEntity.class);
    }
}
