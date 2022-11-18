// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.utils;

public class UploadTopicHelper {
    private static final String UPLOAD_ACK_TOPIC_FILTER_FORMAT = "$iot/filesend/ack/%s/%d";
    private static final String UPLOAD_LISTENER_TOPIC_FILTER_FORMAT = "$iot/filesend/%s";
    private static final String UPLOAD_MSG_RECV_TOPIC_FILTER_FORMAT = "$iot/filesend/%s/%d";

    public static String getUploadAckTopicFilter(String uploaderId, long sessionId) {
        return String.format(UPLOAD_ACK_TOPIC_FILTER_FORMAT, uploaderId, sessionId);
    }

    public static String getUploadListenerTopicFilter(String receiverId) {
        return String.format(UPLOAD_LISTENER_TOPIC_FILTER_FORMAT, receiverId);
    }

    public static String getUploadMsgRecvTopicFilter(String receiverId, long sessionId) {
        return String.format(UPLOAD_MSG_RECV_TOPIC_FILTER_FORMAT, receiverId, sessionId);
    }
}
