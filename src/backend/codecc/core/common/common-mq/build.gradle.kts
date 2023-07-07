plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-api"))
    api(project(":core:common:common-util"))
    api("org.springframework.amqp:spring-amqp")
    api("org.springframework.amqp:spring-rabbit")
}
