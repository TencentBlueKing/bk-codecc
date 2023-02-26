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
    api("org.codehaus.groovy:groovy:2.5.3")
    api(group = "com.tencent.bk.devops.ci.common", name="common-redis"){
        isChanging = true
    }
}


