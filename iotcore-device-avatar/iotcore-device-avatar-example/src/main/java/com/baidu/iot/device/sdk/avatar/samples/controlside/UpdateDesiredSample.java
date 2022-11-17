// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.samples.controlside;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;

import java.util.HashMap;
import java.util.Map;

import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.controlside.ControlledDevice;
import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceController;
import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceControllerFactory;
import com.baidu.iot.mqtt.common.MqttConfigFactory;
import com.baidu.iot.thing.avatar.operation.model.Status;

public class UpdateDesiredSample {

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

        Map<PropertyKey, PropertyValue> properties = new HashMap<>();
        properties.put(new PropertyKey("test"), new PropertyValue("\"desired value\""));
        controlledDevice.updateDesired(properties).blockingSubscribe(new DisposableSingleObserver<Status>() {
            @Override
            public void onSuccess(@NonNull Status status) {
                System.out.println("Update desired success, status:" + status);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                System.out.println("Update desired failure");
                e.printStackTrace();
            }
        });

        controlledDevice.close();
        deviceController.close();
        deviceControllerFactory.close();
        deviceControllerFactory.closeSchedulers();
    }

}
