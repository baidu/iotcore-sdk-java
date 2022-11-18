// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.samples.auth;

import java.io.File;
import java.io.FileNotFoundException;

import com.baidu.iot.device.sdk.avatar.deviceside.Device;
import com.baidu.iot.device.sdk.avatar.deviceside.IoTDeviceFactory;
import com.baidu.iot.mqtt.common.MqttConfigFactory;

public class SslSample {

    public static void main(String[] args) {
        String iotCoreId = "xxxxxxx";
        String deviceName = "test";
        // put your cert file to resources
        String deviceCertFileName = "xxxxxx";
        String deviceCertKeyFileInResourceName = "xxxxxxx";

        File caCertFile = new File(SslSample.class.getClassLoader().getResource("GlobalSign.cer").getFile());
        File deviceCertFile = new File(SslSample.class.getClassLoader().getResource(deviceCertFileName).getFile());
        File deviceCertKeyFile = new File(
                SslSample.class.getClassLoader().getResource(deviceCertKeyFileInResourceName).getFile());

        IoTDeviceFactory factory = new IoTDeviceFactory(IoTDeviceFactory.Config.builder()
                .iotCoreId(iotCoreId)
                .build());

        Device device;
        try {
            device = factory.getDevice(deviceName, MqttConfigFactory.genSSlMqttConfig(
                    iotCoreId, caCertFile, deviceCertFile, deviceCertKeyFile)).blockingGet();
        } catch (FileNotFoundException e) {
            System.out.println("Cert file not found");
            throw new RuntimeException(e);
        }

        System.out.println("Device init success");

        device.close();
        factory.close();
        factory.closeSchedulers();
    }

}
