plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-util"))
    api("jakarta.ws.rs:jakarta.ws.rs-api")
    api("io.swagger.core.v3:swagger-annotations-jakarta") {
        exclude(group = "org.json", module = "json")
    }
    api("org.hashids:hashids")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-json-provider")
    api("com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-base")
    api("org.bouncycastle:bcprov-jdk18on")  // 升级：JDK 17+ 兼容
    api("org.bouncycastle:bcpkix-jdk18on")  // 升级：PEMParser 需要此模块
    api("com.squareup.okhttp3:okhttp")
    api("commons-codec:commons-codec:${Versions.commonsCodecVersion}")
    api("org.projectlombok:lombok")
    api("org.glassfish.jersey.media:jersey-media-multipart")
}

