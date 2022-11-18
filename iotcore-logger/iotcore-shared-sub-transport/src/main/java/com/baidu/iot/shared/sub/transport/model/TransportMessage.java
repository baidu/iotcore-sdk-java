// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.shared.sub.transport.model;

import lombok.Builder;
import lombok.Data;

import com.baidu.iot.shared.sub.transport.enums.Qos;

@Data
@Builder
public class TransportMessage {

    private String transportId;
    private String topic;
    private Qos qos;
    private byte[] payload;

}
