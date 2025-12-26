plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
}

group = "cn.chahuyun"
version = rootProject.version

// 统一 JVM 目标版本，避免 compileJava(17)/compileKotlin(1.8) 不一致警告（Gradle 8+ 会变为错误）
kotlin {
    jvmToolchain(11)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // 注意：处理器本身不需要依赖 auth-api（否则发布到 Maven 时 POM 会携带该依赖，导致下游拉取失败）
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

// KSP 需要在 processor classpath 中找到 SymbolProcessorProvider（通过 jar 内的 META-INF/services），因此 jar 必须生成。
tasks.named<Jar>("jar") {
    // 产物名与发布 artifactId 对齐（可读性更好；不影响 Maven 的 artifactId）
    archiveBaseName.set("HuYanAuthorize-ksp")
    // Windows 下 jar 可能被运行中的 Java 进程占用导致无法覆盖旧文件：
    // - 发布时必须使用稳定文件名（便于上传与校验）
    // - 本地开发构建时使用带时间戳的文件名，避免“删除旧 jar”触发失败
    val isPublishRequested = gradle.startParameter.taskNames.any { it.contains("publish", ignoreCase = true) }
    if (!isPublishRequested) {
        archiveFileName.set("HuYanAuthorize-ksp-${project.version}-dev-${System.currentTimeMillis()}.jar")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "HuYanAuthorize-ksp"
            
            pom {
                name.set("HuYanAuthorize KSP Processor")
                description.set("KSP Processor for HuYanAuthorize")
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
