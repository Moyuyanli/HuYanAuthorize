@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import net.mamoe.mirai.console.gradle.wrapNameWithPlatform


plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"

    id("org.jetbrains.dokka") version "1.8.10"
    id("com.github.gmazzo.buildconfig") version "3.1.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "cn.chahuyun"
version = "1.2.4"

repositories {
    maven("https://nexus.jsdu.cn/repository/maven-public/")

//    mavenCentral()
//    maven("https://maven.aliyun.com/repository/public")
}

dependencies {
    implementation("cn.chahuyun:hibernate-plus:1.0.16")
    implementation("cn.hutool:hutool-core:5.8.22")
}

// hibernate 6 和 HikariCP 5 需要 jdk11
mirai {
    jvmTarget = JavaVersion.VERSION_11
}

nexusPublishing {
    repositories {
        create("sonatype") { // 对于2021年2月24日之后注册的用户
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(project.findProperty("sonatypeUsername") as? String ?: System.getenv("SONATYPE_USERNAME"))
            password.set(project.findProperty("sonatypePassword") as? String ?: System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

tasks {
    //打包mirai插件
    create<net.mamoe.mirai.console.gradle.BuildMiraiPluginV2>("pluginJar") {
        group = "mirai"
        registerMetadataTask(
            this@tasks,
            "miraiPublicationPrepareMetadata".wrapNameWithPlatform(kotlin.target, true)
        )
        init(kotlin.target)
        destinationDirectory.value(
            project.layout.projectDirectory.dir(project.buildDir.name).dir("mirai")
        )
        archiveExtension.set("mirai2.jar")
    }
    //打包javadoc
    register<Jar>("dokkaJavadocJar") {
        group = "documentation"
        dependsOn(dokkaJavadoc)
        from(dokkaJavadoc.flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }
}

//上传额外内容
setupMavenCentralPublication {
    artifact(tasks.kotlinSourcesJar)
    artifact(tasks["pluginJar"])
    artifact(tasks["dokkaJavadocJar"])
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