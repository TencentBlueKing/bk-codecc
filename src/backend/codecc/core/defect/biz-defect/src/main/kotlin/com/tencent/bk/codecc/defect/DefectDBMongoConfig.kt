package com.tencent.bk.codecc.defect

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
    basePackages = ["com.tencent.bk.codecc.defect.dao.defect"],
    mongoTemplateRef = DefectDBMongoConfig.MONGO_TEMPLATE
)
class DefectDBMongoConfig {

    companion object {
        const val MONGO_TEMPLATE = "defectMongoTemplate"
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.data.mongodb.defect.db")
    fun defectMongoProperties() = CodeCCMongoProperties()

    @Bean
    fun defectMongoDbFactory(defectMongoProperties: CodeCCMongoProperties) =
        SimpleMongoClientDatabaseFactory(defectMongoProperties.uri!!)

    @Bean
    fun defectMappingMongoConverter(
        defectMongoDbFactory: SimpleMongoClientDatabaseFactory,
        mongoMappingContext: MongoMappingContext
    ): MappingMongoConverter {
        val dbRefResolver = DefaultDbRefResolver(defectMongoDbFactory)
        return MappingMongoConverter(dbRefResolver, mongoMappingContext)
    }

    @Bean(MONGO_TEMPLATE)
    fun mongoTemplate(
        defectMongoDbFactory: SimpleMongoClientDatabaseFactory,
        defectMappingMongoConverter: MappingMongoConverter
    ) =
        MongoTemplate(defectMongoDbFactory, defectMappingMongoConverter)
}
