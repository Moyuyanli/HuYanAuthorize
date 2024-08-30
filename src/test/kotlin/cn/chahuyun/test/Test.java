package cn.chahuyun.test;

import cn.chahuyun.authorize.PermissionServer;
import cn.chahuyun.authorize.entity.Perm;

import java.util.ArrayList;

/**
 * @author Moyuyanli
 * @date 2024/8/21 14:59
 */
public class Test {

    public static void main(String[] args) {

        PermissionServer.INSTANCE.registerPermCode(
                this,
                new Perm("admin","管理员权限")
                );



    }

}
