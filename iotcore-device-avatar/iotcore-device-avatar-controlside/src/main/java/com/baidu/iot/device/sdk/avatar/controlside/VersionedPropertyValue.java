// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.controlside;

import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

/**
 * Author zhangxiao18
 * Date 2020/11/17
 */
public class VersionedPropertyValue extends PropertyValue {

    @Getter
    private final int version;

    public VersionedPropertyValue(String json, int version) {
        super(json);
        this.version = version;
    }

    public VersionedPropertyValue(JsonNode jsonNode, int version) {
        super(jsonNode);
        this.version = version;
    }

    @Override
    public String toString() {
        return "VersionedPropertyValue{" + "version=" + version + ",value=" + super.toString() + '}';
    }
}
