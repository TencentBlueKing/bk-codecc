dependencies {
    api(project(":core:common:common-service"))
    api(project(":core:common:common-api"))
    api(project(":core:common:common-util"))
    api("io.github.openfeign:feign-jaxrs3")  // 支持 Jakarta EE
    api("io.github.openfeign:feign-okhttp")
    api("io.github.openfeign:feign-jackson")
    api(group = "com.tencent.bk.devops.ci.common", name = "common-api"){
        isChanging=true
    }
}
