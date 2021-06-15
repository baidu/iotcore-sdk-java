/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.iot.log.sdk;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.baidu.iot.log.sdk.enums.LogLevel;
import com.baidu.iot.mqtt.common.MqttConfigFactory;
import com.baidu.iot.shared.sub.transport.MqttSharedSubTransport;
import com.baidu.iot.shared.sub.transport.MqttTransportConfig;

/**
 * Created by mafei01 in 6/8/21 2:08 PM
 */
public class IotCoreLoggerRegisterTest {

    @Mocked
    private MqttSharedSubTransport transport;

    private String coreId = "testCore";
    private Config config;
    private AtomicReference<IotCoreLogger> success;
    private AtomicReference<Throwable> error;

    @Before
    public void setup() {
        config = Config.builder()
                .iotCoreId(coreId)
                .mqttConfig(MqttConfigFactory.genPlainMqttConfig(coreId, "user", "pwd".toCharArray()))
                .logLevel(LogLevel.INFO)
                .includeLowerLevel(true)
                .build();
        success = new AtomicReference();
        error = new AtomicReference();

    }

    @Test
    public void testRegisterLoggerSuccess() {
        IotCoreLoggerRegister register = new IotCoreLoggerRegister();
        List<MqttTransportConfig> mqttTransportConfigs = new ArrayList<>();
        new Expectations() {{
            MqttSharedSubTransport.create(withCapture(mqttTransportConfigs), (Scheduler) any);
            transport.start();
            result = Completable.complete();
        }};
        register.registerLogger(config).subscribe(success::set, error::set);
        Assert.assertEquals(1, mqttTransportConfigs.size());
        Assert.assertNotNull(success.get());
        Assert.assertEquals(config.getLogLevel().code + 1, mqttTransportConfigs.get(0).getTopicFilters().length);
        new Verifications() {{
            transport.start();
            times = 1;
        }};
    }

    @Test
    public void testRegisterLoggerFailed() {
        IotCoreLoggerRegister register = new IotCoreLoggerRegister();
        List<MqttTransportConfig> mqttTransportConfigs = new ArrayList<>();
        new Expectations() {{
            MqttSharedSubTransport.create(withCapture(mqttTransportConfigs), (Scheduler) any);
            transport.start();
            result = Completable.error(new RuntimeException());
        }};
        register.registerLogger(config).subscribe(success::set, error::set);
        Assert.assertEquals(1, mqttTransportConfigs.size());
        Assert.assertNotNull(error.get());
        Assert.assertEquals(config.getLogLevel().code + 1, mqttTransportConfigs.get(0).getTopicFilters().length);
        new Verifications() {{
            transport.start();
            times = 1;
        }};
    }

    @Test
    public void testRegisterLoggerTwice() {
        IotCoreLoggerRegister register = new IotCoreLoggerRegister();
        Single<IotCoreLogger> logger_1 = register.registerLogger(config);
        config = config.toBuilder().logLevel(LogLevel.DEBUG).build();
        Single<IotCoreLogger> logger_2 = register.registerLogger(config);
        Assert.assertEquals(logger_1, logger_2);
        Assert.assertEquals(1, register.loggerMap.size());
        new Verifications() {{
            transport.start();
            times = 1;
        }};
    }

    @Test
    public void testRegisterLoggerWithDeviceKey() {
        config = config.toBuilder().deviceKeys(new String[]{"device_1", "device_2"}).build();
        IotCoreLoggerRegister register = new IotCoreLoggerRegister();
        List<MqttTransportConfig> mqttTransportConfigs = new ArrayList<>();
        new Expectations() {{
            MqttSharedSubTransport.create(withCapture(mqttTransportConfigs), (Scheduler) any);
            transport.start();
            result = Completable.complete();
        }};
        register.registerLogger(config).subscribe(success::set, error::set);
        Assert.assertEquals(1, mqttTransportConfigs.size());
        Assert.assertNotNull(success.get());
        Assert.assertEquals((config.getLogLevel().code + 1) * 2, mqttTransportConfigs.get(0).getTopicFilters().length);
    }

    @Test
    public void testRegisterLoggerClose() {
        IotCoreLoggerRegister register = new IotCoreLoggerRegister();
        new Expectations() {{
            transport.start();
            result = Completable.complete();
            transport.close();
            result = Completable.complete();
        }};
        Single<IotCoreLogger> logger_1 = register.registerLogger(config);
        config = config.toBuilder().iotCoreId("iotCore2").logLevel(LogLevel.DEBUG).build();
        Single<IotCoreLogger> logger_2 = register.registerLogger(config);
        Assert.assertNotEquals(logger_1, logger_2);
        register.close().blockingSubscribe();
        new Verifications() {{
            transport.close();
            times = 2;
        }};
    }
}
