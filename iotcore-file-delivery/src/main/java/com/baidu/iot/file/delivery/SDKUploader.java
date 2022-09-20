package com.baidu.iot.file.delivery;

import java.io.File;

import org.eclipse.paho.client.mqttv3.MqttException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SDKUploader {
    public static void main(String[] args) throws MqttException {
        IPeer peer = Peer.builder()
                .iotCoreId("aljllex")
                .userName("aljllex/retain-device-01")
                .password("lmAGKPFwKNfJZfsF")
                .fileHolderDir("/Users/gujiawei01/Desktop/baidu/iotcore-sdk-java/file/uploader")
                .chunkSize(32 * 1024)
                .peerId("testUploader")
                .build();
        File file = new File("/Users/gujiawei01/Desktop/baidu/iotcore-sdk-java/file/uploader/uploader-image.jpg");
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
    }
}
