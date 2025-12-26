### KSP 加速原理（HuYanAuthorize）

本文档用于解释 **HuYanAuthorize（上游前置插件）** 与 **下游插件** 在启用 KSP 后的完整运行链路：**编译期 → 打包期 → 运行时**。  
目标是让你能快速判断：KSP 到底有没有跑、生成物是什么、为什么运行时找不到 registrar、以及如何排查。

---

### 核心结论（一句话版本）

- **编译期**：`HuYanAuthorize-ksp` 扫描下游代码中的 `@MessageAuthorize`，为每个声明类生成一个 `*_AuthorizeRegistrar`（编译期注册器）。
- **运行时**：`HuYanAuthorize` 通过 `ServiceLoader`（或索引回退）找到这些 registrar，替代反射扫描来完成事件订阅，从而加速启动并减少反射开销。

---

### 一、角色与产物（先建立名词）

- **上游（HuYanAuthorize / core）**：提供运行时入口 `AuthorizeServer.registerEvents(...)` 和 registrar 接口 `GeneratedListenerRegistrar`。
- **上游（HuYanAuthorize-ksp / processor）**：KSP 处理器，负责扫描注解并生成 registrar/索引/服务文件。
- **下游插件（你的业务插件）**：实际写 `@MessageAuthorize` 的地方；通过 `ksp("cn.chahuyun:HuYanAuthorize-ksp:版本")` 引入处理器。

KSP 生成的关键产物有三类：

- **(A) Registrar 类（源码 → class）**：`你的类名_AuthorizeRegistrar`
- **(B) ServiceLoader 入口（resources）**：`META-INF/services/cn.chahuyun.authorize.listener.GeneratedListenerRegistrar`
- **(C) 兼容索引（源码 → class）**：`cn.chahuyun.authorize.listener.GeneratedListenerRegistrarIndex`

其中 **(C)** 是为了兼容某些打包链丢失 `META-INF/services` 的情况；只要 class 还在，就能回退发现 registrar。

---

### 二、编译期流程（下游 build/buildPlugin 时发生了什么）

#### 2.1 下游：你写的代码长什么样？

最小案例（下游插件）：

```kotlin
@EventComponent
class MyListener {

    @MessageAuthorize(text = ["你好"])
    suspend fun onHello(event: net.mamoe.mirai.event.events.MessageEvent) {
        event.subject.sendMessage("你好！")
    }
}
```

以及启动时调用（下游插件 onEnable）：

```kotlin
override fun onEnable() {
    // useKsp = true：强制走 KSP（找不到 registrar 会直接报错）
    AuthorizeServer.registerEvents(this, "your.package.name", useKsp = true)
}
```

> 说明：`@EventComponent` 仍然用于 “定位要注册的类”；真正的“订阅事件”由 registrar 或反射注册器完成。

#### 2.2 Gradle：KSP 处理器是怎么被加载的？

KSP 通过 Service Provider 机制加载处理器：

- `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`
- 内容为：`cn.chahuyun.authorize.ksp.AuthorizeProcessorProvider`

下游只要同时满足：

- `plugins { id("com.google.devtools.ksp") ... }`
- `dependencies { ksp("cn.chahuyun:HuYanAuthorize-ksp:xxx") }`

就会在 `kspKotlin`（或多平台对应 task）中执行处理器。

#### 2.3 处理器扫描什么？生成什么？

处理器扫描 **所有带 `@MessageAuthorize` 的函数**，并按“所属类”分组，每个类生成一个 registrar：

- `MyListener_AuthorizeRegistrar`

生成逻辑的要点：

- registrar 实现 `cn.chahuyun.authorize.listener.GeneratedListenerRegistrar`
- registrar 的 `register(...)` 会：
  - new 一个监听类实例（`val instance = MyListener()`）
  - 为每个 `@MessageAuthorize` 方法构建过滤链（权限过滤 + 消息过滤）
  - 调用 `channel.subscribeAlways(...)` 完成订阅
  - try/catch，把异常交给 `ExceptionHandleApi`

#### 2.4 编译期“可发现性”：为什么要写 service 文件 + 索引？

处理器在 `finish()` 阶段写两个“发现入口”：

1) **ServiceLoader 文件**（resources）  
`META-INF/services/cn.chahuyun.authorize.listener.GeneratedListenerRegistrar`  
内容为每个生成的 registrar 全限定名（FQCN），每行一个。

2) **索引类**（class）  
`cn.chahuyun.authorize.listener.GeneratedListenerRegistrarIndex`  
里面保存 `REGISTRARS: Array<String>`，同样是 registrar 的 FQCN 列表。

设计原因：

- `ServiceLoader` 是 JVM 标准机制，优先使用，最干净。
- 但某些下游的打包链/插件打包任务可能会丢失 KSP 的 generated resources（导致 service 文件没进包）。
- 索引类是“class 产物”，通常不会被打包丢失，所以可作为回退通道。

---

### 三、打包期流程（buildPlugin 为什么可能“看起来没生成”）

下游 `buildPlugin` 最终产物是 `.mirai2.jar`。要让运行时能发现 registrar，需要满足至少一个条件：

- **service 文件在包里**：存在 `META-INF/services/cn.chahuyun.authorize.listener.GeneratedListenerRegistrar`
- **或索引 class 在包里**：存在 `cn/chahuyun/authorize/listener/GeneratedListenerRegistrarIndex.class`

如果两者都不在，运行时就只能报错（useKsp=true）或回退反射（useKsp=false）。

快速检查（Windows）：

```bash
jar tf build/libs/*.mirai2.jar | findstr GeneratedListenerRegistrar
```

---

### 四、运行时流程（启动后如何真正“注册监听器”）

#### 4.1 下游调用入口：AuthorizeServer.registerEvents

运行时入口负责：

1) 使用 Reflections 扫描 `@EventComponent` 的类（定位需要注册的监听器类）
2) 调用 `ListenerManager.register(...)` 决定走 KSP 还是反射

> 注意：`useKsp` 的语义是 “强制 KSP”。如果你没开 KSP 或产物没打进去，强制 KSP 会直接抛错，避免你误以为自己用了加速。

#### 4.2 ListenerManager：优先 ServiceLoader，失败则索引回退

当 `useKsp=true` 时：

1) 先用当前插件的 classloader：  
`ServiceLoader.load(GeneratedListenerRegistrar::class.java, pluginCl)`  
并且强制只接受 `registrar.javaClass.classLoader === pluginCl` 的 provider（避免跨插件误加载）。

2) 如果一个都没找到：尝试回退加载索引类  
`Class.forName("...GeneratedListenerRegistrarIndex", ..., pluginCl)`  
读取 `REGISTRARS` 或调用 `registrarClassNames()`，拿到 registrar 列表后逐个反射 `newInstance()` 并执行 `register(...)`。

3) 如果仍然找不到：  
`useKsp=true` 会直接报错：  
“强制使用 KSP 注册事件，但未找到任何 GeneratedListenerRegistrar …”

#### 4.3 Registrar.register(...) 真正做了什么？

每个 `*_AuthorizeRegistrar` 的 `register(...)` 会在 `EventChannel` 上对每个事件方法做订阅，大致结构：

- 构建过滤链：`permFilter(...) && messageFilter(...)`
- `subscribeAlways(concurrency=..., priority=...)`
- try/catch → `handleApi.handle(e)`

---

### 五、排查清单（下游 KSP “不生效”时按顺序检查）

#### 5.1 编译期：KSP 是否真的执行了？

- Gradle 输出中是否出现 `:kspKotlin`（或类似任务）
- `build/generated/ksp/main/kotlin` 下是否生成了 `*_AuthorizeRegistrar.kt`

#### 5.2 打包期：最终 `.mirai2.jar` 里是否包含发现入口？

至少满足一个：

- `META-INF/services/cn.chahuyun.authorize.listener.GeneratedListenerRegistrar`
- `GeneratedListenerRegistrarIndex.class`

检查命令见上文 `jar tf ... | findstr ...`。

#### 5.3 运行时：你是否真的“强制 KSP”？

- 下游启动代码是否 `useKsp = true`
- 日志是否出现类似：
  - “检测到编译期生成的监听注册器: ...”
  - 或 “通过索引发现编译期生成的监听注册器: ...”

---

### 六、常见误区

- **误区 1：只开了 KSP 插件，但没加 processor 依赖**  
必须同时 `id("com.google.devtools.ksp")` + `ksp("cn.chahuyun:HuYanAuthorize-ksp:...")`

- **误区 2：KSP 生成了 registrar，但运行时找不到**  
多半是 service 文件未入包；现在可以依赖索引回退，但仍建议检查最终 jar 内容。

- **误区 3：useKsp=false 以为也会走 KSP**  
`useKsp=false` 会走反射注册器；KSP 生成物即使存在也不会被使用。


