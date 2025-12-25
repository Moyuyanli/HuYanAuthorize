@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

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
}

group = "cn.chahuyun"
version = "1.3.2"

// 提取公共 POM 配置
fun MavenPom.setupCommonMetadata() {
    name.set(project.name)
    description.set("一个用于针对简化mirai插件开发的前置插件")
    url.set("https://github.com/moyuyanli/HuYanAuthorize")
    licenses {
        license {
            name.set("Apache-2.0 License")
            url.set("https://www.apache.org/licenses/LICENSE-2.0")
        }
    }
    developers {
        developer {
            id.set("moyuyanli")
            name.set("moyuyanli")
            email.set("572490972@qq.com")
        }
    }
    scm {
        connection.set("scm:git:github.com/moyuyanli/HuYanAuthorize.git")
        developerConnection.set("scm:git:ssh://github.com/moyuyanli/HuYanAuthorize.git")
        url.set("https://github.com/moyuyanli/HuYanAuthorize")
    }
}

dependencies {
    compileOnly("top.mrxiaom.mirai:overflow-core-api:1.0.8")

    implementation("cn.chahuyun:hibernate-plus:2.0.1")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

// hibernate 6 和 HikariCP 5 需要 jdk11
mirai {
    jvmTarget = JavaVersion.VERSION_11
}

java {
    // 移除这里，改用下方手动定义的任务以保持对旧版 Mirai 插件结构的兼容
    // withSourcesJar()
    // withJavadocJar()
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

afterEvaluate {
    publishing {
        publications.create<MavenPublication>("mavenJava") {
            // mirai-console 插件的核心构建任务
            artifact(tasks.named("buildPlugin"))

            artifact(sourcesJar)
            artifact(javadocJar)

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                setupCommonMetadata()
            }
        }

        repositories {
            maven {
                name = "local"
                url = file("local").toURI()
            }
        }
    }

    signing {
        // 如果本地安装了 GPG，可以使用 gpg 命令签名
         useGpgCmd()
        // 如果没有 GPG 环境，先注释掉 sign 任务，手动上传时在 Nexus 后台可能可以处理，
        // 但通常 Central 要求上传前必须签名。建议本地安装 GPG 并配置。
//         sign(publishing.publications["mavenJava"])


//        // 从项目属性中获取PGP签名配置信息
//        val signingKey = project.findProperty("signing.secretKey") as String?
//        val signingPassword = project.findProperty("signing.password") as String?
//        val signingKeyId = project.findProperty("signing.keyId") as String?
//
//        // 使用获取到的密钥信息配置内存中的PGP签名环境
//        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

        // 对Maven Java发布进行数字签名
        sign(publishing.publications["mavenJava"])
    }
}


