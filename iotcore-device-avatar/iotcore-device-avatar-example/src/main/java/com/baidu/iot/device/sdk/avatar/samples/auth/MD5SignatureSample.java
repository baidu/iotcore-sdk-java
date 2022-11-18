// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.samples.auth;

import com.baidu.iot.device.sdk.avatar.deviceside.Device;
import com.baidu.iot.device.sdk.avatar.deviceside.IoTDeviceFactory;
import com.baidu.iot.mqtt.common.MqttConfigFactory;

public class MD5SignatureSample {

    public static void main(String[] args) {
        String iotCoreId = "xxxxxxx";
        String deviceName = "test1";
        String password = "xxxxxxxxxxxxxxxxx";

        IoTDeviceFactory factory = new IoTDeviceFactory(IoTDeviceFactory.Config.builder()
                .iotCoreId(iotCoreId)
                .build());

        Device device = factory.getDevice(deviceName,
                MqttConfigFactory.genMD5SignatureMqttConfig(iotCoreId, deviceName, password)).blockingGet();

        System.out.println("Device init success");

        device.close();
        factory.close();
        factory.closeSchedulers();
    }

}
