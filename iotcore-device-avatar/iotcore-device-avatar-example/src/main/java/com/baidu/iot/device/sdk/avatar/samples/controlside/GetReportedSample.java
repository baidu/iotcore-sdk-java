// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.samples.controlside;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;

import com.baidu.iot.device.sdk.avatar.controlside.ControlledDevice;
import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceController;
import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceControllerFactory;
import com.baidu.iot.device.sdk.avatar.controlside.VersionedPropertyValue;
import com.baidu.iot.mqtt.common.MqttConfigFactory;

public class GetReportedSample {

    public static void main(String[] args) {
        String iotCoreId = "xxxxxxx";
        String deviceName = "test1";
        String controllerName = "control";
        String controllerUsername = "xxxxxxx/control";
        char[] controllerPassword = "test".toCharArray();
        
        IoTDeviceControllerFactory deviceControllerFactory =
                new IoTDeviceControllerFactory(IoTDeviceControllerFactory.Config.builder()
                        .iotCoreId(iotCoreId)
                        .build());
        IoTDeviceController deviceController = deviceControllerFactory.createIoTDeviceController(
                controllerName,
                MqttConfigFactory.genPlainMqttConfig(iotCoreId, controllerUsername, controllerPassword))
                .blockingGet();
        ControlledDevice controlledDevice = deviceController.registerDevice(deviceName).blockingGet();

        controlledDevice.getReported().blockingSubscribe(new DisposableSingleObserver<VersionedPropertyValue>() {
            @Override
            public void onSuccess(@NonNull VersionedPropertyValue versionedPropertyValue) {
                System.out.println("Get reported success, reported:" + versionedPropertyValue);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                System.out.println("Get reported failure");
                e.printStackTrace();
            }
        });

        controlledDevice.close();
        deviceController.close();
        deviceControllerFactory.close();
        deviceControllerFactory.closeSchedulers();
    }

}
