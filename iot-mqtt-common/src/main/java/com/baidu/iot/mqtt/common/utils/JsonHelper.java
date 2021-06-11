/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.iot.mqtt.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Author zhangxiao18
 * Date 2020/10/7
 */
public class JsonHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JsonNode toJsonNode(String json) throws JsonProcessingException {
        return objectMapper.readTree(json);
    }

    public static String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public static ObjectNode createDefaultObjectNode() {
        return objectMapper.createObjectNode();
    }


}
