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

import com.baidu.iot.device.sdk.avatar.common.MqttConfigFactory;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.deviceside.Device;
import com.baidu.iot.device.sdk.avatar.deviceside.IoTDeviceFactory;
import com.baidu.iot.thing.avatar.operation.model.Status;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;

import java.util.HashMap;
import java.util.Map;

public class UpdateReportedSample {

    public static void main(String[] args) {
        String iotCoreId = "xxxxxxx";
        String deviceName = "test1";
        String username = "xxxxxxx/test1";
        char[] password = "xxxxxxxxxxxxxxxxx".toCharArray();

        IoTDeviceFactory factory = new IoTDeviceFactory(IoTDeviceFactory.Config.builder()
                .iotCoreId(iotCoreId)
                .build());

        Device device = factory.getDevice(deviceName,
                MqttConfigFactory.genPlainMqttConfig(iotCoreId, username, password)).blockingGet();

        Map<PropertyKey, PropertyValue> properties = new HashMap<>();
        properties.put(new PropertyKey("test"), new PropertyValue("\"test value\""));
        device.updateReported(properties).blockingSubscribe(new DisposableSingleObserver<Status>() {
            @Override
            public void onSuccess(@NonNull Status status) {
                System.out.println("Update reported success, status:" + status);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                System.out.println("Update reported failure");
                e.printStackTrace();
            }
        });

        device.close();
        factory.close();
        factory.closeSchedulers();
    }

}
