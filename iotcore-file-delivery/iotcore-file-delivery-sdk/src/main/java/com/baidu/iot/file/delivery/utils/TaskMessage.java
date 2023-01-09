// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.utils;

import lombok.Data;

@Data
public class TaskMessage {
    public enum MessageType {
        PUB_DATA,
        PUB_UPLOAD_ACK,
        PUB_DOWNLOAD_HEADER,
        TRIGGER_UPLOAD_RECEIVE,
        TRIGGER_DOWNLOAD_SEND,
        UNSUB,
        FINISH_TRANSFER
    }

    private String topic;
    private String taskId;
    private MessageType messageType;
    private byte[] payload;
    private long seq;
    private String fileName;

    public TaskMessage(String topic,
                       String taskId,
                       MessageType messageType,
                       byte[] payload,
                       long seq) {
        this.topic = topic;
        this.taskId = taskId;
        this.messageType = messageType;
        this.payload = payload;
        this.seq = seq;
    }
}
