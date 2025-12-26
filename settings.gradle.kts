@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        google()
        mavenCentral()
    }
}
pluginManagement{
    repositories{
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        google()
        mavenCentral()
    }
}

rootProject.name = "HuYanAuthorize"

include("auth-api")
include("auth-core")
include("auth-ksp")
