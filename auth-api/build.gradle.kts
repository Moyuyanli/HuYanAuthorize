@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
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

dependencies {
    compileOnly(libs.mirai.core.api)
}

// auth-api 仅用于工程内拆分与 KSP 编译期依赖，不对外发布，也不需要产物 jar，避免 Windows 下文件占用导致 clean/build 失败
tasks.withType<Jar>().configureEach {
    enabled = false
}
