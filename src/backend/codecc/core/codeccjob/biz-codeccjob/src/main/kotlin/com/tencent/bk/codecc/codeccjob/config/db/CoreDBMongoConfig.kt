package com.tencent.bk.codecc.codeccjob.config.db

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(
    basePackages = ["com.tencent.bk.codecc.codeccjob.dao.core"],
    mongoTemplateRef = CoreDBMongoConfig.MONGO_TEMPLATE
)
class CoreDBMongoConfig {

    companion object {
        const val MONGO_TEMPLATE = "defectCoreMongoTemplate"
    }

    @ConfigurationProperties(prefix = "spring.data.mongodb.defect.coredb")
    @Bean
    fun defectCoreMongoProperties() = CodeCCMongoProperties()

    @Bean
    fun defectCoreMongoDbFactory(defectCoreMongoProperties: CodeCCMongoProperties) =
        SimpleMongoClientDatabaseFactory(defectCoreMongoProperties.uri!!)

    @Bean
    fun mongoMappingContext() = MongoMappingContext()

    @Bean
    fun defectCoreMappingMongoConverter(
        defectCoreMongoDbFactory: SimpleMongoClientDatabaseFactory,
        mongoMappingContext: MongoMappingContext
    ): MappingMongoConverter {
        val dbRefResolver = DefaultDbRefResolver(defectCoreMongoDbFactory)
        return MappingMongoConverter(dbRefResolver, mongoMappingContext)
    }

    @Bean(MONGO_TEMPLATE)
    fun mongoTemplate(
        defectCoreMongoDbFactory: SimpleMongoClientDatabaseFactory,
        defectCoreMappingMongoConverter: MappingMongoConverter
    ) =
        MongoTemplate(defectCoreMongoDbFactory, defectCoreMappingMongoConverter)
}
