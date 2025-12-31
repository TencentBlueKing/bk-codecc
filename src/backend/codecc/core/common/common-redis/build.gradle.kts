plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-redis")  // Lettuce 会自动引入
    api("org.redisson:redisson")
    // Lettuce 已由 spring-boot-starter-data-redis 自动引入，无需手动添加
}
