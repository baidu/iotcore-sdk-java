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
