plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-api"))

    api(group = "org.slf4j", name = "slf4j-api")
    api("com.tencent.bk.sdk:spring-boot-bk-audit-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter")
}
