plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-api"))
    api(project(":core:common:common-util"))
    api(project(":core:common:common-mq"))
    api("io.opentelemetry:opentelemetry-api")
}
