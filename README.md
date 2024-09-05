# HuYanAuthorize 壶言鉴权

## 说明

一个用于针对简化mirai插件开发的前置插件，简化的内容为：

* 自动化添加消息监听
* 针对用户(Friend),群(Group),群成员(GroupMember),群管理(GroupAdmin)都有独立的权限
* 权限是通过分组形式添加，可以继承
* 自动匹配触发内容(直接匹配或正则匹配)
* 权限code也自定义添加

## 使用

* 添加依赖

在build.gradle中

```groovy
    dependencies {
    compileOnly("cn.chahuyun:HuYanAuthorize:1.0.8")
}
```

在插件的onEnable方法中启用

java

```java
    //添加本插件的注册消息包信息
    PermissionServer.INSTANCE.init(this,"cn.chahuyun.authorize.event");
```

kotlin

```kotlin
    PermissionServer.init(this, "cn.chahuyun.authorize.event")

```

在对应的包里面建立一个`class`,然后在类上面添加`@EventComponent`注解，表明这个类下面有需要注册的消息事件方法。

建立一个方法，只有一个消息参数，无返回值。

```java

//添加消息鉴权注解
@MessageAuthorize(text = "你好")
public void reply(MessageEvent event) {
    event.getSubject().sendMessage("你好呀！");
}
```

```kotlin
    @MessageAuthorize(text = ["你好"])
suspend fun reply(event: MessageEvent) {
    event.subject.sendMessage("你好啊")
}
```

即可完成最基本的使用！

## 消息鉴权api

`MessageAuthorize`注解的所有可操作属性

### text 匹配文本

`text: Array<String>`

用于匹配的文本。

* 在默认模式下，有任何一个`String`字符串，于其中一个相同，即匹配成功。
* 在正则模式下，取第一个匹配。

### custom 自定义匹配

`custom: KClass<out CustomPattern>`

自定义匹配规则。

实现`CustomPattern`接口即可。

在匹配方式设置为`CUSTOM`时启用，启用时将忽略`text`的配置。

### messageMatching 消息匹配方式

`messageMatching: MessageMatchingEnum`

消息的匹配方式，有:

* TEXT 文本匹配
* REGULAR 正则匹配
* CUSTOM 自定义匹配

### messageConversion 消息转换方式

`messageConversion: MessageConversionEnum`

用于在匹配消息的时候，消息转换为`string`的方式。

* CONTENT `contentToString()`
* MIRAI_CODE `serializeToMiraiCode()`
* JSON `serializeToJsonString()`

### userPermissions 用户权限

`userPermissions: Array<String>`

需要用户拥有的权限。

自己的权限需要进行注册，注册方式请查看权限注册。

### userPermissionsMatching 用户权限匹配方式

`userPermissionsMatching: PermissionMatchingEnum`

如果需要用户权限有多个，多个权限之间的匹配方式.

* or 或，满足一个即匹配成功
* and 且,都满足才匹配成功

### groupPermissions 群权限

`groupPermissions: Array<String>`

要求的群权限，跟用户同理，只不过要求的是群的权限。

### groupPermissionsMatching 群权限匹配方式

`groupPermissionsMatching:PermissionMatchingEnum`

同上面

### userInGroupPermissionsAssociation 用户与群权限的匹配方式

`userInGroupPermissionsAssociation: PermissionMatchingEnum`

如果某一条指令，即要求用户权限，又要求群权限。

这里可以控制两种权限的合并方式。

同上面用户的匹配方式。

### priority 优先级

消息响应优先级,跟mirai的消息注册一样，一般不建议修改。

### concurrency 消息的处理方式

消息的处理方式，跟mirai的消息注册一样，一般不建议修改。

## 权限

权限是本插件的核心，用于对整个消息注册做控制。

才用的是`code`形式，是`String`字符串。

本插件默认权限：

* `owner` 主人权限，最高权限，类比root
* `admin` 管理员权限，比root低

不可使用的code:

* `null` 表明无权限

### 自定义注册权限

`fun registerPermCode(plugin: JvmPlugin, vararg perms: Perm)`
注册权限很简单。

权限可以一次性注册多个。

java

```java
    PermissionServer.INSTANCE.registerPermCode(this,new Perm("test","一个测试的权限")...);
```

```kotlin
    PermissionServer.registerPermCode(this, Perm("test", "一个测试的权限"))
```

注册成功后，会打印debug日志显示是否注册成功，或者该权限是否被别的插件注册。

然后就可以在`MessageAuthorize`中的权限列表使用了。

### 操作权限

权限的实现分两部分，一部分是权限组(PermGroup)，权限组有权限(Perm)和用户(User)。

对于权限组，在依赖插件中如果手动操作了权限组，可以使用`PermGroup`自己的`save()`方法保存，或者使用工具操作权限组。

操作权限组指令:

| 指令                        | 含义                                     | 案例                  | 案例含义                                            |
|---------------------------|----------------------------------------|---------------------|-------------------------------------------------|
| `+perm (权限组名称) [权限code]`  | 创建一个权限组，并添加对应的权限，权限组必填，code选填。         | +perm 测试组 admin     | 添加或修改一个名叫`测试组`的权限组，并为测试组添加权限`admmin`。           |
| `+perm (权限组名称) [%父权限组名称]` | 继承一个权限组，从父权限组哪里继承权限。指令能添加和修改权限组，并且能共用。 | +perm 测试组 %主人 admin | 添加或修改一个名叫`测试组`的权限组，继承`主人`权限组的所有权限，并添加`admin`权限。 |
| `-perm (权限组名称) [权限code]`  | 删除一个权限或权限组,当不填权限code时，会尝试删除整个权限组。      | -perm 测试组 admin     | 删除权限组`测试组`的权限`admin`。                           |
| `=perm [权限组名称]`           | 查询权限组信息，也可以查询单独一个权限组的信息。               | =perm               | 查询所有权限组信息                                       |

用户操作指令:

用户分为四类用户，他们有不同的作用域。

#### 全局用户

这个用户是作用于所有位置的一个单qq用户。

| 指令                        | 含义                           | 案例             | 案例含义         |
|---------------------------|------------------------------|----------------|--------------|
| `+global (@user) (权限组名称)` | 添加这个用户到对应权限组，这里可以at，可以直接填qq。 | +global @放空 主人 | 添加放空到`主人`权限组 |
| `-global (@user) (权限组名称)` | 还没写                          |                |              |

#### 群成员用户

这个用户作用于指定群的某个一群成员用户。

| 指令                        | 含义                            | 案例             | 案例含义            |
|---------------------------|-------------------------------|----------------|-----------------|
| `+member (@user) (权限组名称)` | 添加这个群成员到对应权限组，这里只能at，并且得在群里操作 | +member @放空 主人 | 添加群成员放空到`主人`权限组 |
| `-member (@user) (权限组名称)` | 还没写                           |                |                 |

#### 群管理用户

这个用户是对于一个群的群主和管理员的用户。

| 指令                | 含义                      | 案例         | 案例含义            |
|-------------------|-------------------------|------------|-----------------|
| `+admin  (权限组名称)` | 添加这个群的管理员到对应权限组，只能在群里操作 | +admin  主人 | 添加本群管理员到`主人`权限组 |
| `-admin (权限组名称)`  | 还没写                     |            |                 |

#### 群用户

这个用户跟上面三个不一样，这个代表的是这个群拥有的权限，而不是这个群里面所有人拥有的权限。

| 指令                | 含义                  | 案例         | 案例含义         |
|-------------------|---------------------|------------|--------------|
| `+group  (权限组名称)` | 添加这个群到对应权限组，只能在群里操作 | +group  主人 | 添加本群到`主人`权限组 |
| `-group (权限组名称)`  | 还没写                 |            |              |

## 其他api

还开发一些好用的工具类，供大家使用。

### 权限操作工具类(PermUtil)

[PermUtil.kt](https://github.com/Moyuyanli/HuYanAuthorize/blob/master/src/main/kotlin/cn/chahuyun/authorize/utils/PermUtil.kt)

#### 获取一个权限

`fun takePerm(code: String): Perm`

只能获取已经注册过的权限，未注册的权限会抛错。

```kotlin
PermUtil.takePerm("admin")
```

#### 获取一个权限组

`fun talkPermGroupByName(name: String): PermGroup`

获取一个权限组，不存在则新建。

```kotlin
PermUtil.talkPermGroupByName("测试组")
```

#### 检查这个用户有没有这个权限

`fun checkUserHasPerm(user: User, code: String): Boolean`

用户的获取请看下面

```kotlin
    //获取一个群用户
val group = User.group(group.id)
PermUtil.checkUserHasPerm(group, "admin")
```

#### 将这个用户添加到对应的权限组

`fun addUserToPermGroupByName(user: User, name: String): Boolean`

```kotlin
PermUtil.addUserToPermGroupByName(group, "测试组")
```

#### 将这个权限添加到对于的权限组

`fun addPermToPermGroupByName(perm: Perm, name: String): Boolea`
`fun addPermToPermGroupByPermGroup(perm: Perm, permGroup: PermGroup): Boolean`

#### 将这个用户从对应的权限组删除

`fun delUserFromPermGroupByName(user: User, name: String): Boolean`

### 用户的操作(User)

[User.kt](https://github.com/Moyuyanli/HuYanAuthorize/blob/master/src/main/kotlin/cn/chahuyun/authorize/entity/User.kt)

#### 获取用户

* fun globalUser(userId: Long): User 查询或新建一个全局用户
* fun group(groupId: Long): User 查询或新建一个群用户
* fun groupAdmin(groupId: Long): User 查询或新建一个群管理用户
* fun member(groupId: Long, userId: Long): User 查询或新建一个群成员用户

kotlin

```kotlin
    User.group(390444068)
User.member(390444068, 572490972)
```

java

```java
    User.Companion.group(390444068L);
    User.Companion.

member(390444068L,572490972L);
```

### 消息工具类(MessageUtil)

[MessageUtil.kt](https://github.com/Moyuyanli/HuYanAuthorize/blob/master/src/main/kotlin/cn/chahuyun/authorize/utils/MessageUtil.kt)

这个类比较简单，具体看看源码就明白了，但是好用！

### 日志工具类(Log)

[Log.kt](https://github.com/Moyuyanli/HuYanAuthorize/blob/master/src/main/kotlin/cn/chahuyun/authorize/utils/Log.kt)

使用方法:

在插件的`onEnable`方法中：

```kotlin
Log.init(this)
```

即可加载完成。

然后在需要打印日志的地方直接：

```kotlin
Log.info("这是一条日志")
```

```java
Log.INSTANCE.info("这是%s日志","一个");
```

对于java，自带格式化。

如果觉得不优雅，可以只引入他的`INSTANCE`。

### 错误工具(ExceptionHandle)

[ExceptionHandle.kt](https://github.com/Moyuyanli/HuYanAuthorize/blob/master/src/main/kotlin/cn/chahuyun/authorize/exception/ExceptionHandle.kt)

对于消息注册方法，我做了一个错误收集。

```kotlin
fun init(
    plugin: JvmPlugin,
    packageName: String,
    exceptionHandle: ExceptionHandleApi = ExceptionHandle(),
)
```
这是一个消息注册的重载方法，只需要传递自己实现的错误接口即可拿到错误。

### 获取主人

对于依赖于本插件的插件，可以实现共用一个主人，就算是换代，换配置，主人依然可以保存。

主人信息存在于数据库，如果`config`没有设置，会尝试从数据库找之前设置的主人来同步。
如果`config`已经设置，会自动通过`config`的信息同步到数据。

```kotlin
    HuYanAuthorize.INSTANCE.getOwner()
```
```java
HuYanAuthorize.Companion.getINSTANCE().getOwner();
```

## 指令

| 命令前缀 | 命令    | 含义       | 案例                   |
|------|-------|----------|----------------------|
| #hya | owner | 设置主人     | #hya owner 572490972 |
|      | v     | 查询当前插件版本 | #hya v               |




