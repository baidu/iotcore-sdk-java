/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.iot.log.sdk;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import com.baidu.iot.log.sdk.enums.LogLevel;
import com.baidu.iot.mqtt.common.MqttConfig;

/**
 * Created by mafei01 in 6/4/21 1:33 PM
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "iotCoreId")
public class Config {

    private final String iotCoreId;

    private final String groupKey;

    private final LogLevel logLevel;

    private final String[] deviceKeys;

    private final MqttConfig mqttConfig;

    @Builder.Default
    private final boolean includeLowerLevel = true;

    @Builder.Default
    private final int clientPoolSize = 1;
}
