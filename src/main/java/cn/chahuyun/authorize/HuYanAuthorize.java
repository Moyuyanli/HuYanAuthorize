package cn.chahuyun.authorize;

import cn.chahuyun.authorize.command.AuthorizeCommand;
import cn.chahuyun.authorize.config.AuthorizeConfig;
import cn.chahuyun.authorize.manager.HibernateUtil;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.BotOnlineEvent;
import net.mamoe.mirai.utils.MiraiLogger;
import xyz.cssxsh.mirai.hibernate.MiraiHibernateConfiguration;

public final class HuYanAuthorize extends JavaPlugin {
    public static final HuYanAuthorize INSTANCE = new HuYanAuthorize();

    /**
     * 全局版本
     */
    public static final String version = "0.1.0";

    public static final MiraiLogger log = INSTANCE.getLogger();

    private HuYanAuthorize() {
        super(new JvmPluginDescriptionBuilder("cn.chahuyun.HuYanAuthorize", version)
                .name("HuYanAuthorize")
                .author("Moyuyanli")
                .info("壶言权限管理")
                .dependsOn("xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin", false)
                .build());
    }

    @Override
    public void onEnable() {
        //加载配置
        reloadPluginConfig(AuthorizeConfig.INSTANCE);
        //加载指令
        CommandManager.INSTANCE.registerCommand(AuthorizeCommand.INSTANCE,true);
        //加载配置
        MiraiHibernateConfiguration configuration = new MiraiHibernateConfiguration(this);
        //初始化插件数据库
        HibernateUtil.init(configuration);
        //注册本插件的监听
        EventChannel<Event> eventEventChannel = GlobalEventChannel.INSTANCE.parentScope(INSTANCE);
        //添加本插件的注册消息包信息
        PermissionServer instance = PermissionServer.getInstance();
        instance.addPackagePath(INSTANCE, "cn.chahuyun.authorize.manager");
        eventEventChannel.subscribeOnce(BotOnlineEvent.class, event -> instance.init(eventEventChannel));
        getLogger().info("Plugin loaded!");
    }
}