package com.baidu.iot.file.delivery;

import org.eclipse.paho.client.mqttv3.MqttException;

public class SDKResponder {
    public static void main(String[] args) throws MqttException {
        Peer.builder()
                .iotCoreId("aljllex")
                .userName("aljllex/retain-device-01")
                .password("lmAGKPFwKNfJZfsF")
                .fileHolderDir("/Users/gujiawei01/Desktop/baidu/iotcore-sdk-java/file/receiver")
                .chunkSize(32 * 1024)
                .peerId("testReceiver")
                .build();
    }
}
