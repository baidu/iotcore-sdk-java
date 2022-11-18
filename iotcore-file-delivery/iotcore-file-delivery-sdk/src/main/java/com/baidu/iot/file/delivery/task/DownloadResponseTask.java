// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.task;

import java.io.File;

import com.baidu.iot.file.delivery.message.DownloadDataMessage;
import com.baidu.iot.file.delivery.message.DownloadHeader;
import com.baidu.iot.file.delivery.message.FileMetaData;
import com.baidu.iot.file.delivery.utils.DownloadTopicHelper;
import com.baidu.iot.file.delivery.utils.IoTCoreFeedback;
import com.baidu.iot.file.delivery.utils.TaskMessage;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class DownloadResponseTask {
    enum DownloadResponseState {
        STOPPED,
        ON_GOING
    }

    private String taskId;
    private BehaviorSubject<TaskMessage> taskSignal;
    private CompositeDisposable disposables;
    private int resendTimes;
    private int chunkSize;
    private File file;
    private String downloadMsgReceiveTopicFilter;
    private DownloadResponseState state;

    public DownloadResponseTask(String remotePeer,
                                long sessionId,
                                String taskId,
                                String fileHolderDir,
                                String fileName,
                                int chunkSize,
                                BehaviorSubject<TaskMessage> taskSignal,
                                BehaviorSubject<IoTCoreFeedback> feedbackSignal) {
        this.taskId = taskId;
        this.file = new File(fileHolderDir + "/" + fileName);
        this.chunkSize = chunkSize;
        this.taskSignal = taskSignal;
        this.disposables = new CompositeDisposable();
        this.resendTimes = 0;
        this.downloadMsgReceiveTopicFilter = DownloadTopicHelper.getDownloadMsgRecvTopicFilter(remotePeer, sessionId);
        this.state = DownloadResponseState.ON_GOING;
        disposables.add(feedbackSignal.subscribe(feedback -> {
            if (state == DownloadResponseState.ON_GOING && feedback.getTaskId().equals(taskId)) {
                IoTCoreFeedback.Feedback pubFeedBack = feedback.getFeedback();
                if (pubFeedBack == IoTCoreFeedback.Feedback.PUB_HEADER_OK) {
                    close();
                    triggerDownloadSend();
                }else {
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
        DownloadHeader header;
        if (!canRead()) {
            state = DownloadResponseState.STOPPED;
            header = DownloadHeader.newBuilder()
                    .setReply(DownloadHeader.Reply.REJECTED)
                    .build();
            TaskMessage finishMessage = new TaskMessage(downloadMsgReceiveTopicFilter,
                    taskId, TaskMessage.MessageType.FINISH_TRANSFER, null, 0L);
            taskSignal.onNext(finishMessage);
        }else {
            header = DownloadHeader.newBuilder()
                    .setReply(DownloadHeader.Reply.ACCEPTED)
                    .setFileMetaData(FileMetaData.newBuilder()
                            .setChunkSize(chunkSize)
                            .setFileSize(file.length())
                            .build())
                    .build();
        }
        DownloadDataMessage dataMessage = DownloadDataMessage.newBuilder().setHeader(header).build();
        TaskMessage taskMessage = new TaskMessage(downloadMsgReceiveTopicFilter,
                taskId, TaskMessage.MessageType.PUB_DOWNLOAD_HEADER, dataMessage.toByteArray(), 0L);
        taskMessage.setFileName(file.getName());
        taskSignal.onNext(taskMessage);
    }

    private boolean canRead() {
        if (!file.exists() || !file.canRead()) {
            return false;
        }
        return true;
    }

    private void triggerDownloadSend() {
        TaskMessage taskMessage = new TaskMessage(downloadMsgReceiveTopicFilter,
                taskId,
                TaskMessage.MessageType.TRIGGER_DOWNLOAD_SEND,
                null,
                0L);
        taskMessage.setFileName(file.getName());
        taskSignal.onNext(taskMessage);
    }

    public void close() {
        disposables.dispose();
    }
}
