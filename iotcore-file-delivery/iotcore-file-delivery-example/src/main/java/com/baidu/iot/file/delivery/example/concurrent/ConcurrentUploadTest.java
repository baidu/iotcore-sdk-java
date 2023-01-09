// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.example.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.baidu.iot.file.delivery.IPeer;
import com.baidu.iot.file.delivery.Peer;
import com.baidu.iot.file.delivery.result.UploadResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConcurrentUploadTest {
    public static void main(String[] args) {
        try {
            String prefix = "iotcore-file-delivery-demo/src/main/resources/";
            String uploaderId = "uploader";
            String responderId = "responder";
            int taskNum = 5;
            IPeer uploader = Peer.builder()
                    .iotCoreId("your_IoTCoreId")
                    .userName("deviceKey")
                    .password("deviceSecret")
                    .fileHolderDir(prefix + uploaderId)
                    .chunkSize(32 * 1024)
                    .messageSendInterval(100L)
                    .waitForCompletion(50000L)
                    .peerId(uploaderId)
                    .build();

            IPeer responder = Peer.builder()
                    .iotCoreId("your_IoTCoreId")
                    .userName("deviceKey")
                    .password("deviceSecret")
                    .fileHolderDir(prefix + responderId)
                    .messageSendInterval(100L)
                    .waitForCompletion(50000L)
                    .chunkSize(32 * 1024)
                    .peerId(responderId)
                    .build();

            List<CompletableFuture<UploadResult>> futureList = new ArrayList<>();

            for (int index = 0; index < taskNum; index++) {
                int finalIndex = index;
                File file = new File(prefix + uploaderId + "/" + index + ".jpg");
                CompletableFuture<UploadResult> future = uploader
                        .upload(file, responderId)
                        .whenComplete((value, exception) -> {
                            if (exception != null) {
                                log.error("upload index: {}, file failed: ", finalIndex, exception);
                            }else {
                                log.info("upload index: {} file result: {}", finalIndex, value);
                            }
                        });
                futureList.add(future);
            }

            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()]))
                    .whenComplete((v , e) -> {
                        if (e != null) {
                            log.error("concurrent tasks failed: ", e);
                        }else {
                            log.info("concurrent tasks ok");
                        }
                        try {
                            uploader.close();
                            responder.close();
                        }catch (Exception exception) {
                            log.error("fail to close peer: ", exception);
                        }
                    });
        }catch (Exception e) {
            log.error("fail to upload files: ", e);
        }
    }
}
