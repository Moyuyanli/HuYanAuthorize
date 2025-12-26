@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.mirai.console)

    alias(libs.plugins.dokka)
    alias(libs.plugins.buildconfig)

    signing
    `java-library`
    `maven-publish`

    alias(libs.plugins.ksp)
}

group = "cn.chahuyun"
version = rootProject.version

// 产物命名保持对外兼容：即使模块名叫 auth-core，生成的 jar / mirai2.jar 也使用 HuYanAuthorize
base {
    archivesName.set("HuYanAuthorize")
}

// 将 auth-api 的源码直接编译进 auth-core（最终也会打进 HuYanAuthorize 的 jar / sourcesJar）
kotlin {
    jvmToolchain(11)
    sourceSets {
        main {
            kotlin.srcDir(rootProject.file("auth-api/src/main/kotlin"))
        }
    }
}

// 统一 JVM 目标版本，避免 compileJava(17)/compileKotlin(1.8) 不一致警告（Gradle 8+ 会变为错误）
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

dependencies {
    compileOnly(libs.overflow.core.api)
    // auth-api 的注解引用了 mirai 的 EventPriority/ConcurrencyKind 等类型，这里需要提供编译期依赖
    compileOnly(libs.mirai.core.api)

    ksp(project(":auth-ksp"))

    implementation(libs.hibernate.plus)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

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

tasks.test {
    useJUnitPlatform()
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

// 定义 Kotlin 源码 JAR
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets["main"].kotlin)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// 定义 Dokka 生成的 Javadoc JAR
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named<DokkaTask>("dokkaJavadoc"))
}

// mirai-console 的 buildPlugin 任务默认用 project.name 作为文件名：
// 这里用惰性匹配的方式配置，避免在 task 尚未创建时 tasks.named("buildPlugin") 直接报错
tasks.withType<Jar>().configureEach {
    when (name) {
        // 普通 jar / sources / javadoc：文件名统一为 HuYanAuthorize-<version>(-classifier).jar
        "jar", "sourcesJar", "javadocJar" -> {
            archiveBaseName.set("HuYanAuthorize")
        }

        // mirai2 插件包：文件名统一为 HuYanAuthorize-<version>.mirai2.jar
        "buildPlugin" -> {
            archiveBaseName.set("HuYanAuthorize")
            archiveFileName.set("HuYanAuthorize-${project.version}.mirai2.jar")
            // 清理旧的 auth-core-<version>.mirai2.jar，避免同一插件 id 在 plugins 目录下出现两份
            doFirst {
                val legacy = destinationDirectory.file("auth-core-${project.version}.mirai2.jar").get().asFile
                if (legacy.exists()) legacy.delete()
            }
        }
    }
}

afterEvaluate {
    publishing {
        publications.create<MavenPublication>("mavenJava") {
            artifact(tasks.named("buildPlugin"))
            artifact(sourcesJar)
            artifact(javadocJar)

            groupId = project.group.toString()
            artifactId = "HuYanAuthorize" // 保持原有的 artifactId
            version = project.version.toString()

            pom {
                setupCommonMetadata()
            }
        }

        repositories {
            maven {
                name = "local"
                url = rootProject.file("local").toURI()
            }
            maven {
                name = "chahuyun"
                url = uri("https://nexus.chahuyun.cn/repository/maven-releases/")
                credentials {
                    username = providers.gradleProperty("chahuyunUser").orNull
                    password = providers.gradleProperty("chahuyunPassword").orNull
                }
            }
        }
    }

    signing {
        useGpgCmd()
        sign(publishing.publications["mavenJava"])
    }
}

