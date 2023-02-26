plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-api"))
    api(project(":core:common:common-redis"))
    api(project(":core:common:common-mq"))
    api(project(":core:common:common-util"))
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-log4j2")
    api("org.springframework.cloud:spring-cloud-commons")
    api("io.github.openfeign:feign-okhttp")
    api("org.jolokia:jolokia-core")
    api("org.projectlombok:lombok")
    api("io.micrometer:micrometer-registry-prometheus")
    api("io.micrometer:micrometer-jersey2")
    api("org.springframework.boot:spring-boot-starter-aop")
    api("io.opentelemetry:opentelemetry-api")
    api("com.esotericsoftware:reflectasm")
}
