package cn.chahuyun.authorize.manager;

import cn.chahuyun.authorize.HuYanAuthorize;
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
            HuYanAuthorize.log.error("请删除data中的HuYanAuthorize.mv.db后重新启动！", e);
            return;
        }
        HuYanAuthorize.log.info("HuYanAuthorize database initialization succeeded!");
    }


}
