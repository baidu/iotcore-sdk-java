// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.task;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.baidu.iot.file.delivery.exception.IoTCoreTimeoutException;
import com.baidu.iot.file.delivery.message.UploadDataMessage;
import com.baidu.iot.file.delivery.result.UploadResult;
import com.baidu.iot.file.delivery.utils.IoTCoreFeedback;
import com.baidu.iot.file.delivery.utils.TaskMessage;
import com.google.protobuf.ByteString;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UploadReadTask extends AbstractReadTask{

    private CompletableFuture<UploadResult> future;
    private boolean firstChunkArrived = false;
    private ScheduledFuture timeoutFuture;
    private boolean isExceedMaxRetry;

    public UploadReadTask(String fileName,
                          String sendTopic,
                          int chunkSize,
                          CompletableFuture<UploadResult> future,
                          BehaviorSubject<TaskMessage> taskSignal,
                          BehaviorSubject<IoTCoreFeedback> feedbackSignal,
                          long timeoutDelay,
                          long messageSendInterval,
                          ScheduledExecutorService scheduler) throws FileNotFoundException {
        super(fileName, chunkSize, taskSignal, feedbackSignal);
        this.sendTopic = sendTopic;
        this.future = future;
        this.isExceedMaxRetry = false;
        this.timeoutFuture = scheduler.schedule(() -> {
            if (!firstChunkArrived) {
                future.completeExceptionally(new IoTCoreTimeoutException("close task due to late ack"));
                close();
            }
        }, timeoutDelay, TimeUnit.MILLISECONDS);
        disposables.add(feedbackSignal.subscribe(feedback -> {
            if (feedback.getTaskId().equals(taskId)) {
                IoTCoreFeedback.Feedback pubFeedback = feedback.getFeedback();
                if (pubFeedback == IoTCoreFeedback.Feedback.PUB_OK) {
                    unConfirmedTaskMsg = null;
                    scheduledFuture = scheduler.schedule(() -> readAndSend(),
                            messageSendInterval, TimeUnit.MILLISECONDS);
                }else {
                    if (++unConfirmedTaskMsg.resendTimes > 3) {
                        isExceedMaxRetry = true;
                        close();
                    }else {
                        taskSignal.onNext(unConfirmedTaskMsg.taskMessage);
                    }
                }
            }
        }));
    }

    @Override
    public void doProcess() {
        firstChunkArrived = true;
        readAndSend();
    }

    private void readAndSend() {
        byte[] chunk = null;
        try {
            chunk = reader.read();
        } catch (IOException e) {
            log.error("read file error, fileName: {}", absoluteFileName, e);
            future.completeExceptionally(e);
            close();
        }
        if (chunk == null) {
            endOfFile = true;;
        }else {
            TaskMessage taskMessage = new TaskMessage(sendTopic,
                    taskId,
                    TaskMessage.MessageType.PUB_DATA,
                    UploadDataMessage.newBuilder()
                            .setSeq(seq)
                            .setChunk(ByteString.copyFrom(chunk))
                            .build().toByteArray(),
                    seq);
            unConfirmedTaskMsg = new InFlightTaskMessage(taskMessage, 0);
            seq++;
            taskSignal.onNext(taskMessage);
        }
        if (endOfFile == true) {
            close();
        }
    }

    @Override
    public void close() {
        super.close();
        timeoutFuture.cancel(true);
        if (!future.isDone()) {
            if (isExceedMaxRetry) {
                future.completeExceptionally(new IoTCoreTimeoutException("exceed maximum re-try times"));
            }else {
                future.complete(UploadResult.OK);
            }
        }
    }
}
