# HuYanAuthorize 壶言鉴权

## 说明

一个用于针对简化 mirai 插件开发的前置插件。它通过注解驱动的方式，大幅减少了编写消息监听和权限校验的重复代码。

### 核心特性

*   **自动化消息监听**: 使用 `@EventComponent` 和 `@MessageAuthorize` 自动化注册监听器。
*   **编译期优化 (KSP)**: 支持使用 KSP 在编译期生成注册代码，消除运行时反射开销，提升启动速度。
*   **多维度权限系统**: 针对用户 (Friend)、群 (Group)、群成员 (GroupMember)、群管理 (GroupAdmin) 提供独立权限控制。
*   **权限分组与继承**: 权限通过分组管理，支持组间继承。
*   **灵活的消息匹配**: 支持文本、正则、以及自定义逻辑匹配。
*   **指令前缀支持**: 全局或局部配置指令触发前缀。
*   **轻量化**: 移除外部依赖，减少插件体积。

---

## 快速开始

### 1. 添加依赖

在 `settings.gradle.kts` 中添加仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        // 壶言私服 - 国内加速镜像代理 (推荐)
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        // KSP 插件与常规依赖来源 (备用)
        google()
        mavenCentral()
    }
}
```

在子项目的 `build.gradle.kts` 中添加插件和依赖：

```kotlin
plugins {
    // 引入 KSP 插件 (版本需与 Kotlin 版本匹配，如 Kotlin 1.9.20 对应 1.9.20-1.0.14)
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

dependencies {
    // 核心库
    compileOnly("cn.chahuyun:HuYanAuthorize:1.3.2")
    
    // 启用 KSP 编译期加速 (可选，需要同时引入上面的 ksp 插件)
    ksp("cn.chahuyun:HuYanAuthorize-ksp:1.3.2")
}
```

### 2. 启用插件

在插件的 `onEnable` 方法中：

```kotlin
override fun onEnable() {
    // 注册本插件的消息监听包
    AuthorizeServer.registerEvents(this, "your.package.name")
}
```

### 3. 编写监听器

创建一个带有 `@EventComponent` 注解的类，并在方法上使用 `@MessageAuthorize`：

```kotlin
@EventComponent
class MyListener {

    @MessageAuthorize(text = ["你好"])
    suspend fun onHello(event: MessageEvent) {
        event.subject.sendMessage("你好啊！")
    }

    @MessageAuthorize(
        text = ["admin"],
        userPermissions = ["admin_code"] // 需要拥有 admin_code 权限
    )
    suspend fun onAdmin(event: MessageEvent) {
        event.subject.sendMessage("管理员你好！")
    }
}
```

---

## 核心服务：AuthorizeServer

`AuthorizeServer` 是功能入口类（原 `PermissionServer` 已弃用，但仍保留向下兼容）。

### 注册事件
`registerEvents(plugin, packageName, exceptionHandle, prefix)`
扫描指定包下所有带有 `@EventComponent` 的类并自动注册监听器。

### 注册权限码
`registerPermissions(plugin, vararg perms)`
预定义插件所需的权限码。

---

## 编译期优化 (KSP) 接入指南

本插件支持使用 KSP (Kotlin Symbol Processing) 来替换运行时的反射扫描。

### 为什么使用 KSP？
1. **启动速度**: 避免在插件启动时扫描大量的 Class 注解。
2. **混淆安全**: 直接生成代码调用，不受 R8/Proguard 混淆影响。
3. **性能**: 消除反射调用 (Method.invoke) 的额外开销。

### 接入步骤：
1. 在项目 `build.gradle.kts` 的 `plugins` 块中引入 `com.google.devtools.ksp`。
2. 在 `dependencies` 块中添加 `ksp("cn.chahuyun:huyan-authorize-ksp:版本号")`。
3. **无需修改业务代码**: KSP 会在编译时自动为每个类生成 `XXX_AuthorizeRegistrar` 辅助类，主插件在运行时会优先识别并加载它们。

---

## 消息鉴权注解 API (@MessageAuthorize)

| 属性                                  | 类型                | 说明                                        |
|:------------------------------------|:------------------|:------------------------------------------|
| `text`                              | `Array<String>`   | 匹配文本。正则模式下取第一条。                           |
| `messageMatching`                   | `Enum`            | 匹配方式：`TEXT`(默认), `REGULAR`, `CUSTOM`。     |
| `messageConversion`                 | `Enum`            | 转换方式：`MIRAI_CODE`(默认), `CONTENT`, `JSON`。 |
| `userPermissions`                   | `Array<String>`   | 用户需拥有的权限码。                                |
| `groupPermissions`                  | `Array<String>`   | 群需拥有的权限码。                                 |
| `userInGroupPermissionsAssociation` | `Enum`            | 用户与群权限的关联逻辑：`AND`(默认), `OR`。              |
| `priority`                          | `EventPriority`   | 监听优先级。                                    |
| `concurrency`                       | `ConcurrencyKind` | 并发处理方式。                                   |

---

## IntelliJ IDEA 插件支持 (开发计划)

为了提供最佳的开发体验，我们计划推出配套的 IntelliJ IDEA 插件。目前该插件仍在路线图中，尚未正式发布。

### 核心功能 (规划中)
*   **[未实现] 消除未使用警告**: 自动识别 `@MessageAuthorize` 标记的方法为程序入口点，不再提示 "Method is never used"。
*   **[未实现] 权限码自动补全**: 在注解中输入权限码时，提供智能提示。
*   **[未实现] 正则校验**: 在编写 `@MessageAuthorize` 正则规则时，实时进行语法检查。
*   **[未实现] 侧边栏导航 (Gutter Icons)**: 在监听方法侧边显示图标，点击可快速跳转或查看配置。

### 临时解决方案 (针对 "Unused" 警告)
在配套插件发布前，下游开发者可以通过以下方式手动消除 IDEA 的未使用方法警告：

1.  **手动添加入口点 (Entry Point)**:
    *   打开 IDEA 设置: `Editor` -> `Inspections`。
    *   找到 `JVM languages` -> `Unused declaration`。
    *   在右侧点击 `Entry Points` 标签页 -> `Annotations...`。
    *   点击 `+` 号并输入: `cn.chahuyun.authorize.MessageAuthorize`。
2.  **项目级共享 (推荐)**:
    *   在 IDEA 中将检查配置文件 (Inspection Profile) 的 `Scheme` 改为 `Project`。
    *   将 `.idea/inspectionProfiles/Project_Default.xml` 提交到 Git 仓库，这样所有下游开发者克隆项目后都将自动获得该配置。

---

## 权限管理常用指令

| 指令                   | 作用            | 示例                   |
|:---------------------|:--------------|:---------------------|
| `+perm (组名) [code]`  | 创建/修改权限组并添加权限 | `+perm 管理组 admin`    |
| `+global (@用户) (组名)` | 将用户加入全局权限组    | `+global 123456 管理组` |
| `+member (@用户) (组名)` | 将群成员加入权限组     | `+member @小明 成员组`    |
| `=perm [组名]`         | 查询权限组信息       | `=perm`              |
