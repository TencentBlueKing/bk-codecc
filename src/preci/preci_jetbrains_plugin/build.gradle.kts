plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.3"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

group = "com.tencent.devops.ide.plugin"
version = findProperty("pluginVersion")?.toString() ?: "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // 日志框架
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("ch.qos.logback:logback-core:1.5.16")

    // HTTP 客户端
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON 序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // 测试框架
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.6")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("org.jetbrains.plugins.terminal", "Git4Idea"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set(provider { null })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // 配置测试任务使用 JUnit Platform
    test {
        useJUnitPlatform()
    }
}

// Detekt 静态代码检查配置
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt-config.yml")
    baseline = file("$projectDir/detekt-baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(true)
    }
}

tasks.named("buildPlugin") {
    dependsOn.removeIf { it.toString().contains("detekt") }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    onlyIf { gradle.startParameter.taskNames.any { it.contains("detekt") } }
}
