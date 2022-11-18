// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.task;

import com.baidu.iot.file.delivery.message.FileMetaData;
import com.baidu.iot.file.delivery.message.UploadAck;
import com.baidu.iot.file.delivery.utils.IoTCoreFeedback;
import com.baidu.iot.file.delivery.utils.TaskMessage;
import com.baidu.iot.file.delivery.utils.UploadTopicHelper;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UploadResponseTask {

    private String remotePeer;
    private long sessionId;
    private String taskId;
    private FileMetaData fileMetaData;
    private BehaviorSubject<TaskMessage> taskSignal;
    private CompositeDisposable disposables;
    private int resendTimes;
    private String fileName;

    public UploadResponseTask(String fileName,
                              String remotePeer,
                              long sessionId,
                              String taskId,
                              FileMetaData fileMetaData,
                              BehaviorSubject<TaskMessage> taskSignal,
                              BehaviorSubject<IoTCoreFeedback> feedbackSignal) {
        this.fileName = fileName;
        this.remotePeer = remotePeer;
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.fileMetaData = fileMetaData;
        this.taskSignal = taskSignal;
        this.disposables = new CompositeDisposable();
        this.resendTimes = 0;
        disposables.add(feedbackSignal.subscribe(feedback -> {
            if (feedback.getTaskId().equals(taskId)) {
                IoTCoreFeedback.Feedback pubFeedPack = feedback.getFeedback();
                if (pubFeedPack == IoTCoreFeedback.Feedback.PUB_OK) {
                    close();
                }else {
                    log.info("pub ack failed, fileName: {}, resendTimes: {}", fileName, resendTimes);
                    if (++resendTimes > 3) {
                        close();
                    }else {
                        doProcess();
                    }
                }
            }
        }));
    }

    public void doProcess() {
        UploadAck uploadAck = UploadAck.newBuilder()
                .setReply(UploadAck.Reply.ACCEPTED)
                .build();
        if (resendTimes == 0) {
            triggerUploadReceive();
        }
        taskSignal.onNext(new TaskMessage(
                UploadTopicHelper.getUploadAckTopicFilter(remotePeer, sessionId),
                taskId,
                TaskMessage.MessageType.PUB_UPLOAD_ACK,
                uploadAck.toByteArray(), 0L));
    }

    private void triggerUploadReceive() {
        String receiveMgsTopicFilter = generateUploadReceiveTopicFilter();
        TaskMessage taskMessage = new TaskMessage(receiveMgsTopicFilter,
                taskId,
                TaskMessage.MessageType.TRIGGER_UPLOAD_RECEIVE,
                fileMetaData.toByteArray(),
                0L);
        taskMessage.setFileName(fileName);
        taskSignal.onNext(taskMessage);
    }

    private void close() {
        disposables.dispose();
    }

    private String generateUploadReceiveTopicFilter() {
        return UploadTopicHelper.getUploadMsgRecvTopicFilter(remotePeer, sessionId);
    }

}
