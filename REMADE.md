# HuYanAuthorize 壶言鉴权

### 说明

一个用于针对简化mirai插件开发的前置插件，简化的内容为：

* 自动化添加消息监听
* 针对用户(Friend),群(Group),群成员(GroupMember)都有独立的权限
* 权限可自定义跟改，权限类别也可以自定义添加
* 自动匹配触发内容(直接匹配或正则匹配)

### 使用

* 添加依赖

在build.gradle中

```groovy
    dependencies {
    compileOnly("cn.chahuyun:HuYanAuthorize:1.0.0")
}
```

在插件的onLoad方法中启用

```java
        //添加本插件的注册消息包信息
        PermissionServer instance=PermissionServer.getInstance();
        instance.init(INSTANCE,"cn.chahuyun.authorize.manager");
```

再在需要消息监听的类上和方法上分别添加`@MessageComponent`和`@MessageAuthorize`注解

```java

@MessageComponent //声明此类中有需要注册消息监听的方法
public class PermissionManager {

    @MessageAuthorize(text = "你好")//注册消息监听，具体参数可以查看 MessageAuthorize 注解
    public void reply(MessageEvent event) {
        event.getSubject().sendMessage("你好呀！");
    }

}
```

#### 指令

| 命令前缀 | 命令    | 含义       | 案例                   |
|------|-------|----------|----------------------|
| #hya | owner | 设置主人     | #hya owner 572490972 |
|      | v     | 查询当前插件版本 | #hya v               |

#### 配置
```yaml
    # 主人
    owner: 0
```

### 开发
#### 添加自定义权限