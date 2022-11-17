// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.mqtt.common;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

public class MqttConfigFactoryTest {

    @Test
    public void testGenPlainMqttConfig() {
        String iotCoreId = "test";
        String username = "username";
        char[] password = "password".toCharArray();
        MqttConfig mqttConfig = MqttConfigFactory.genPlainMqttConfig(iotCoreId, username, password);
        Assert.assertEquals(URI.create("tcp://test.iot.gz.baidubce.com:1883"), mqttConfig.getUri());
        Assert.assertEquals(username, mqttConfig.getUsername());
        Assert.assertEquals(password, mqttConfig.getPassword());
        Assert.assertNull(mqttConfig.getSslSocketFactory());
    }

    @Test
    public void testGenMD5SignatureMqttConfig() {
        String iotCoreId = "test";
        String deviceKey = "deviceKey";
        String secretKey = "secretKey";
        MqttConfig mqttConfig = MqttConfigFactory.genMD5SignatureMqttConfig(iotCoreId, deviceKey, secretKey);
        Assert.assertEquals(URI.create("tcp://test.iot.gz.baidubce.com:1883"), mqttConfig.getUri());
        Assert.assertNotNull(mqttConfig.getUsername());
        Assert.assertNotNull(mqttConfig.getPassword());
        Assert.assertNull(mqttConfig.getSslSocketFactory());
    }

    @Test
    public void testGenSHA256SignatureMqttConfig() {
        String iotCoreId = "test";
        String deviceKey = "deviceKey";
        String secretKey = "secretKey";
        MqttConfig mqttConfig = MqttConfigFactory.genSHA256SignatureMqttConfig(iotCoreId, deviceKey, secretKey);
        Assert.assertEquals(URI.create("tcp://test.iot.gz.baidubce.com:1883"), mqttConfig.getUri());
        Assert.assertNotNull(mqttConfig.getUsername());
        Assert.assertNotNull(mqttConfig.getPassword());
        Assert.assertNull(mqttConfig.getSslSocketFactory());
    }

    @Test
    public void testGenBceIamSignatureMqttConfig() {
        String iotCoreId = "test";
        String appKey = "appKey";
        String secretKey = "secretKey";
        MqttConfig mqttConfig = MqttConfigFactory.genBceIamSignatureMqttConfig(iotCoreId, appKey, secretKey);
        Assert.assertEquals(URI.create("tcp://test.iot.gz.baidubce.com:1883"), mqttConfig.getUri());
        Assert.assertNotNull(mqttConfig.getUsername());
        Assert.assertNotNull(mqttConfig.getPassword());
        Assert.assertNull(mqttConfig.getSslSocketFactory());
    }

}
