package com.baidu.iot.device.sdk.avatar.common;

import java.util.UUID;

/**
 * Author zhangxiao18
 * Date 2020/10/16
 */
public class TestKit {

    public static String genRandomIotCoreId() {
        return genRandomStr();
    }

    public static String genRandomDeviceName() {
        return genRandomStr();
    }

    private static String genRandomStr() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }

}
