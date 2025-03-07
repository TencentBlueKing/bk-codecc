package com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca

import com.tencent.bk.codecc.defect.model.sca.LicenseDetailEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface LicenseDetailRepository : MongoRepository<LicenseDetailEntity, String> {
    /**
     * 根据许可证名称列表查询许可证详情
     * @param names 许可证名称列表
     * @return 匹配的许可证详情列表
     */
    fun findByNameIn(names: List<String>): List<LicenseDetailEntity>

    /**
     * 根据名称查找第一个匹配的许可证详情
     * @param name 许可证名称
     * @return 匹配的许可证详情实体或null
     */
    fun findFirstByName(name: String): LicenseDetailEntity?

    /**
     * 根据名称集合或别名集合查询许可证详情
     * @param names 许可证名称集合
     * @param alias 许可证别名集合
     * @return 匹配的许可证详情列表
     */
    fun findByNameInOrAliasIn(
        name: Collection<String>,
        alias: Collection<String>
    ): List<LicenseDetailEntity>
}
