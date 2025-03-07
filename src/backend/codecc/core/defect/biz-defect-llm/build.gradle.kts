plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.21"
}

dependencies {
    api(project(":core:common:common-auth:common-auth-api"))
    api(project(":core:common:common-service"))
    api(project(":core:defect:biz-defect"))
    api(project(":core:defect:api-defect"))
    api(project(":core:defect:api-defect-llm"))
    api(project(":core:defect:model-defect-llm"))

    api("io.ktor:ktor-client-core-jvm:2.2.1")
//    api("io.ktor:ktor-client-core-jvm:2.3.2")
    api("io.ktor:ktor-client-okhttp:2.2.1")
    api("io.ktor:ktor-network:2.2.1")
    api("io.ktor:ktor-client-logging:2.2.1")
    api("io.ktor:ktor-client-auth:2.2.1")
    api("io.ktor:ktor-client-content-negotiation:2.2.1")
    api("io.ktor:ktor-serialization-kotlinx-json-jvm:2.2.1")

    api(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core"){
        isChanging=true
    }
    api(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core-jvm"){
        isChanging=true
    }
    api("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    api("org.springframework.boot:spring-boot-starter-websocket")
    api(group="javax.websocket", name="javax.websocket-api", version= "1.1")
}