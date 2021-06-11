/*
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.iot.device.sdk.avatar.samples.integration;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

import com.google.common.util.concurrent.Uninterruptibles;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.controlside.ControlledDevice;
import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceController;
import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceControllerFactory;
import com.baidu.iot.device.sdk.avatar.deviceside.Device;
import com.baidu.iot.device.sdk.avatar.deviceside.IoTDeviceFactory;
import com.baidu.iot.mqtt.common.MqttConfigFactory;
import com.baidu.iot.thing.avatar.operation.model.Status;

public class DeviceReportSample {

    public static void main(String[] args) {
        String iotCoreId = "xxxxxxx";
        int reportIntervalInSec = 1;
        long testTimeInSec = 10;
        PropertyKey reportKey = new PropertyKey("test");

        String deviceName = "test1";
        String username = "xxxxxxx/test1";
        char[] password = "xxxxxxxxxxxxxxxxx".toCharArray();

        String controllerName = "control";
        String controllerUsername = "xxxxxxx/control";
        char[] controllerPassword = "test".toCharArray();

        // init deviceside
        IoTDeviceFactory factory = new IoTDeviceFactory(IoTDeviceFactory.Config.builder()
                .iotCoreId(iotCoreId)
                .build());

        Device device = factory.getDevice(deviceName,
                MqttConfigFactory.genPlainMqttConfig(iotCoreId, username, password))
                .blockingGet();

        // init control side
        IoTDeviceControllerFactory deviceControllerFactory =
                new IoTDeviceControllerFactory(IoTDeviceControllerFactory.Config.builder()
                        .iotCoreId(iotCoreId)
                        .build());
        IoTDeviceController deviceController = deviceControllerFactory.createIoTDeviceController(
                controllerName,
                MqttConfigFactory.genPlainMqttConfig(iotCoreId, controllerUsername, controllerPassword))
                .blockingGet();
        ControlledDevice controlledDevice = deviceController.registerDevice(deviceName).blockingGet();

        controlledDevice.observeReportedDelta(reportKey)
                .observeOn(Schedulers.computation())
                .subscribe(new DisposableSubscriber<PropertyValue>() {
                    @Override
                    public void onNext(PropertyValue propertyValue) {
                        System.out.println("Receive reported delta:" + propertyValue);
                        request(1);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Observe reported error");
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        // waiting for observe
        Uninterruptibles.sleepUninterruptibly(Duration.ofSeconds(5));

        for (int i = 0; i < testTimeInSec / reportIntervalInSec; i ++) {
            Map<PropertyKey, PropertyValue> properties = new HashMap<>();
            PropertyValue reportValue = new PropertyValue(String.valueOf(i));
            properties.put(reportKey, reportValue);
            device.updateReported(properties).blockingSubscribe(new DisposableSingleObserver<Status>() {
                @Override
                public void onSuccess(@NonNull Status status) {
                    System.out.println("Update reported using " + reportValue + " success, status:" + status);
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    System.out.println("Update reported failure");
                    e.printStackTrace();
                }
            });
            Uninterruptibles.sleepUninterruptibly(Duration.ofSeconds(reportIntervalInSec));
        }

        device.close();
        factory.close();

        controlledDevice.close();
        deviceController.close();
        deviceControllerFactory.close();

        factory.closeSchedulers();
        deviceControllerFactory.closeSchedulers();
    }

}
