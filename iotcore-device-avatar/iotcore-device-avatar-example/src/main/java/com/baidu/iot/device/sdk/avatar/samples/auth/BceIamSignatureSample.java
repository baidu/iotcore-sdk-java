// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.samples.auth;

import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceController;
import com.baidu.iot.device.sdk.avatar.controlside.IoTDeviceControllerFactory;
import com.baidu.iot.mqtt.common.MqttConfigFactory;

public class BceIamSignatureSample {

    public static void main(String[] args) {
        String iotCoreId = "xxxxxxx";
        String appName = "test";
        String appKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        String secretKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

        IoTDeviceControllerFactory deviceControllerFactory =
                new IoTDeviceControllerFactory(IoTDeviceControllerFactory.Config.builder()
                        .iotCoreId(iotCoreId)
                        .build());
        IoTDeviceController deviceController = deviceControllerFactory.createIoTDeviceController(
                appName,
                MqttConfigFactory.genBceIamSignatureMqttConfig(iotCoreId, appKey, secretKey))
                .blockingGet();

        System.out.println("DeviceController init success");

        deviceController.close();
        deviceControllerFactory.close();
        deviceControllerFactory.closeSchedulers();
    }

}
