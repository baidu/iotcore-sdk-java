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

package com.baidu.iot.device.sdk.avatar.common.internal.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.baidu.iot.device.sdk.avatar.common.Constants;
import com.baidu.iot.device.sdk.avatar.common.internal.model.Avatar;
import com.baidu.iot.mqtt.common.utils.JsonHelper;

/**
 * Created by zhuchenhao at 2019/11/29
 */
public class AvatarHelper {

    public static Avatar buildAvatar(String avatarId, String avatarJson) throws JsonProcessingException {
        JsonNode node = JsonHelper.toJsonNode(avatarJson);
        return Avatar.builder()
                .id(avatarId)
                .tags(node.get(Constants.AVATAR_TAGS_FIELD_NAME))
                .reported(node.get(Constants.AVATAR_REPORTED_FIELD_NAME))
                .desired(node.get(Constants.AVATAR_DESIRED_FIELD_NAME))
                .build();
    }

    public static Avatar buildDefaultAvatar(String avatarId) {
        return Avatar.builder()
                .id(avatarId)
                .tags(JsonHelper.createDefaultObjectNode())
                .reported(JsonHelper.createDefaultObjectNode())
                .desired(JsonHelper.createDefaultObjectNode())
                .build();
    }

    public static String buildDefaultAvatarJson() {
        ObjectNode root = JsonHelper.createDefaultObjectNode();
        root.set(Constants.AVATAR_TAGS_FIELD_NAME, initMetadataNode());
        root.set(Constants.AVATAR_REPORTED_FIELD_NAME, initMetadataNode());
        root.set(Constants.AVATAR_DESIRED_FIELD_NAME, initMetadataNode());
        try {
            return JsonHelper.toJson(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonNode initMetadataNode() {
        ObjectNode tags = JsonHelper.createDefaultObjectNode();
        tags.put(Constants.AVATAR_VERSION_FIELD_NAME, 0);
        return tags;
    }

}
