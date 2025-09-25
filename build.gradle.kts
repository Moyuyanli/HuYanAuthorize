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
//    id("moe.karla.maven-publishing") version "1.3.1"
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

//mavenPublishing {
//    // 设置成手动发布（运行结束后要到 Central 确认发布），如果要自动发布，就用 AUTOMATIC
//    publishingType = PublishingType.USER_MANAGED
//    // 改成你自己的信息
//    url = "https://github.com/用户名/仓库"
//    developer("用户名", "邮箱")
//}


// 配置发布
//afterEvaluate {
//    publishing {
//        publications {
//            create<MavenPublication>("mavenJava") {
//                from(components["java"])
//                // 手动指定主 JAR 文件
//                artifact(tasks.named("buildPlugin"))
//
//                // 添加源码和文档
//                artifact(sourcesJar)
//                artifact(javadocJar)
//
//                // 手动配置 POM 元数据
//                pom {
//                    name.set("HuYan Authorize Plugin")
//                    description.set("Mirai plugin for advanced authorization features")
//                    url.set("https://github.com/Moyuyanli/HuYanAuthorize")
//
//                    licenses {
//                        license {
//                            name.set("MIT License")
//                            url.set("https://opensource.org/licenses/MIT")
//                        }
//                    }
//
//                    developers {
//                        developer {
//                            id.set("moyuyanli")
//                            name.set("Moyu yanli")
//                            url.set("https://github.com/Moyuyanli")
//                        }
//                    }
//
//                    scm {
//                        connection.set("scm:git:git://github.com/Moyuyanli/HuYanAuthorize.git")
//                        developerConnection.set("scm:git:ssh://github.com/Moyuyanli/HuYanAuthorize.git")
//                        url.set("https://github.com/Moyuyanli/HuYanAuthorize")
//                    }
//                }
//            }
//        }
//    }
//
////    nexusPublishing {
////        repositories {
////            //旧仓库
//////            sonatype()
//////            //自定义仓库，用户和密码是xxxUsername和xxxPassword
//////            create("central") {
//////                nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
//////                snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
//////            }
////            sonatype {
////                nexusUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
////                snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
////            }
////        }
////    }
//
//    signing {
//        useGpgCmd()
////        sign(publishing.publications["mavenJava"])
//    }
//}
