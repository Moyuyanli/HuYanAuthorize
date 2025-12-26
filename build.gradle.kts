@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    // 统一在根项目声明版本（来自 gradle/libs.versions.toml），子模块不要再写 version
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.mirai.console) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.buildconfig) apply false
}

group = "cn.chahuyun"
version = "1.3.5"

subprojects {
    repositories {
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        google()
        mavenCentral()
    }
}
