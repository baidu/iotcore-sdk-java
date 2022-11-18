// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Author zhangxiao18
 * Date 2020/10/16
 */
public class UserMessageIdTest {

    @Test
    public void testCreateAndConvert() {
        MessageId messageId = MessageId.next();
        Assert.assertEquals(messageId, MessageId.of(messageId.toString()));
    }

    @Test
    public void testCreateHighFrequency() throws InterruptedException {
        Set<MessageId> messageIds = new HashSet<>();
        for (int i = 0; i < 1100; i ++) {
            messageIds.add(MessageId.next());
        }

        // 1024 per sec
        Assert.assertEquals(1024, messageIds.size());

        messageIds = new HashSet<>();
        for (int i = 0; i < 1100; i ++) {
            messageIds.add(MessageId.next());
            Thread.sleep(1);
        }
        Assert.assertEquals(1100, messageIds.size());
    }

}
