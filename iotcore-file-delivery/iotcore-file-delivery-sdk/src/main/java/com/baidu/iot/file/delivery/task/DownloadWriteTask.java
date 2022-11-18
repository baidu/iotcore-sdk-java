// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.task;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import com.baidu.iot.file.delivery.exception.IoTCoreTimeoutException;
import com.baidu.iot.file.delivery.message.Download;
import com.baidu.iot.file.delivery.message.DownloadDataMessage;
import com.baidu.iot.file.delivery.message.FileMetaData;
import com.baidu.iot.file.delivery.result.DownloadResult;
import com.baidu.iot.file.delivery.utils.TaskMessage;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadWriteTask extends AbstractWriteTask {
    private CompletableFuture<DownloadResult> future;

    public DownloadWriteTask(BehaviorSubject<TaskMessage> taskSignal,
                             String subscribedTopicFilter,
                             ScheduledExecutorService scheduler,
                             long timeoutDelay,
                             CompletableFuture<DownloadResult> future) {
        super(UUID.randomUUID().toString(), taskSignal, subscribedTopicFilter, scheduler, timeoutDelay);
        this.future = future;
    }

    public void setFileMetaDataAndWriteBuffer(FileMetaData fileMetaData,
                                              String fileName, long fileSize) throws FileNotFoundException {
        this.fileMetaData = fileMetaData;
        this.bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileName),
                fileMetaData.getChunkSize());
        if (fileSize == 0) {
            close(DownloadResult.OK);
        }
    }

    public FileMetaData getFileMetaData() {
        return this.fileMetaData;
    }

    @Override
    public void doProcess(byte[] data) throws IOException {
        timeout = false;
        scheduledFuture.cancel(true);
        if (data == null || data.length == 0) {
            close(DownloadResult.OK);
            return;
        }
        Download download = DownloadDataMessage.parseFrom(data).getDownload();
        if (download.getSeq() == lastSeq + 1) {
            lastSeq++;
            byte[] chunk = download.getChunkData().toByteArray();
            fileSizeCount += chunk.length;
            bufferedOutputStream.write(chunk);
            log.trace("write data, lastSeq: {}, taskId: {}", lastSeq, taskId);
            if (fileSizeCount >= fileMetaData.getFileSize()) {
                close(DownloadResult.OK);
            }else {
                timeout = true;
                countDown();
            }
        }else if (download.getSeq() > lastSeq) {
            log.warn("receive non-consecutive chunk, downloadSeq: {}, lastSeq: {}", download.getSeq(), lastSeq);
            close(DownloadResult.FAILED);
        }else {
            log.trace("receive dup seq: {} lastSeq: {}, ignore but reset timer", download.getSeq(), lastSeq);
            timeout = true;
            countDown();
        }

    }

    public void close(DownloadResult result) {
        close();
        if (timeout == true && result != DownloadResult.OK) {
            future.completeExceptionally(new IoTCoreTimeoutException("wait too much idle time for next chunk"));
        }else {
            future.complete(result);
        }
    }
}
