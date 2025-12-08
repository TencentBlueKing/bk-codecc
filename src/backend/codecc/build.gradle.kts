import com.tencent.devops.enums.AssemblyMode
import com.tencent.devops.utils.findPropertyOrEmpty
import kotlin.streams.toList

plugins {
    id("com.tencent.devops.boot") version "1.1.0"
}


val devopsDependencies = mapOf(
    "com.tencent.bk.devops.ci.common" to listOf(
        "common-pipeline",
        "common-redis",
        // "common-auth-provider",  // 已移除：项目未实际使用，认证功能由本地 common-auth 模块提供
        "common-kafka",
        "common-api",
        "common-web",
        "common-scm",
        "common-codecc"
    ),
    "com.tencent.bk.devops.ci.auth" to listOf("api-auth"),
    "com.tencent.bk.devops.ci.project" to listOf("api-project"),
    "com.tencent.bk.devops.ci.process" to listOf("api-process"),
    "com.tencent.bk.devops.ci.log" to listOf("api-log"),
    "com.tencent.bk.devops.ci.metrics" to listOf("api-metrics"),
    "com.tencent.bk.devops.ci.quality" to listOf("api-quality"),
    "com.tencent.bk.devops.ci.repository" to listOf("api-repository"),
    "com.tencent.bk.devops.ci.notify" to listOf("api-notify"),
    "com.tencent.bk.devops.ci.misc" to listOf("api-image", "api-plugin")
).entries.stream().map { entry -> entry.value.map { "${entry.key}:$it:${Versions.devopsVersion}" }.toList() }
        .toList().flatten()

allprojects {
    group = "com.tencent.bk.codecc"
    version = "1.7.37-RELEASE"

    apply(plugin = "com.tencent.devops.boot")

    val mavenRepoUrl = project.findPropertyOrEmpty("MAVEN_REPO_URL")
    repositories {
        mavenLocal()
        maven(url = mavenRepoUrl)
        if (System.getenv("GITHUB_WORKFLOW") == null) { // 普通环境
            maven(url = "https://mirrors.tencent.com/nexus/repository/maven-public")
            maven(url = "https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
        } else { // GitHub Action 环境
//            maven {
//                name = "MavenSnapshot"
//                url = java.net.URI("https://oss.sonatype.org/content/repositories/snapshots/")
//                mavenContent {
//                    snapshotsOnly()
//                }
//            }
            mavenCentral()
            gradlePluginPortal()
        }
        mavenCentral()
    }

    // 版本管理
    dependencyManagement {
        imports {
            devopsDependencies.forEach { devopsDependency ->
                mavenBom(devopsDependency)
            }
        }
        setApplyMavenExclusions(false)
        dependencies {
            dependency("com.tencent.bk.sdk:spring-boot-bk-audit-starter:${Versions.bkAudit}")
            dependency("org.hashids:hashids:${Versions.hashidsVersion}")
            dependency("jakarta.ws.rs:jakarta.ws.rs-api:${Versions.jaxrsVersion}")
            dependency("org.tmatesoft.svnkit:svnkit:${Versions.svnkitVersion}")
            dependency("com.squareup.okhttp3:okhttp:${Versions.okHttpVersion}")
            dependency("org.apache.httpcomponents:httpclient:${Versions.httpclientVersion}")
            dependency("org.apache.commons:commons-exec:${Versions.commonExecVersion}")
            dependency("org.apache.commons:commons-pool2:${Versions.commonPool2Version}")  // 添加：Jedis 连接池需要
            dependency("redis.clients:jedis:${Versions.jedisVersion}")  // 添加：Jedis Redis 客户端
            dependency("com.vmware:vijava:${Versions.vmwareVersion}")

            dependency("org.dom4j:dom4j:${Versions.dom4jVersion}")  // groupId 从 dom4j 改为 org.dom4j (2.x 版本)
            dependency("org.apache.commons:commons-compress:${Versions.compressVersion}")
            dependency("org.reflections:reflections:${Versions.reflectionsVersion}")
            dependency("com.github.fge:json-schema-validator:${Versions.jsonSchemaVersion}")
            //dependency "com.github.ulisesbocchio:jasypt-spring-boot-starter:$jasyptVersion"
            dependency("org.jolokia:jolokia-core:${Versions.jolokiaVersion}")
            dependency("org.projectlombok:lombok:${Versions.lombokVersion}")
            // dependency("org.apache.tomcat.embed:tomcat-embed-core:${Versions.tomcatEmbedCoreVersion}")  // 移除：让 Spring Boot 3 自动管理
            dependency("commons-collections:commons-collections:${Versions.commonCollection}")
            // Lettuce 由 Spring Boot 自动管理，无需手动定义版本
            // dependency("io.lettuce:lettuce-core:${Versions.lettuceVersion}")
            dependency("org.glassfish.jersey.ext:jersey-bean-validation:${Versions.jerseyValidationVersion}")
            dependency("commons-io:commons-io:${Versions.commonsIOVersion}")
            dependency("org.apache.xmlrpc:xmlrpc-client:${Versions.xmlrpcVersion}")
            dependency("commons-httpclient:commons-httpclient:${Versions.commonsHttpclientVersion}")
            dependency("com.alibaba:easyexcel:${Versions.easyexcel}")
            dependency("org.redisson:redisson:${Versions.redisson}")
            // dependency("com.alibaba:fastjson:${Versions.fastjson}")  // 已废弃：存在安全漏洞，使用 fastjson2 替代
            dependency("com.alibaba.fastjson2:fastjson2:${Versions.fastjson2}")  // 升级：支持 Jakarta EE, 修复安全问题
            dependency("org.quartz-scheduler:quartz:${Versions.quartz}")
            dependency("org.quartz-scheduler:quartz-jobs:${Versions.quartzJobs}")
            dependencySet("org.bouncycastle:${Versions.bouncyCastleVersion}") {
                entry("bcprov-jdk18on")  // 升级：从 jdk15on，JDK 17+ 兼容
                entry("bcpkix-jdk18on")  // 添加：PEMParser 需要此模块
            }
            dependencySet("org.jetbrains.kotlin:${Versions.Kotlin}") {
                entry("kotlin-stdlib-jdk8")
                entry("kotlin-reflect")
            }
            dependencySet("io.swagger.core.v3:${Versions.swaggerVersion}") {
                entry("swagger-annotations-jakarta")
                entry("swagger-jaxrs2-jakarta")
                entry("swagger-models-jakarta")
            }
            dependencySet("io.github.openfeign:${Versions.feignVersion}") {
                entry("feign-jaxrs3")  // 支持 Jakarta EE (jakarta.ws.rs)
                entry("feign-okhttp")
                entry("feign-jackson")
            }
            dependencySet("org.slf4j:${Versions.slf4jVersion}") {
                entry("slf4j-api")
                entry("slf4j-simple")
            }
            // JJWT 0.11.x 需要拆分为多个模块
            dependency("io.jsonwebtoken:jjwt-api:${Versions.jjwtVersion}")
            dependency("io.jsonwebtoken:jjwt-impl:${Versions.jjwtVersion}")
            dependency("io.jsonwebtoken:jjwt-jackson:${Versions.jjwtVersion}")

            dependencySet("org.mockito:${Versions.mockitoVersion}") {
                entry("mockito-core")  // 升级：mockito-all 在 5.x 中不存在
            }
            dependencySet("net.sf.json-lib:${Versions.jsonLibVersion}") {
                entry("json-lib")
            }
            dependencySet("com.cronutils:${Versions.cronutilsVersion}") {
                entry("cron-utils")
            }
            dependencySet("ch.qos.logback:${Versions.logbackVersion}") {
                entry("logback-core")
                entry("logback-classic")
            }
            dependencySet("com.amazonaws:${Versions.awsS3Version}") {
                entry("aws-java-sdk-s3")
            }
            dependencySet("org.apache.poi:${Versions.poiVersion}") {
                entry("poi")
                entry("poi-ooxml")
            }
            dependencySet("org.apache.poi:${Versions.poiVersion}") {
                entry("poi")
                entry("poi-ooxml")
            }
            dependencySet("org.apache.logging.log4j:${Versions.log4jVersion}") {
                entry("log4j-api")
                entry("log4j-core")
                entry("log4j-slf4j-impl")
            }
            dependencySet("io.micrometer:${Versions.micrometerVersion}") {
                // micrometer-jersey2 在 1.10+ 中已移除，使用 micrometer-core 替代
                entry("micrometer-core")
                entry("micrometer-registry-prometheus")
            }
            /**
             * 蓝盾依赖
             */
            devopsDependencies.forEach { devopsDependency ->
                dependency(devopsDependency)
            }

            dependency("io.opentelemetry:opentelemetry-api:${Versions.opentelemetryVersion}")
            dependency("com.esotericsoftware:reflectasm:${Versions.reflectasmVersion}")
            dependency("com.tencent.bk.sdk:iam-java-sdk:${Versions.iamSdkVersion}")
            dependency("com.tencent.bk.sdk:crypto-java-sdk:${Versions.cryptSdkVersion}")
            dependencySet("org.jetbrains.kotlinx:${Versions.kotlinxVersion}") {
                entry("kotlinx-coroutines-core-jvm")
                entry("kotlinx-coroutines-core")
                entry("kotlinx-coroutines-jdk8")
                entry("kotlinx-coroutines-slf4j")
                entry("kotlinx-coroutines-test")
            }

            dependency("com.qcloud:cos_api:${Versions.cosVersion}")
            dependency("org.tukaani:xz:${Versions.xzVersion}")
            dependency("org.json:json:${Versions.jsonVersion}")
        }
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        implementation("org.springframework.boot:spring-boot-starter-jersey")
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
        implementation("org.glassfish.jersey.media:jersey-media-multipart")
        implementation("org.glassfish.jersey.ext:jersey-bean-validation")
        testImplementation(group = "com.github.shyiko", name = "ktlint", version = Versions.ktlintVersion)
        testImplementation("junit:junit")
        testImplementation("org.mockito:mockito-core")  // 升级：mockito-all 在 5.x 中不存在
        testImplementation("com.nhaarman:mockito-kotlin-kt1.1:${Versions.mockitoKotlinVersion}")
    }

    // 配置 Java 编译参数，保留参数名信息用于 Spring 依赖注入
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    val property = project.findPropertyOrEmpty("devops.assemblyMode").trim()
    configurations.forEach {
        it.exclude("org.springframework.boot", "spring-boot-starter-logging")
        it.exclude("org.springframework.boot", "spring-boot-starter-tomcat")
        it.exclude("org.apache.tomcat", "tomcat-jdbc")
        it.exclude("org.slf4j", "log4j-over-slf4j")
        it.exclude("org.slf4j", "slf4j-log4j12")
        it.exclude("org.slf4j", "slf4j-nop")
        it.exclude("ch.qos.logback", "logback-classic")
        it.exclude("commons-logging", "commons-logging")  // 排除 commons-logging，避免与 spring-jcl 冲突
        it.exclude("redis.clients", "jedis")  // 排除 Jedis Redis 客户端，使用 Lettuce
        it.exclude("org.apache.commons", "commons-pool2")  // 排除 Jedis 依赖的连接池
        // 排除旧的 JAX-RS API (javax.ws.rs)，使用 Jakarta EE 版本 (jakarta.ws.rs)
        it.exclude("javax.ws.rs", "javax.ws.rs-api")
        it.exclude("javax.ws.rs", "jsr311-api")
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-archive-tencent")
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-archive")
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-client")
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-service")
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-web")
        it.exclude(group = "org.bouncycastle", module = "bcprov-jdk16")
        it.exclude(group = "com.github.ulisesbocchio", module = "jasypt-spring-boot-starter")
        // 移除旧版 jdk15on 模块的 exclude，现在使用 jdk18on
        it.exclude("org.bouncycastle", "bcprov-jdk15on")
        it.exclude("org.bouncycastle", "bcprov-ext-jdk15on")
        it.exclude("org.bouncycastle", "bcutil-jdk15on")
        it.exclude("org.bouncycastle", "bcpkix-jdk15on")
        if (project.name.contains("biz-codeccjob") && project.name != "boot-codeccjob") {
            it.exclude("io.undertow", "undertow-websockets-jsr")
        }
        if (project.name.contains("boot-idcsync-tencent")) {
            it.exclude("com.tencent.bk.devops.ci.common", "common-web")
        }
        if (project.name.startsWith("boot-")) {
            when (AssemblyMode.ofValueOrDefault(property)) {
                AssemblyMode.CONSUL -> {
                    it.exclude("org.springframework.cloud", "spring-cloud-starter-kubernetes-client")
                    it.exclude("org.springframework.cloud", "spring-cloud-starter-kubernetes-client-config")
                }

                AssemblyMode.K8S, AssemblyMode.KUBERNETES -> {
                    it.exclude("org.springframework.cloud", "spring-cloud-starter-config")
                    it.exclude("org.springframework.cloud", "spring-cloud-starter-consul-config")
                    it.exclude("org.springframework.cloud", "spring-cloud-starter-consul-discovery")
                }

                else -> {}
            }
        }
    }
}





