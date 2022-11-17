// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;

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

    public static AvatarId genRandomAvatarId() {
        return new AvatarId(genRandomIotCoreId(), genRandomDeviceName());
    }

    public static String genRandomStr() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }

    public static String genRandomJsonStr() {
        return "\"" + genRandomStr() + "\"";
    }

}
