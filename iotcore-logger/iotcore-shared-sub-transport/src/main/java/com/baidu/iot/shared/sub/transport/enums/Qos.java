// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.shared.sub.transport.enums;

/**
 * Created by mafei01 in 6/1/21 10:25 AM
 */
public enum  Qos {

    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2),
    FAILURE(0x80);

    private final int value;

    Qos(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static Qos valueOf(int value) {
        switch (value) {
            case 0:
                return AT_MOST_ONCE;
            case 1:
                return AT_LEAST_ONCE;
            case 2:
                return EXACTLY_ONCE;
            case 0x80:
                return FAILURE;
            default:
                throw new IllegalArgumentException("invalid QoS: " + value);
        }
    }
}
