// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.samples.deviceside;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

import com.google.common.util.concurrent.Uninterruptibles;

import java.time.Duration;

import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.deviceside.Device;
import com.baidu.iot.device.sdk.avatar.deviceside.IoTDeviceFactory;
import com.baidu.iot.mqtt.common.MqttConfigFactory;

public class ObserveDesiredDeltaSample {

    public static void main(String[] args) {
        String iotCoreId = "xxxxxxx";
        String deviceName = "test1";
        String username = "xxxxxxx/test1";
        char[] password = "xxxxxxxxxxxxxxxxx".toCharArray();
        long testTimeInSec = 10;

        IoTDeviceFactory factory = new IoTDeviceFactory(IoTDeviceFactory.Config.builder()
                .iotCoreId(iotCoreId)
                .build());

        Device device = factory.getDevice(deviceName,
                MqttConfigFactory.genPlainMqttConfig(iotCoreId, username, password)).blockingGet();

        device.observeDesired(new PropertyKey("test"))
                .observeOn(Schedulers.computation())
                .subscribe(new DisposableSubscriber<PropertyValue>() {
                    @Override
                    public void onNext(PropertyValue propertyValue) {
                        System.out.println("Receive delta:" + propertyValue);
                        request(1);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Observe error");
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        Uninterruptibles.sleepUninterruptibly(Duration.ofSeconds(testTimeInSec));
        device.close();
        factory.close();
        factory.closeSchedulers();
    }

}
