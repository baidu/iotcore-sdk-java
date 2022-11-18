// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal;

import org.junit.Assert;
import org.junit.Test;

/**
 * Author zhangxiao18
 * Date 2020/10/16
 */
public class AvatarSchedulersTest {

    @Test
    public void testInitAndClose() {
        String iotCoreId = "AvatarSchedulersTest";

        Assert.assertNotNull(AvatarSchedulers.io(iotCoreId));
        Assert.assertNotNull(AvatarSchedulers.task(iotCoreId));

        Assert.assertSame(AvatarSchedulers.taskExecutorService(iotCoreId), AvatarSchedulers.taskExecutorService(iotCoreId));

        AvatarSchedulers.close(iotCoreId);
        Assert.assertFalse(AvatarSchedulers.sdkSchedules.containsKey(iotCoreId));
    }

}
