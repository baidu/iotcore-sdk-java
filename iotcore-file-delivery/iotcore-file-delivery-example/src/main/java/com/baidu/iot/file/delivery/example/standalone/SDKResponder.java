// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.example.standalone;

import com.baidu.iot.file.delivery.Peer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SDKResponder {
    public static void main(String[] args) {
        try {
            Peer.builder()
                    .iotCoreId("your_IoTCoreId")
                    .userName("deviceKey")
                    .password("deviceSecret")
                    .fileHolderDir("your_fileDir")
                    .chunkSize(32 * 1024)
                    .peerId("testReceiver")
                    .build();
        }catch (Exception exception) {
            log.error("failed to instance responder peer: ", exception);
        }
    }
}
