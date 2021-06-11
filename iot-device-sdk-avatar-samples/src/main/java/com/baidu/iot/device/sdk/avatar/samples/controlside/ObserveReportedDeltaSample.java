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

package com.baidu.iot.device.sdk.avatar.samples.controlside;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

import com.google.common.util.concurrent.Uninterruptibles;

import java.time.Duration;

import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.controlside.ControlledDevice;
import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceController;
import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceControllerFactory;
import com.baidu.iot.mqtt.common.MqttConfigFactory;

public class ObserveReportedDeltaSample {

    public static void main(String[] args) {
        String iotCoreId = "xxxxxxx";
        String deviceName = "test1";
        String controllerName = "control";
        String controllerUsername = "xxxxxxx/control";
        char[] controllerPassword = "test".toCharArray();
        long testTimeInSec = 10;

        IoTDeviceControllerFactory deviceControllerFactory =
                new IoTDeviceControllerFactory(IoTDeviceControllerFactory.Config.builder()
                        .iotCoreId(iotCoreId)
                        .build());
        IoTDeviceController deviceController = deviceControllerFactory.createIoTDeviceController(
                controllerName,
                MqttConfigFactory.genPlainMqttConfig(iotCoreId, controllerUsername, controllerPassword))
                .blockingGet();
        ControlledDevice controlledDevice = deviceController.registerDevice(deviceName).blockingGet();

        controlledDevice.observeReportedDelta(new PropertyKey("test"))
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
        controlledDevice.close();
        deviceController.close();
        deviceControllerFactory.close();
        deviceControllerFactory.closeSchedulers();
    }

}
