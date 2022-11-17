// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.example.standalone;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.baidu.iot.file.delivery.IPeer;
import com.baidu.iot.file.delivery.Peer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SDKDownloader {
    public static void main(String[] args) {
        log.info("---- test logback ----");
        try {
            IPeer downloader = Peer.builder()
                    .iotCoreId("your_IoTCoreId")
                    .userName("deviceKey")
                    .password("deviceSecret")
                    .fileHolderDir("your_fileDir")
                    .chunkSize(32 * 1024)
                    .peerId("testDownloader")
                    .build();
            long startTime = System.currentTimeMillis();
            downloader.download("fileName", "testReceiver", "your_fileDir")
                    .whenComplete((v, e) -> {
                        if (e != null) {
                            log.error("failed to download file ", e);
                        }else {
                            log.trace("finish downloading, duration: {}", (System.currentTimeMillis() - startTime));
                        }
                        try {
                            log.trace("closing downloader");
                            downloader.close();
                        } catch (MqttException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }catch (Exception exception) {
            log.error("failed to download file: ", exception);
        }
    }
}
