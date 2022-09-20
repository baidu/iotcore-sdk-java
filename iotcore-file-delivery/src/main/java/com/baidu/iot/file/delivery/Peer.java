package com.baidu.iot.file.delivery;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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
        scheduler = scheduler == null ? new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() / 2,
                new ThreadFactoryBuilder().setNameFormat(iotCoreId + "-" + peerId + "-%d").build())
                : scheduler;
        this.client = new IoTCoreClient(getEndpoint(), userName, password, peerId,
                fileHolderDir,
                chunkSize == null ? 32 * 1024 : chunkSize,
                messageSendInterval == null ? 3000 : messageSendInterval,
                waitForCompletion == null ? 5000 : waitForCompletion,
                maxRetryTimes == null ? 3 : maxRetryTimes,
                scheduler);
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
        return Files.isWritable(Path.of(dir));
    }

    private String getEndpoint() {
        return "tcp://" + iotCoreId + ".iot.gz.baidubce.com:1883";
    }
}
