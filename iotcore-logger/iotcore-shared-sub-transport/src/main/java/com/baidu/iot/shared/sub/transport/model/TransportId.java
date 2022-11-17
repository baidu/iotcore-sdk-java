// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.shared.sub.transport.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by mafei01 in 6/1/21 10:47 AM
 */
@EqualsAndHashCode(of = "id")
@Getter
public class TransportId {

    private static final String MQTT_CLIENT_ID_DISALLOW_CHARACTERS_REGEX = "\\W";
    private static final int MAX_MQTT_CLIENT_ID_LENGTH = 128;

    private final String id;

    private final String groupKey;

    private final String mqttIdPrefix;

    public TransportId(String id, String groupKey, String mqttIdPrefix) {
        this.id = id;
        this.groupKey = groupKey == null ? String.valueOf(System.nanoTime()) : groupKey;
        this.mqttIdPrefix = mqttIdPrefix == null ? this.id + "_" + this.groupKey : mqttIdPrefix;
    }

    public String genMqttClientId() {
        String result = mqttIdPrefix.replaceAll(MQTT_CLIENT_ID_DISALLOW_CHARACTERS_REGEX, "")
                + "_" + System.nanoTime();
        if (result.length() > MAX_MQTT_CLIENT_ID_LENGTH) {
            return result.substring(result.length() - MAX_MQTT_CLIENT_ID_LENGTH);
        }
        return result;
    }
}
