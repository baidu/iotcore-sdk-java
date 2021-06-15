/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.iot.shared.sub.transport;

import lombok.Builder;
import lombok.Getter;

import com.baidu.iot.mqtt.common.MqttConfig;
import com.baidu.iot.shared.sub.transport.enums.Qos;
import com.baidu.iot.shared.sub.transport.model.TransportId;

/**
 * Author zhangxiao18
 * Date 2020/11/18
 */
@Getter
@Builder(toBuilder = true)
public class MqttTransportConfig {

    private final TransportId transportId;

    private final MqttConfig mqttConfig;

    private final String[] topicFilters;

    @Builder.Default
    private final int clientPoolSize = 1;

    @Builder.Default
    private final Qos qos = Qos.AT_LEAST_ONCE;

    @Builder.Default
    private final int maxInflightMessageNum = 200;

    @Builder.Default
    private final long timeoutInMs = 10000L;

}
