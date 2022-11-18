// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery.exception;

/**
 * It is an exception that is thrown when the traffic is throttled and the Peer instance cannot accept more task.
 * If encountering it, one can re-try download / upload invocation after a while
 */
public class PeerThrottleException extends RuntimeException {
    public PeerThrottleException(String message) {
        super(message);
    }
}
