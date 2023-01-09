// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.samples.deviceside;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;

import java.util.HashMap;
import java.util.Map;

import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.deviceside.Device;
import com.baidu.iot.device.sdk.avatar.deviceside.IoTDeviceFactory;
import com.baidu.iot.mqtt.common.MqttConfigFactory;
import com.baidu.iot.thing.avatar.operation.model.Status;

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
