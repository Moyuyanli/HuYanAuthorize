package cn.chahuyun.test;

import cn.chahuyun.authorize.utils.UserUtil;

/**
 * @author Moyuyanli
 * @date 2024/8/21 14:59
 */
public class Test {

    public static void main(String[] args) {
        UserUtil.INSTANCE.globalUser(1L);
    }

}
