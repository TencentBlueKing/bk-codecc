import com.tencent.devops.enums.AssemblyMode
import com.tencent.devops.utils.findPropertyOrEmpty
import kotlin.streams.toList

plugins {
    id("com.tencent.devops.boot") version "0.0.7"
}


val devopsDependencies = mapOf(
    "com.tencent.bk.devops.ci.common" to listOf(
        "common-pipeline",
        "common-redis",
        "common-auth-v3",
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
    "com.tencent.bk.devops.ci.quality" to listOf("api-quality"),
    "com.tencent.bk.devops.ci.repository" to listOf("api-repository"),
    "com.tencent.bk.devops.ci.notify" to listOf("api-notify"),
    "com.tencent.bk.devops.ci.image" to listOf("api-image"),
    "com.tencent.bk.devops.ci.plugin" to listOf("api-plugin")
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
        jcenter()
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
            dependency("org.hashids:hashids:${Versions.hashidsVersion}")
            dependency("javax.ws.rs:javax.ws.rs-api:${Versions.jaxrsVersion}")
            dependency("org.tmatesoft.svnkit:svnkit:${Versions.svnkitVersion}")
            dependency("com.squareup.okhttp3:okhttp:${Versions.okHttpVersion}")
            dependency("org.apache.httpcomponents:httpclient:${Versions.httpclientVersion}")
            dependency("org.apache.commons:commons-exec:${Versions.commonExecVersion}")
//            dependency("org.apache.commons:commons-pool2:${Versions.commonPool2Version}")
            dependency("com.vmware:vijava:${Versions.vmwareVersion}")

            dependency("dom4j:dom4j:${Versions.dom4jVersion}")
            dependency("org.apache.commons:commons-compress:${Versions.compressVersion}")
            dependency("org.reflections:reflections:${Versions.reflectionsVersion}")
            dependency("com.github.fge:json-schema-validator:${Versions.jsonSchemaVersion}")
            //dependency "com.github.ulisesbocchio:jasypt-spring-boot-starter:$jasyptVersion"
            dependency("org.jolokia:jolokia-core:${Versions.jolokiaVersion}")
            dependency("org.projectlombok:lombok:${Versions.lombokVersion}")
            dependency("org.apache.tomcat.embed:tomcat-embed-core:${Versions.tomcatEmbedCoreVersion}")
            dependency("commons-collections:commons-collections:${Versions.commonCollection}")
            dependency("biz.paluch.redis:lettuce:${Versions.lettuceVersion}")
            dependency("org.glassfish.jersey.ext:jersey-bean-validation:${Versions.jerseyValidationVersion}")
            dependency("commons-io:commons-io:${Versions.commonsIOVersion}")
            dependency("org.apache.xmlrpc:xmlrpc-client:${Versions.xmlrpcVersion}")
            dependency("commons-httpclient:commons-httpclient:${Versions.commonsHttpclientVersion}")
            dependency("com.alibaba:easyexcel:${Versions.easyexcel}")
            dependency("org.redisson:redisson:${Versions.redisson}")
            dependency("com.alibaba:fastjson:${Versions.fastjson}")
            dependency("org.quartz-scheduler:quartz:${Versions.quartz}")
            dependency("org.quartz-scheduler:quartz-jobs:${Versions.quartzJobs}")
            dependencySet("org.bouncycastle:${Versions.bouncyCastleVersion}") {
                entry("bcprov-jdk15on")
                entry("bcprov-ext-jdk15on")
            }
            dependencySet("org.jetbrains.kotlin:${Versions.Kotlin}") {
                entry("kotlin-stdlib-jdk8")
                entry("kotlin-reflect")
            }
            dependencySet("io.swagger:${Versions.swaggerVersion}") {
                entry("swagger-annotations")
                entry("swagger-jersey2-jaxrs")
                entry("swagger-models")
                entry("swagger-core")
                entry("swagger-jaxrs")
            }
            dependencySet("io.github.openfeign:${Versions.feignVersion}") {
                entry("feign-jaxrs")
                entry("feign-okhttp")
                entry("feign-jackson")
            }
            dependencySet("org.slf4j:${Versions.slf4jVersion}") {
                entry("slf4j-api")
                entry("slf4j-simple")
            }
            dependencySet("io.jsonwebtoken:${Versions.jjwtVersion}") {
                entry("jjwt")
            }

            dependencySet("org.mockito:${Versions.mockitoVersion}") {
                entry("mockito-all")
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
                entry("micrometer-jersey2")
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
        testImplementation("org.mockito:mockito-all")
        testImplementation("com.nhaarman:mockito-kotlin-kt1.1:1.6.0")
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
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-archive-tencent")
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-archive")
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-client")
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-service")
        it.exclude(group = "com.tencent.bk.devops.ci.common", module = "common-web")
        it.exclude(group = "org.bouncycastle", module = "bcprov-jdk16")
        it.exclude(group = "com.github.ulisesbocchio", module = "jasypt-spring-boot-starter")
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
            }
        }
    }
}




