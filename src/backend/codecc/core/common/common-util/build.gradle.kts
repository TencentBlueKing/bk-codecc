plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api("jakarta.ws.rs:jakarta.ws.rs-api")
    api("org.hashids:hashids")
    //注意 新加的依赖
//    api("org.glassfish.jersey.bundles.repackaged:jersey-guava")
    api("com.google.guava:guava:${Versions.guavaVersion}")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-json-provider")
    api("com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-base")
    api("org.bouncycastle:bcprov-jdk18on")  // 升级：JDK 17+ 兼容
    api("org.bouncycastle:bcpkix-jdk18on")  // 升级：PEMParser 需要此模块
    api("commons-collections:commons-collections")
    api("commons-lang:commons-lang:${Versions.commonsLangVersion}")
    api("commons-codec:commons-codec:${Versions.commonsCodecVersion}")
    api("com.google.code.gson:gson:${Versions.gsonVersion}")
    // api("com.alibaba:fastjson")  // 已废弃：使用 fastjson2
    api("com.alibaba.fastjson2:fastjson2")  // 升级到 Fastjson2
    api("com.squareup.okhttp3:okhttp")
    api("org.springframework.boot:spring-boot-starter-jersey")
    api("org.springframework.boot:spring-boot-starter-undertow")
//    api("org.springframework.boot:spring-boot-starter-web")
    api(group = "org.json", name = "json", version = Versions.jsonVersion)
    api(group = "org.slf4j", name = "slf4j-api")
    api(group = "org.apache.poi", name = "poi", version = "${Versions.poiVersion}")
    api(group = "org.apache.poi", name = "poi-ooxml", version = "${Versions.poiVersion}")
    api(group = "org.apache.commons", name = "commons-exec")
    api("com.github.ben-manes.caffeine:caffeine")
    api("com.tencent.bk.sdk:crypto-java-sdk")

    api("com.tencent.bk.sdk:spring-boot-bk-audit-starter")
}
