dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        google()        // 必须包含这个
        mavenCentral()  // 必须包含这个
    }
}

rootProject.name = "HuYanAuthorize"

include("ksp-processor")
