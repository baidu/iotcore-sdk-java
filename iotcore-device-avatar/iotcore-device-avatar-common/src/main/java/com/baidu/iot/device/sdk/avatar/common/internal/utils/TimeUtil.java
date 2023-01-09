// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.utils;

import java.util.concurrent.TimeUnit;

/**
 * Author zhangxiao18
 * Date 2020/9/28
 */
public class TimeUtil {

    public static int getCurrentTimeSec() {
        return (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }

}
