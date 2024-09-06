plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
    id("com.github.gmazzo.buildconfig") version "3.1.0"
}

group = "cn.chahuyun"
version = "1.1.6 "

repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public")
}

dependencies {
    implementation("cn.chahuyun:hibernate-plus:1.0.16")
    implementation("cn.hutool:hutool-core:5.8.22")
}

// hibernate 6 和 HikariCP 5 需要 jdk11
mirai {
    jvmTarget = JavaVersion.VERSION_11
}

// 打包时执行
mavenCentralPublish {
    useCentralS01()
    licenseApacheV2()

    singleDevGithubProject("Moyuyanli", "HuYanAuthorize")
    licenseFromGitHubProject("AGPL-3.0", "main")

    // 设置 Publish 临时目录
    workingDir = System.getenv("PUBLICATION_TEMP")?.let { file(it).resolve(projectName) }
        ?: buildDir.resolve("publishing-tmp")

    // 设置额外上传内容
    publication {
        artifact(tasks["buildPlugin"])
    }
}

buildConfig {
    className("BuildConstants")
    packageName("cn.chahuyun.authorize")
    useKotlinOutput()
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField(
        "java.time.Instant",
        "BUILD_TIME",
        "java.time.Instant.ofEpochSecond(${System.currentTimeMillis() / 1000L}L)"
    )
}