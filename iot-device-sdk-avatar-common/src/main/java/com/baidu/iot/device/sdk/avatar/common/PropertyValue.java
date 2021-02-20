/*
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.iot.device.sdk.avatar.common;

import com.baidu.iot.device.sdk.avatar.common.utils.JsonHelper;
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

    @Override
    public String toString() {
        try {
            return JsonHelper.toJson(jsonNode);
        } catch (JsonProcessingException e) {
            // TODO specific exception
            throw new RuntimeException(e);
        }
    }

    /*
    parent:
    {
        "a": 3,
        "b": {
            "c": 5
        }
    }

    keyEntries: empty -> return: parent
    keyEntries: a -> return: 3
    keyEntries: b -> return: { c: 5 }
    keyEntries: b,c -> return: 5
    keyEntries: a,d -> return: null
    keyEntries: d -> return: null
     */
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

}
