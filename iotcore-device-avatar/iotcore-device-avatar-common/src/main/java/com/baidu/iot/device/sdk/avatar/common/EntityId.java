// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Author zhangxiao18
 * Date 2020/9/29
 */
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class EntityId {

    private static final String MQTT_CLIENT_ID_DISALLOW_CHARACTERS_REGEX = "\\W";

    private static final int MAX_MQTT_CLIENT_ID_LENGTH = 128;

    @Getter
    private String iotCoreId;

    @Getter
    private String id;

    public String genMqttClientId() {
        // TODO use timestamp and random.
        String result = iotCoreId
                + "_"
                + id.replaceAll(MQTT_CLIENT_ID_DISALLOW_CHARACTERS_REGEX, "")
                + ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        if (result.length() > MAX_MQTT_CLIENT_ID_LENGTH) {
            return result.substring(0, MAX_MQTT_CLIENT_ID_LENGTH);
        }
        return result;
    }

}
