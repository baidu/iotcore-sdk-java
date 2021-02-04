package com.baidu.iot.device.sdk.avatar.common.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import org.junit.Assert;
import org.junit.Test;

/**
 * @Author zhangxiao18
 * @Date 2020/10/16
 */
public class TopicTest {

    @Test
    public void testGen() {
        AvatarId avatarId = new AvatarId("test", "testabc");
        Topic topic = Topic.genGetTopic(avatarId);
        Topic converted = new Topic(topic.getTopicFilter());
        Assert.assertEquals(topic, converted);
    }

    @Test
    public void testCovert() {
        String getTopic = "$iot/test/shadow/get";
        Assert.assertSame(new Topic(getTopic).getTopicType(), Topic.TopicType.GET);
    }

}
