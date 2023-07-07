plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-service"))
    api(project(":core:common:common-web"))
    api(project(":core:common:common-auth"))
    api(project(":core:common:common-client"))
    api(project(":core:common:common-redis"))
    api(project(":core:common:common-storage"))
    api(project(":core:defect:model-defect"))
    api(project(":core:defect:api-defect"))
    api(project(":core:task:api-task"))
    api(project(":core:quartz:api-quartz"))
    api(project(":core:common:common-auth:common-auth-api"))
    api(project(":core:schedule:api-schedule"))
    api(project(":core:defect:biz-defect-base"))
    api("org.apache.httpcomponents:httpclient:4.5.2")
    api("org.redisson:redisson")
    api(group = "com.tencent.bk.devops.ci.process", name = "api-process") {
        isChanging = true
    }
}
