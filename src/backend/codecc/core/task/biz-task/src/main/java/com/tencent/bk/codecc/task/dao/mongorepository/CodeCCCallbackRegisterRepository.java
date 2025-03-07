package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.CodeCCCallbackRegister;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 基础数据持久化接口
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Repository
public interface CodeCCCallbackRegisterRepository extends MongoRepository<CodeCCCallbackRegister, String> {

    CodeCCCallbackRegister findFirstByTaskId(Long taskId);

}
