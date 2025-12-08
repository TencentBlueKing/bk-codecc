plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-service"))
    api(project(":core:common:common-client"))
    api(project(":core:common:common-web"))
    api(project(":core:quartz:model-quartz"))
    api(project(":core:quartz:api-quartz"))
    api(project(":core:quartz:sdk-quartz"))
    api("org.quartz-scheduler:quartz")
    api("org.quartz-scheduler:quartz-jobs")
    api("org.reflections:reflections")
    api("org.codehaus.groovy:groovy:${Versions.groovyVersion}")
    api(group = "com.tencent.bk.devops.ci.common", name="common-redis"){
        isChanging = true
        exclude(group = "redis.clients", module = "jedis")  // 排除 Jedis，使用 Lettuce
    }
}


