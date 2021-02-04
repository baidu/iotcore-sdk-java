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

package com.baidu.iot.device.sdk.avatar.samples.auth;

import com.baidu.iot.device.sdk.avatar.common.MqttConfigFactory;
import com.baidu.iot.device.sdk.avatar.deviceside.Device;
import com.baidu.iot.device.sdk.avatar.deviceside.IoTDeviceFactory;

import java.io.File;
import java.io.FileNotFoundException;

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
