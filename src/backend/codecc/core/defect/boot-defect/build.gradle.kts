dependencies {
    api(project(":core:defect:biz-defect-migration"))
    api(project(":core:defect:biz-defect-bkcheck"))
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-starter-undertow")

    api(group = "com.tencent.bk.devops.ci.repository", name = "api-repository"){
        isChanging=true
    }
}
