package cn.chahuyun.authorize;

import cn.chahuyun.authorize.command.AuthorizeCommand;
import cn.chahuyun.authorize.config.AuthorizeConfig;
import cn.chahuyun.authorize.utils.HibernateUtil;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.utils.MiraiLogger;
import xyz.cssxsh.mirai.hibernate.MiraiHibernateConfiguration;

/**
 * @author Moyuyanli
 */
public final class HuYanAuthorize extends JavaPlugin {

    /**
     * 插件本身唯一实例
     */
    public static final HuYanAuthorize INSTANCE = new HuYanAuthorize();
    /**
     * 全局版本
     */
    public static final String VERSION = "1.0.7";
    /**
     * 日志
     */
    public static final MiraiLogger LOGGER = INSTANCE.getLogger();

    private HuYanAuthorize() {
        super(new JvmPluginDescriptionBuilder("cn.chahuyun.HuYanAuthorize", VERSION)
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
        CommandManager.INSTANCE.registerCommand(AuthorizeCommand.INSTANCE, true);
        //加载配置
        MiraiHibernateConfiguration configuration = new MiraiHibernateConfiguration(this);
        //初始化插件数据库
        HibernateUtil.init(configuration);
        //添加本插件的注册消息包信息
        PermissionServer instance = PermissionServer.getInstance();
        instance.init(INSTANCE, "cn.chahuyun.authorize.manager");
        LOGGER.info("HuYanAuthorize plugin loaded!");
    }

    @Override
    public void onDisable() {
        LOGGER.info("HuYanAuthorize plugin uninstall!");
    }
}