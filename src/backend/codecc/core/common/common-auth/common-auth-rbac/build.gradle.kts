dependencies {
    api(project(":core:common:common-auth:common-auth-api"))
    api(project(":core:common:common-redis"))
    api(project(":core:common:common-client"))
    api(group = "com.tencent.bk.devops.ci.repository", name = "api-repository", version = Versions.devopsVersion)
    api(group = "com.tencent.bk.devops.ci.auth", name = "api-auth", version = Versions.devopsVersion)
}