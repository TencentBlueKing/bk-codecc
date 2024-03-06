plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-util"))
    api("javax.ws.rs:javax.ws.rs-api")
    api("io.swagger:swagger-annotations") {
        exclude(group = "org.json", module = "json")
    }
    api("org.hashids:hashids")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider")
    api("com.fasterxml.jackson.jaxrs:jackson-jaxrs-base")
    api("org.bouncycastle:bcprov-jdk15on")
    api("org.bouncycastle:bcprov-ext-jdk15on")
    api("com.squareup.okhttp3:okhttp")
    api("commons-codec:commons-codec:1.9")
    api("org.projectlombok:lombok")
    api("org.glassfish.jersey.media:jersey-media-multipart")

    api("com.vdurmont:emoji-java")
    api("org.apache.commons:commons-csv")
    api("org.apache.lucene:lucene-core")
    api("com.perforce:p4java")
    api("com.github.taptap:pinyin-plus")
    api("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch")
    api("org.apache.commons:commons-text")
    api("com.tencent.bk.repo:api-generic")
    api("com.tencent.bk.repo:api-repository")
    api("com.tencent.bk.repo:api-webhook")
    api("org.apache.ant:ant")

}

