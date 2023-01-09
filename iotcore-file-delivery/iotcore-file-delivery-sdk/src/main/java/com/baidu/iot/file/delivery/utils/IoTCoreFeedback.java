// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.utils;


import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class IoTCoreFeedback {
    public enum Feedback {
        PUB_OK,
        PUB_HEADER_OK,
        PUB_FAILED,
        SUB_OK,
        SUB_FAILED,
        UNSUB_OK,
        UNSUB_FAILED
    }

    private Feedback feedback;
    private String taskId;
    private long seq;
}
