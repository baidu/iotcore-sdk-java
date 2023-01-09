// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.example.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.baidu.iot.file.delivery.IPeer;
import com.baidu.iot.file.delivery.Peer;
import com.baidu.iot.file.delivery.result.DownloadResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConcurrentDownloadTest {
    public static void main(String[] args) {
        try {
            String prefix = "iotcore-file-delivery-demo/src/main/resources/";
            String downloaderId = "downloader";
            String responderId = "responder";
            int taskNum = 5;
            IPeer downloader = Peer.builder()
                    .iotCoreId("your_IoTCoreId")
                    .userName("deviceKey")
                    .password("deviceSecret")
                    .fileHolderDir(prefix + downloaderId)
                    .chunkSize(32 * 1024)
                    .messageSendInterval(100L)
                    .waitForCompletion(5000L)
                    .peerId(downloaderId)
                    .build();

            IPeer responder = Peer.builder()
                    .iotCoreId("your_IoTCoreId")
                    .userName("deviceKey")
                    .password("deviceSecret")
                    .fileHolderDir(prefix + responderId)
                    .messageSendInterval(100L)
                    .waitForCompletion(5000L)
                    .chunkSize(32 * 1024)
                    .peerId(responderId)
                    .build();

            List<CompletableFuture<DownloadResult>> futureList = new ArrayList<>();

            for (int index = 0; index < taskNum; index++) {
                int finalIndex = index;
                CompletableFuture<DownloadResult> future = downloader
                        .download(index + ".jpg", responderId, prefix + downloaderId)
                        .whenComplete((value, exception) -> {
                            if (exception != null) {
                                log.error("download index: {}, file failed: ", finalIndex, exception);
                            }else {
                                log.info("download index: {} file result: {}", finalIndex, value);
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
                            downloader.close();
                            responder.close();
                        }catch (Exception exception) {
                            log.error("fail to close peer: ", exception);
                        }
                    });
        }catch (Exception e) {
            log.error("fail to download files: ", e);
        }

    }
}
