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
    basePackages = ["com.tencent.bk.codecc.codeccjob.dao.codeccjob"],
    mongoTemplateRef = CodeccjobDBMongoConfig.MONGO_TEMPLATE
)
class CodeccjobDBMongoConfig {
    companion object {
        const val MONGO_TEMPLATE = "codeccjobMongoTemplate"
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.data.mongodb.codeccjob")
    fun codeccjobMongoProperties() = CodeCCMongoProperties()

    @Bean
    fun codeccjobMongoDbFactory(codeccjobMongoProperties: CodeCCMongoProperties) =
        SimpleMongoClientDatabaseFactory(codeccjobMongoProperties.uri!!)

    @Bean
    fun codeccjobMappingMongoConverter(
        codeccjobMongoDbFactory: SimpleMongoClientDatabaseFactory,
        mongoMappingContext: MongoMappingContext
    ): MappingMongoConverter {
        val dbRefResolver = DefaultDbRefResolver(codeccjobMongoDbFactory)
        return MappingMongoConverter(dbRefResolver, mongoMappingContext)
    }

    @Bean(MONGO_TEMPLATE)
    fun mongoTemplate(
        codeccjobMongoDbFactory: SimpleMongoClientDatabaseFactory,
        codeccjobMappingMongoConverter: MappingMongoConverter
    ) = MongoTemplate(codeccjobMongoDbFactory, codeccjobMappingMongoConverter)
}
