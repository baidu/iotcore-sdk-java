// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common;

import com.baidu.iot.mqtt.common.utils.JsonHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Author zhangxiao18
 * Date 2020/9/24
 */
public class PropertyValue {

    public static final PropertyValue EMPTY = new PropertyValue("{}");

    private JsonNode jsonNode;

    public PropertyValue(String json) {
        try {
            this.jsonNode = JsonHelper.toJsonNode(json);
        } catch (JsonProcessingException e) {
            // TODO specific exception
            throw new RuntimeException(e);
        }
    }

    public JsonNode toJsonNode() {
        return jsonNode;
    }

    public PropertyValue(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public PropertyValue getValue(PropertyKey key) {
        return new PropertyValue(getValue(jsonNode, key.getEntries()));
    }

    private static JsonNode getValue(JsonNode parent, List<String> keyEntries) {
        if (keyEntries.isEmpty()) {
            return parent;
        }
        if (parent instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) parent;
            if (objectNode.has(keyEntries.get(0))) {
                String fieldName = keyEntries.remove(0);
                return getValue(objectNode.get(fieldName), keyEntries);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        try {
            return JsonHelper.toJson(jsonNode);
        } catch (JsonProcessingException e) {
            // TODO specific exception
            throw new RuntimeException(e);
        }
    }

}
