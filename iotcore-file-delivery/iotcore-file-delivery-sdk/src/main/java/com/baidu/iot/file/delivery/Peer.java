// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.baidu.iot.file.delivery.result.DownloadResult;
import com.baidu.iot.file.delivery.result.UploadResult;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.Builder;
import lombok.NonNull;

public class Peer implements IPeer {

    private String iotCoreId;
    private final IoTCoreClient client;

    /**
     * Create an instance for message delivery
     *
     * @param iotCoreId the IoTCoreId for IoTCore service
     * @param userName the userName for connection
     * @param password the password for connection
     * @param peerId the peer id
     * @param fileHolderDir the files' directory
     * @param chunkSize the chunkSize for file slicing, default value is 32 kb
     * @param messageSendInterval the interval during chunk sending
     * @param waitForCompletion the maximum waiting time for communicating with IoTCore service
     * @param maxRetryTimes the maximum re-try times for any failure communication with IoTCore
     * @param scheduler the shared scheduler across paho SDK and sub-task during file delivery
     * @throws MqttException if any other problem was encountered during the interaction with IoTCore service
     */
    @Builder
    public Peer(@NonNull String iotCoreId,
                @NonNull String userName,
                @NonNull String password,
                @NonNull String peerId,
                @NonNull String fileHolderDir,
                Integer chunkSize,
                Long messageSendInterval,
                Long waitForCompletion,
                Integer maxRetryTimes,
                ScheduledExecutorService scheduler) throws MqttException {
        this.iotCoreId = iotCoreId;
        scheduler = scheduler == null ? new ScheduledThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() / 2,
                new ThreadFactoryBuilder().setNameFormat(iotCoreId + "-" + peerId + "-%d").build())
                : scheduler;
        int cnkSize = 32 * 1024;
        if (chunkSize != null) {
            cnkSize = chunkSize;
        }
        int maxInflightTasks = 512 * 1024 / cnkSize;
        this.client = new IoTCoreClient(getEndpoint(), userName, password, peerId,
                fileHolderDir,
                cnkSize,
                messageSendInterval == null ? 3000 : messageSendInterval,
                waitForCompletion == null ? 5000 : waitForCompletion,
                maxRetryTimes == null ? 3 : maxRetryTimes,
                scheduler,
                maxInflightTasks);
    }

    @Override
    public CompletableFuture<DownloadResult> download(String fileName,
                                                      String remotePeer,
                                                      String dir) {
        if (!hasWritePermission(dir)) {
            return CompletableFuture.completedFuture(DownloadResult.NO_PERMISSION);
        }
        return client.downloadFile(fileName, remotePeer, dir);
    }

    @Override
    public CompletableFuture<UploadResult> upload(File file, String remotePeer) {
        if (!file.canRead()) {
            return CompletableFuture.completedFuture(UploadResult.FAILED);
        }
        return client.uploadFile(file, remotePeer);
    }

    @Override
    public void close() throws MqttException {
        client.close();
    }

    private boolean hasWritePermission(String dir) {
        return Files.isWritable(Paths.get(dir));
    }

    private String getEndpoint() {
        return "tcp://" + iotCoreId + ".iot.gz.baidubce.com:1883";
    }
}
