@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import moe.karla.maven.publishing.MavenPublishingExtension
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    val kotlinVersion = "1.9.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"

    id("org.jetbrains.dokka") version "1.8.10"
    id("com.github.gmazzo.buildconfig") version "3.1.0"


    signing
    `java-library`
    `maven-publish`
    id("moe.karla.maven-publishing") version "1.3.1"
}

group = "cn.chahuyun"
version = "1.2.4"

repositories {
    maven("https://nexus.jsdu.cn/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation("cn.chahuyun:hibernate-plus:1.0.16")
    implementation("cn.hutool:hutool-core:5.8.22")
}

// hibernate 6 和 HikariCP 5 需要 jdk11
mirai {
    jvmTarget = JavaVersion.VERSION_11
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


//  定义 Kotlin 源码 JAR
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets["main"].kotlin)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // 跳过重复文件
}

// 定义 Dokka 生成的 Javadoc JAR
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named<DokkaTask>("dokkaJavadoc"))
}

mavenPublishing {
    // 设置成手动发布（运行结束后要到 Central 确认发布），如果要自动发布，就用 AUTOMATIC
    publishingType = MavenPublishingExtension.PublishingType.USER_MANAGED
    // 改成你自己的信息
    url = "https://github.com/moyuyanli/HuYanAuthorize"
    developer("moyuyanli", "572490972@qq.com")
}
afterEvaluate {
    publishing {
        publications.create<MavenPublication>("mavenJava") {
            // 手动指定主 JAR 文件
            artifact(tasks.named("buildPlugin"))

            // 添加源码和文档
            artifact(sourcesJar)
            artifact(javadocJar)

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }

        repositories {
            maven {
                name = "CentralSnapshots"
                // kotlin
                setUrl("https://central.sonatype.com/repository/maven-snapshots/")
                // 登录仓库
                credentials {
                    // 账号密码通过 Generate User Token 获取
                    // https://central.sonatype.com/account
                    username = project.findProperty("mavenCentralUsername") as String? ?: ""
                    password = project.findProperty("mavenCentralPassword") as String? ?: ""
                }
            }

            maven {
                name = "local"
                url = file("local").toURI()
            }
        }
    }

    signing {
        useGpgCmd()
//        sign(publishing.publications["mavenJava"])
    }
}


