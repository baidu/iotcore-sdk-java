// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.task;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import com.baidu.iot.file.delivery.message.FileMetaData;
import com.baidu.iot.file.delivery.message.UploadDataMessage;
import com.baidu.iot.file.delivery.utils.TaskMessage;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UploadWriteTask extends AbstractWriteTask {
    public UploadWriteTask(FileMetaData fileMetaData,
                           String fileName,
                           String taskId,
                           BehaviorSubject<TaskMessage> taskSignal,
                           String subscribedTopicFilter,
                           long timeoutDelay,
                           ScheduledExecutorService scheduler) throws FileNotFoundException {
        super(taskId, taskSignal, subscribedTopicFilter, scheduler, timeoutDelay);
        this.fileMetaData = fileMetaData;
        this.bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileName),
                fileMetaData.getChunkSize());
        if (fileMetaData.getFileSize() == 0) {
            close();
        }
    }

    @Override
    public void doProcess(byte[] data) throws IOException {
        timeout = false;
        scheduledFuture.cancel(true);
        if (data == null || data.length == 0) {
            close();
        }
        UploadDataMessage uploadDataMessage = UploadDataMessage.parseFrom(data);
        if (uploadDataMessage.getSeq() == lastSeq + 1) {
            lastSeq++;
            byte[] chunk = uploadDataMessage.getChunk().toByteArray();
            fileSizeCount += chunk.length;
            log.trace("write data to disk, chunk size: {}, seq: {}", chunk.length, lastSeq);
            bufferedOutputStream.write(chunk);
            if (fileSizeCount >= fileMetaData.getFileSize()) {
                close();
            }else {
                timeout = true;
                countDown();
            }
        }else if (uploadDataMessage.getSeq() > lastSeq){
            log.warn("receive non-consecutive chunk, downloadSeq: {}, lastSeq: {}",
                    uploadDataMessage.getSeq(), lastSeq);
            close();
        }else {
            log.trace("receive dup seq: {} lastSeq: {}, ignore but reset timer", uploadDataMessage.getSeq(), lastSeq);
            timeout = true;
            countDown();
        }
    }

    @Override
    protected void close() {
        super.close();
        scheduledFuture.cancel(true);
        taskSignal.onNext(new TaskMessage(subscribedTopicFilter, taskId,
                TaskMessage.MessageType.FINISH_TRANSFER, null, 0L));
    }
}
