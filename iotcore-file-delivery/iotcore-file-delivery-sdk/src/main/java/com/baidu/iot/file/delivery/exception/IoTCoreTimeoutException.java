// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.exception;

public class IoTCoreTimeoutException extends RuntimeException {
    public IoTCoreTimeoutException(String message) {
        super(message);
    }

    public IoTCoreTimeoutException(Throwable cause) {
        super(cause);
    }

    public IoTCoreTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
