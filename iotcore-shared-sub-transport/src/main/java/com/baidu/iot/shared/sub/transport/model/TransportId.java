/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.iot.shared.sub.transport.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by mafei01 in 6/1/21 10:47 AM
 */
@EqualsAndHashCode(of = "id")
@Getter
public class TransportId {

    private static final String mqttClientIdDisallowCharactersRegex = "\\W";
    private static final int maxMqttClientIdLength = 128;

    private final String id;

    private final String groupKey;

    private final String mqttIdPrefix;

    public TransportId(String id, String groupKey, String mqttIdPrefix) {
        this.id = id;
        this.groupKey = groupKey == null ? String.valueOf(System.nanoTime()) : groupKey;
        this.mqttIdPrefix = mqttIdPrefix == null ? this.id + "_" + this.groupKey : mqttIdPrefix;
    }

    public String genMqttClientId() {
        String result = mqttIdPrefix.replaceAll(mqttClientIdDisallowCharactersRegex, "") + "_" + System.nanoTime();
        if (result.length() > maxMqttClientIdLength) {
            return result.substring(result.length() - maxMqttClientIdLength);
        }
        return result;
    }
}
