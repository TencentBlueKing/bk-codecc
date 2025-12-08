dependencies {
    api(project(":core:common:common-db"))
    api("org.projectlombok:lombok")
    api("com.fasterxml.jackson.core:jackson-annotations")
    // api("com.alibaba:fastjson")  // 已废弃：使用 fastjson2
    api("com.alibaba.fastjson2:fastjson2")  // 升级到 Fastjson2
    api(group = "org.json", name = "json", version = Versions.jsonVersion)
}
