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
