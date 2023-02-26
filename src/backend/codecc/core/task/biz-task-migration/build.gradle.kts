plugins {
    id("com.tencent.devops.boot")
}


dependencies {
    api(project(":core:task:biz-task"))
    api(group = "com.tencent.bk.devops.ci.process", name = "api-process"){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.repository", name = "api-repository"){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.project", name = "api-project"){
        isChanging=true
        exclude(group="com.tencent.bk.devops.ci.project", module = "model-project")
        exclude(group="com.tencent.bk.devops.ci.common", module = "common-api")
        exclude(group="com.tencent.bk.devops.ci.common", module = "common-auth")
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name = "api-plugin"){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.notify", name = "api-notify"){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name = "common-codecc"){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name = "api-codecc"){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.common", name = "common-pipeline"){
        isChanging=true
    }

}
