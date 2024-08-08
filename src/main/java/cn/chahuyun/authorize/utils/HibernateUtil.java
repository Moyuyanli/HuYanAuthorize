package cn.chahuyun.authorize.utils;

import cn.chahuyun.authorize.entity.PermissionInfo;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import xyz.cssxsh.mirai.hibernate.MiraiHibernateConfiguration;

/**
 * 说明
 *
 * @author Moyuyanli
 * @Description :hibernate
 * @Date 2022/7/30 22:47
 */
public class HibernateUtil {


    /**
     * 数据库连接前缀
     */
    private static final String SQL_PATH_PREFIX = "jdbc:h2:file:";

    /**
     * 会话工厂
     */
    public static SessionFactory factory = null;

    private HibernateUtil() {

    }

    /**
     * Hibernate初始化
     *
     * @param configuration Configuration
     * @author Moyuyanli
     * @date 2022/7/30 23:04
     */
    public static void init(MiraiHibernateConfiguration configuration) {
        String path = SQL_PATH_PREFIX + "./data/cn.chahuyun.HuYanAuthorize/HuYanAuthorize";
        configuration.setProperty("hibernate.connection.url", path);
        try {
            factory = configuration.buildSessionFactory();
        } catch (HibernateException e) {
            HuYanAuthorize.LOGGER.error("请删除data中的HuYanAuthorize.mv.db后重新启动！", e);
            return;
        }
        PermissionInfo adminPerm = new PermissionInfo("admin", "权限插件的管理员");
        factory.fromTransaction(session -> session.merge(adminPerm));
        PermissionInfo allPerm = new PermissionInfo("all", "除管理员以外的任意其他权限");
        factory.fromTransaction(session -> session.merge(allPerm));
        HuYanAuthorize.LOGGER.info("HuYanAuthorize database initialization succeeded!");
    }


}
