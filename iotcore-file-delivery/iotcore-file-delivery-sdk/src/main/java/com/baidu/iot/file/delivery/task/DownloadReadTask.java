// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.task;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.baidu.iot.file.delivery.message.Download;
import com.baidu.iot.file.delivery.message.DownloadDataMessage;
import com.baidu.iot.file.delivery.utils.IoTCoreFeedback;
import com.baidu.iot.file.delivery.utils.TaskMessage;
import com.google.protobuf.ByteString;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadReadTask extends AbstractReadTask {

    public DownloadReadTask(String fileName,
                            String dir,
                            String sendTopic,
                            String taskId,
                            int chunkSize,
                            BehaviorSubject<TaskMessage> taskSignal,
                            BehaviorSubject<IoTCoreFeedback> feedbackSignal,
                            ScheduledExecutorService scheduler,
                            long messageSendInterval) throws FileNotFoundException {
        super(dir + "/" + fileName, chunkSize, taskSignal, feedbackSignal);
        this.sendTopic = sendTopic;
        this.taskId = taskId;
        disposables.add(feedbackSignal.subscribe(feedback -> {
            if (feedback.getTaskId().equals(taskId)) {
                IoTCoreFeedback.Feedback pubFeedback = feedback.getFeedback();
                if (pubFeedback == IoTCoreFeedback.Feedback.PUB_HEADER_OK) {
                    return;
                }
                if (pubFeedback == IoTCoreFeedback.Feedback.PUB_OK) {
                    unConfirmedTaskMsg = null;
                    scheduledFuture = scheduler.schedule(() -> readAndSend(),
                            messageSendInterval, TimeUnit.MILLISECONDS);
                }else {
                    if (++unConfirmedTaskMsg.resendTimes > 3) {
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
        readAndSend();
    }

    private void readAndSend() {
        byte[] chunk = null;
        try {
            chunk = reader.read();
        } catch (IOException e) {
            log.error("read file error: ", e);
        }
        if (chunk == null) {
            endOfFile = true;
        }else {
            TaskMessage taskMessage = new TaskMessage(sendTopic,
                    taskId,
                    TaskMessage.MessageType.PUB_DATA,
                    DownloadDataMessage.newBuilder()
                            .setDownload(Download.newBuilder()
                                    .setChunkData(ByteString.copyFrom(chunk))
                                    .setSeq(seq)
                                    .build())
                            .build().toByteArray(),
                    seq);
            unConfirmedTaskMsg = new InFlightTaskMessage(taskMessage, 0);
            seq++;
            log.trace("read data, seq: {}, taskId: {}", seq, taskId);
            taskSignal.onNext(taskMessage);
        }
        if (endOfFile == true) {
            close();
        }
    }

    @Override
    public void close() {
        super.close();
        taskSignal.onNext(new TaskMessage(taskId, taskId, TaskMessage.MessageType.FINISH_TRANSFER, null, 0L));
    }
}
