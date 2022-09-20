package com.baidu.iot.file.delivery;

import java.io.FileNotFoundException;

import org.eclipse.paho.client.mqttv3.MqttException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SDKDownloader {
    public static void main(String[] args) throws MqttException, FileNotFoundException {
        IPeer downloader = Peer.builder()
                .iotCoreId("aljllex")
                .userName("aljllex/retain-device-01")
                .password("lmAGKPFwKNfJZfsF")
                .fileHolderDir("/Users/gujiawei01/Desktop/baidu/iotcore-sdk-java/file/downloader")
                .chunkSize(32 * 1024)
                .peerId("testDownloader")
                .build();
        long startTime = System.currentTimeMillis();
        downloader.download("receiver-image.jpg", "testReceiver", "/Users/gujiawei01/Desktop/baidu/iotcore-sdk-java/file/downloader")
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
    }
}
