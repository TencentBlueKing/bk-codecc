dependencies {
    api(project(":core:common:common-api"))
    api(group = "com.tencent.bk.devops.ci.log", name = "api-log"){
        isChanging = true
    }
}

