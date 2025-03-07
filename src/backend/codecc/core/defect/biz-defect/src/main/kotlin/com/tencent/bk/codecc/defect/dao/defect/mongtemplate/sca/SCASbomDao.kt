package com.tencent.bk.codecc.defect.dao.defect.mongtemplate.sca

import com.google.common.collect.Lists
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomFileEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomInfoEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomRelationshipEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomSnippetEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomFileEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomInfoEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomRelationshipEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomSnippetEntity
import com.tencent.codecc.common.db.CommonEntity
import com.tencent.devops.common.constant.ComConstants
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.FindAndReplaceOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import org.springframework.util.CollectionUtils
import java.util.Collections

@Repository
class SCASbomDao @Autowired constructor(
    private val defectMongoTemplate: MongoTemplate
) {

    fun findSbomInfoByTaskIdAndToolName(taskId: Long, toolName: String): SCASbomInfoEntity? {
        val query = Query.query(Criteria.where("task_id").`is`(taskId).and("tool_name").`is`(toolName))
        return defectMongoTemplate.findOne(query, SCASbomInfoEntity::class.java)
    }

    fun saveSbomInfo(scaSbomInfoEntity: SCASbomInfoEntity) {
        defectMongoTemplate.save(scaSbomInfoEntity)
    }

    fun saveSbomPackages(taskId: Long, scaSbomPackages: List<SCASbomPackageEntity>) {
        doSaveSbomEntity(taskId, scaSbomPackages, SCASbomPackageEntity::class.java)
    }

    fun findPackagesByNameAndVersions(
        taskId: Long,
        toolName: String,
        nameToVersions: Map<String, List<String>?>
    ): List<SCASbomPackageEntity> {
        if (CollectionUtils.isEmpty(nameToVersions)) {
            return Collections.emptyList()
        }
        val keys = nameToVersions.keys.toList()
        val pageKeysList = Lists.partition(keys, ComConstants.COMMON_NUM_1000)
        val packages = mutableListOf<SCASbomPackageEntity>()
        for (pageKeys in pageKeysList) {
            val nameAndVersionCris = pageKeys.mapNotNull { key ->
                val versions = nameToVersions[key]
                if (versions.isNullOrEmpty()) {
                    Criteria.where("name").`is`(key).and("version").exists(false)
                } else {
                    Criteria.where("name").`is`(key).and("version").`in`(versions)
                }
            }.toList()
            if (nameAndVersionCris.isEmpty()) {
                continue
            }
            val cri = Criteria().orOperator(nameAndVersionCris).and("task_id").`is`(taskId)
                    .and("tool_name").`is`(toolName)
            val pagePackages = defectMongoTemplate.find(Query.query(cri), SCASbomPackageEntity::class.java)
            if (pagePackages.isEmpty()) {
                continue
            }
            packages.addAll(pagePackages)
        }
        return packages
    }

    fun findPackagesByStatusWithCursor(
        taskId: Long,
        toolName: String,
        status: Int,
        skipId: String?,
        pageSize: Int
    ): List<SCASbomPackageEntity> {
        val cri = Criteria.where("task_id").`is`(taskId)
                .and("tool_name").`is`(toolName).and("status").`is`(status)
        if (!skipId.isNullOrEmpty()) {
            cri.and("_id").gt(ObjectId(skipId))
        }
        val query = Query.query(cri)
        query.limit(pageSize)
        return defectMongoTemplate.find(query, SCASbomPackageEntity::class.java)
    }

    fun removeRelationsByTaskIdAndToolName(taskId: Long, toolName: String) {
        val query = Query.query(Criteria.where("task_id").`is`(taskId).and("tool_name").`is`(toolName))
        defectMongoTemplate.remove(query, SCASbomRelationshipEntity::class.java)
    }

    fun removeRelationsByTaskIdAndToolNameAndElementIds(taskId: Long, toolName: String, elementIds: Set<String>) {
        val cri = Criteria().andOperator(
            Criteria.where("task_id").`is`(taskId).and("tool_name"),
            Criteria().orOperator(
                Criteria.where("element_id").`in`(elementIds),
                Criteria.where("related_element_id").`in`(elementIds)
            )
        )
        val query = Query.query(cri)
        defectMongoTemplate.remove(query, SCASbomRelationshipEntity::class.java)
    }

    fun findRelationsByTaskIdAndToolName(taskId: Long, toolName: String): List<SCASbomRelationshipEntity> {
        val query = Query.query(Criteria.where("task_id").`is`(taskId).and("tool_name").`is`(toolName))
        return defectMongoTemplate.find(query, SCASbomRelationshipEntity::class.java)
    }

    fun findFilesByTaskAndToolNameAndElementIds(
        taskId: Long,
        toolName: String,
        elementIds: Collection<String>
    ): List<SCASbomFileEntity> {
        if (CollectionUtils.isEmpty(elementIds)) {
            return emptyList()
        }
        val sbomFiles = mutableListOf<SCASbomFileEntity>()
        val idsParts = Lists.partition(Lists.newArrayList(elementIds), ComConstants.COMMON_NUM_10000)
        for (idsPart in idsParts) {
            val query = Query.query(
                Criteria.where("task_id").`is`(taskId).and("tool_name").`is`(toolName)
                        .and("element_id").`in`(idsPart)
            )
            val partSbomFiles = defectMongoTemplate.find(query, SCASbomFileEntity::class.java)
            if (partSbomFiles.isNotEmpty()) {
                sbomFiles.addAll(partSbomFiles)
            }
        }
        return sbomFiles
    }

    fun findSnippetsByTaskAndToolNameAndElementIds(
        taskId: Long,
        toolName: String,
        elementIds: Collection<String>
    ): List<SCASbomSnippetEntity> {
        if (CollectionUtils.isEmpty(elementIds)) {
            return emptyList()
        }
        val sbomSnippets = mutableListOf<SCASbomSnippetEntity>()
        val idsParts = Lists.partition(Lists.newArrayList(elementIds), ComConstants.COMMON_NUM_10000)
        for (idsPart in idsParts) {
            val query = Query.query(
                Criteria.where("task_id").`is`(taskId).and("tool_name").`is`(toolName)
                        .and("element_id").`in`(idsPart)
            )
            val partSbomSnippets = defectMongoTemplate.find(query, SCASbomSnippetEntity::class.java)
            if (partSbomSnippets.isNotEmpty()) {
                sbomSnippets.addAll(partSbomSnippets)
            }
        }
        return sbomSnippets
    }

    fun findFilesByTaskAndToolNameAndStatusWithCursor(
        taskId: Long,
        toolName: String,
        status: Int,
        elementId: String?,
        pageSize: Int
    ): List<SCASbomFileEntity> {
        val cri = Criteria.where("task_id").`is`(taskId)
                .and("tool_name").`is`(toolName).and("status").`is`(status)
        if (!elementId.isNullOrEmpty()) {
            cri.and("element_id").gt(elementId)
        }
        val query = Query.query(cri)
        query.limit(pageSize)
        return defectMongoTemplate.find(query, SCASbomFileEntity::class.java)
    }

    fun findSnippetByTaskAndToolNameAndStatusWithCursor(
        taskId: Long,
        toolName: String,
        status: Int,
        elementId: String?,
        pageSize: Int
    ): List<SCASbomSnippetEntity> {
        val cri = Criteria.where("task_id").`is`(taskId)
                .and("tool_name").`is`(toolName).and("status").`is`(status)
        if (!elementId.isNullOrEmpty()) {
            cri.and("element_id").gt(elementId)
        }
        val query = Query.query(cri)
        query.limit(pageSize)
        return defectMongoTemplate.find(query, SCASbomSnippetEntity::class.java)
    }

    fun saveSbomRelations(taskId: Long, scaSbomRelations: List<SCASbomRelationshipEntity>) {
        doSaveSbomEntity(taskId, scaSbomRelations, SCASbomRelationshipEntity::class.java)
    }

    fun saveSbomFiles(taskId: Long, sbomFiles: List<SCASbomFileEntity>) {
        doSaveSbomEntity(taskId, sbomFiles, SCASbomFileEntity::class.java)
    }

    fun saveSbomSnippets(taskId: Long, sbomSnippets: List<SCASbomSnippetEntity>) {
        doSaveSbomEntity(taskId, sbomSnippets, SCASbomSnippetEntity::class.java)
    }

    fun saveBuildSbomInfo(
        taskId: Long,
        toolName: String,
        buildId: String,
        buildSCASbomInfoEntity: BuildSCASbomInfoEntity
    ) {
        val query = Query.query(
            Criteria.where("task_id").`is`(taskId)
                    .and("tool_name").`is`(toolName).and("build_id").`is`(buildId)
        )
        val oldBuildEntity = defectMongoTemplate.findOne(query, BuildSCASbomInfoEntity::class.java)
        if (oldBuildEntity != null) {
            // 兼容重试的场景
            buildSCASbomInfoEntity.entityId = oldBuildEntity.entityId
        }
        defectMongoTemplate.save(buildSCASbomInfoEntity)
    }

    fun saveBuildSbomPackages(
        taskId: Long,
        toolName: String,
        buildId: String,
        packages: List<BuildSCASbomPackageEntity>
    ) {
        doSaveSbomSnapshot(
            taskId, toolName, buildId, packages,
            BuildSCASbomPackageEntity::class.java
        )
    }

    fun saveBuildSbomFiles(
        taskId: Long,
        toolName: String,
        buildId: String,
        files: List<BuildSCASbomFileEntity>
    ) {
        doSaveSbomSnapshot(
            taskId, toolName, buildId, files,
            BuildSCASbomFileEntity::class.java
        )
    }

    fun saveBuildSbomSnippets(
        taskId: Long,
        toolName: String,
        buildId: String,
        snippets: List<BuildSCASbomSnippetEntity>
    ) {
        doSaveSbomSnapshot(
            taskId, toolName, buildId, snippets, BuildSCASbomSnippetEntity::class.java
        )
    }

    fun saveBuildSbomRelationships(
        taskId: Long,
        toolName: String,
        buildId: String,
        relationships: List<BuildSCASbomRelationshipEntity>
    ) {
        doSaveSbomSnapshot(
            taskId, toolName, buildId, relationships, BuildSCASbomRelationshipEntity::class.java
        )
    }

    fun getBuildSbomInfo(taskId: Long, toolName: String, buildId: String): BuildSCASbomInfoEntity? {
        val sbomInfos =
            getSnapshotByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId, BuildSCASbomInfoEntity::class.java)
        return if (sbomInfos.isEmpty()) {
            null
        } else {
            sbomInfos.first()
        }
    }

    fun getBuildSbomPackages(taskId: Long, toolName: String, buildId: String): List<BuildSCASbomPackageEntity> {
        return getSnapshotByTaskIdAndToolNameAndBuildId(
            taskId,
            toolName,
            buildId,
            BuildSCASbomPackageEntity::class.java
        )
    }

    fun getBuildSbomFiles(taskId: Long, toolName: String, buildId: String): List<BuildSCASbomFileEntity> {
        return getSnapshotByTaskIdAndToolNameAndBuildId(
            taskId,
            toolName,
            buildId,
            BuildSCASbomFileEntity::class.java
        )
    }

    fun getBuildSbomSnippets(taskId: Long, toolName: String, buildId: String): List<BuildSCASbomSnippetEntity> {
        return getSnapshotByTaskIdAndToolNameAndBuildId(
            taskId,
            toolName,
            buildId,
            BuildSCASbomSnippetEntity::class.java
        )
    }

    fun getBuildSbomRelationships(
        taskId: Long,
        toolName: String,
        buildId: String
    ): List<BuildSCASbomRelationshipEntity> {
        return getSnapshotByTaskIdAndToolNameAndBuildId(
            taskId,
            toolName,
            buildId,
            BuildSCASbomRelationshipEntity::class.java
        )
    }

    fun <T : CommonEntity> findSBomEntityByIds(taskId: Long, ids: List<String>, clazz: Class<out T>): List<T> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        val pageIdsList = Lists.partition(ids, ComConstants.COMMON_NUM_10000)
        val entities = mutableListOf<T>()
        for (pageIds in pageIdsList) {
            val objIds = pageIds.map { ObjectId(it) }
            val cri = Criteria.where("task_id").`is`(taskId).and("_id").`in`(objIds)
            val query = Query.query(cri)
            val pageEntities = defectMongoTemplate.find(query, clazz)
            if (pageEntities.isNotEmpty()) {
                entities.addAll(pageEntities)
            }
        }
        return entities
    }

    private fun <T : CommonEntity> getSnapshotByTaskIdAndToolNameAndBuildId(
        taskId: Long,
        toolName: String,
        buildId: String,
        clazz: Class<out T>
    ): List<T> {
        val query = Query.query(
            Criteria.where("task_id").`is`(taskId)
                    .and("tool_name").`is`(toolName).and("build_id").`is`(buildId)
        )
        return defectMongoTemplate.find(query, clazz)
    }

    private fun <T : CommonEntity> doSaveSbomSnapshot(
        taskId: Long,
        toolName: String,
        buildId: String,
        entities: List<T>,
        clazz: Class<out T>
    ) {
        if (entities.isEmpty()) {
            return
        }
        entities.forEach { it.applyAuditInfoOnCreate() }
        val query = Query.query(
            Criteria.where("task_id").`is`(taskId)
                    .and("tool_name").`is`(toolName).and("build_id").`is`(buildId)
        )
        val count = defectMongoTemplate.count(query, clazz)
        if (count > 0) {
            val pageEntities = Lists.partition(entities, ComConstants.COMMON_NUM_10000)
            for (pageEntity in pageEntities) {
                defectMongoTemplate.insertAll(pageEntity)
            }
        } else {
            // 兼容重试的场景, 先删除在插入
            defectMongoTemplate.remove(query, clazz)
            val pageEntities = Lists.partition(entities, ComConstants.COMMON_NUM_10000)
            for (pageEntity in pageEntities) {
                defectMongoTemplate.insertAll(pageEntity)
            }
        }
    }

    private fun <T : CommonEntity> doSaveSbomEntity(taskId: Long, entities: List<T>, clazz: Class<out T>) {
        if (entities.isEmpty()) {
            return
        }
        val inserts = entities.filter { it.entityId.isNullOrEmpty() }
        if (inserts.isNotEmpty()) {
            inserts.forEach { it.applyAuditInfoOnCreate() }
            val pageInsert = Lists.partition(inserts, ComConstants.COMMON_NUM_10000)
            for (pageEntities in pageInsert) {
                defectMongoTemplate.insertAll(pageEntities)
            }
        }
        val replaces = entities.filter { !it.entityId.isNullOrEmpty() }
        if (replaces.isNotEmpty()) {
            replaces.forEach { it.applyAuditInfoOnUpdate() }
            val pageReplaces = Lists.partition(replaces, ComConstants.COMMON_NUM_10000)
            for (pageEntities in pageReplaces) {
                val ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz)
                for (entity in pageEntities) {
                    val query = Query.query(
                        Criteria.where("task_id").`is`(taskId)
                                .and("_id").`is`(ObjectId(entity.entityId))
                    )
                    ops.replaceOne(query, entity, FindAndReplaceOptions.options().upsert())
                }
                ops.execute()
            }
        }
    }
}
