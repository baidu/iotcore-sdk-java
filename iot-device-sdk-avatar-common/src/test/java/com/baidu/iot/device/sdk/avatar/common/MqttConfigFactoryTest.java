package com.baidu.iot.device.sdk.avatar.common;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
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
    public void testGenSslMqttConfig() throws FileNotFoundException {
        String iotCoreId = "test";
        File caCertFile = new File(this.getClass().getClassLoader().getResource("GlobalSign.cer").getFile());
        File deviceCertFile = new File(this.getClass().getClassLoader().getResource("deviceCert.cer").getFile());
        File devicePrivateKeyFile = new File(
                this.getClass().getClassLoader().getResource("devicePrivateKey.cer").getFile());

        MqttConfig mqttConfig = MqttConfigFactory.genSSlMqttConfig(
                iotCoreId, caCertFile, deviceCertFile, devicePrivateKeyFile);
        Assert.assertEquals(URI.create("ssl://test.iot.gz.baidubce.com:1884"), mqttConfig.getUri());
        Assert.assertNotNull(mqttConfig.getSslSocketFactory());
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
