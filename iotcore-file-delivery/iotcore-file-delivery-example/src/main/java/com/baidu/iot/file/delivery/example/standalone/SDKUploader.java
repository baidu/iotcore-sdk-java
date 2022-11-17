// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.example.standalone;

import java.io.File;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.baidu.iot.file.delivery.IPeer;
import com.baidu.iot.file.delivery.Peer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SDKUploader {
    public static void main(String[] args) {
        try {
            IPeer peer = Peer.builder()
                    .iotCoreId("your_IoTCoreId")
                    .userName("deviceKey")
                    .password("deviceSecret")
                    .fileHolderDir("your_fileDir")
                    .chunkSize(32 * 1024)
                    .peerId("testUploader")
                    .build();
            File file = new File("your_file");
            peer.upload(file, "testReceiver")
                    .whenComplete((val, err) -> {
                        if (err != null) {
                            log.error("failed to upload file: ", err);
                        }else {
                            log.trace("upload file result: {}", val);
                            try {
                                peer.close();
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }catch (Exception exception) {
            log.error("failed to upload file: ", exception);
        }
    }
}
