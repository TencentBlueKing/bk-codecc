package com.tencent.bk.codecc.defect.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetPackageEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckerSetPackageRepository extends MongoRepository<CheckerSetPackageEntity, String> {

    List<CheckerSetPackageEntity> findByLangValue(Long langValue);

    List<CheckerSetPackageEntity> findByTypeAndLangValueAndCheckerSetId(String type, Long langValue,
            String checkerSetId);

    List<CheckerSetPackageEntity> findByTypeAndLangValueAndEnvType(String type, Long langValue, String envType);

    List<CheckerSetPackageEntity> findByEnvType(String envType);

    List<CheckerSetPackageEntity> findByTypeAndEnvType(String type, String envType);
}
