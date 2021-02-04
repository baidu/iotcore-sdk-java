package com.baidu.iot.device.sdk.avatar.common.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @Author zhangxiao18
 * @Date 2020/10/16
 */
public class UserMessageTest {

    @Test
    public void testGenAndConvert() {
        UserMessage userMessage = UserMessage.genGetMessage(GetAvatarRequest.newBuilder().build());
        Assert.assertEquals(userMessage.getId(), UserMessage.buildByMqttMessage(
                Topic.genGetReplyTopic(new AvatarId("test", "test")), userMessage.toMqttMessage()).getId());
    }

}
