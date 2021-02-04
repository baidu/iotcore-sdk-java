package com.baidu.iot.device.sdk.avatar.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

public class EntityIdTest {

    @Test
    public void testGenMqttClientId() {
        Pattern allow = Pattern.compile("\\w*");

        EntityId[] entityIds = new EntityId[] {
                new EntityId("abcdesg", "bbbbbaaa"),
                new EntityId("abcdesg", "fafa*faf@bb"),
                new EntityId("abcdesg", "asfAAA"),
                new EntityId("abcdesg", "bbbbbbbbbbbbbbbbaaabbbbbaaabbbbbaaabbbbbaaaaa" +
                        "abbbbbbbbbbaaabbbbbaaabbbbbaaabbbbbaaaaaabbbbaaabbbbbaaabbbbbaaabbbbbaaaaaa"),

        };
        for (EntityId entityId : entityIds) {
            System.out.println(entityId.genMqttClientId());
            Assert.assertTrue(allow.matcher(entityId.genMqttClientId()).matches());
        }
    }

}
