// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.exception;

/**
 * It is an exception that is thrown when the peer is waiting for too much time and will give up its current task.
 */
public class IoTCoreTimeoutException extends RuntimeException {
    public IoTCoreTimeoutException(String message) {
        super(message);
    }
}
