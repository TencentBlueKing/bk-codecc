package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.DynamicConfigEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 动态配置持久化接口
 *
 * @version V1.0
 * @date 2021/5/8
 */
@Repository
public interface DynamicConfigRepository extends MongoRepository<DynamicConfigEntity, ObjectId> {

    /**
     * 根据基础数据类型查询对应基础数据信息
     *
     * @param key
     * @return
     */
    DynamicConfigEntity findFirstByKey(String key);


}
