plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

dependencies {
    api(project(":core:common:common-auth:common-auth-api"))
    api(project(":core:common:common-service"))
    api(project(":core:defect:biz-defect"))
    api(project(":core:defect:api-defect"))
    api(project(":core:defect:api-defect-llm"))
    api(project(":core:defect:model-defect-llm"))

    api("io.ktor:ktor-client-core-jvm:${Versions.ktorVersion}")
    api("io.ktor:ktor-client-okhttp:${Versions.ktorVersion}")
    api("io.ktor:ktor-network:${Versions.ktorVersion}")
    api("io.ktor:ktor-client-logging:${Versions.ktorVersion}")
    api("io.ktor:ktor-client-auth:${Versions.ktorVersion}")
    api("io.ktor:ktor-client-content-negotiation:${Versions.ktorVersion}")
    api("io.ktor:ktor-serialization-kotlinx-json-jvm:${Versions.ktorVersion}")

    api(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core"){
        isChanging=true
    }
    api(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core-jvm"){
        isChanging=true
    }
    api("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    api("org.springframework.boot:spring-boot-starter-websocket")
    api(group="jakarta.websocket", name="jakarta.websocket-api", version= Versions.jakartaWebsocketVersion)
}
