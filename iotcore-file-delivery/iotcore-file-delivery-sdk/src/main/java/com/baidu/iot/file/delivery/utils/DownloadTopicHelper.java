// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.utils;

public class DownloadTopicHelper {
    private static final String DOWNLOAD_MSG_RECV_TOPIC_FILTER_FORMAT = "$iot/filereceive/%s/%d";
    private static final String DOWNLOAD_REQ_TOPIC_FILTER_FORMAT = "$iot/filerequest/%s";

    public static String getDownloadMsgRecvTopicFilter(String receiverId, long sessionId) {
        return String.format(DOWNLOAD_MSG_RECV_TOPIC_FILTER_FORMAT, receiverId, sessionId);
    }

    public static String getDownloadReqTopicFilter(String senderId) {
        return String.format(DOWNLOAD_REQ_TOPIC_FILTER_FORMAT, senderId);
    }


}
