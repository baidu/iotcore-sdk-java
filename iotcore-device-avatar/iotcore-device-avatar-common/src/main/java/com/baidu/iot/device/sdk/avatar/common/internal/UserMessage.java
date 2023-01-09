// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal;

import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarRequest;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 *
 * Author zhangxiao18
 * Date 2020/9/23
 */
@Getter
public class UserMessage implements Message {
    public UserMessage(MessageId messageId, GeneratedMessageV3 content) {
        this.id = messageId;
        this.content = content;
    }

    private final MessageId id;

    private final GeneratedMessageV3 content;

    public static UserMessage genGetMessage(GetAvatarRequest request) {
        MessageId id = MessageId.next();
        GetAvatarRequest req = GetAvatarRequest.newBuilder(request)
                .setReqId(id.toString())
                .build();
        return new UserMessage(id, req);
    }

    public static UserMessage genUpdateReportedMessage(UpdateAvatarRequest request) {
        MessageId id = MessageId.next();
        UpdateAvatarRequest req = UpdateAvatarRequest.newBuilder(request)
                .setReqId(id.toString())
                .build();
        return new UserMessage(id, req);
    }

    public static UserMessage buildByMqttMessage(Topic topic, MqttMessage mqttMessage) {
        try {
            switch (topic.getTopicType()) {
                case GET_REPLY: {
                    GetAvatarReply getAvatarReply = GetAvatarReply.parseFrom(mqttMessage.getPayload());
                    return new UserMessage(MessageId.of(getAvatarReply.getReqId()), getAvatarReply);
                }
                case UPDATE_REPORTED_REPLY:
                case UPDATE_DESIRED_REPLY: {
                    UpdateAvatarReply updateAvatarReply = UpdateAvatarReply.parseFrom(mqttMessage.getPayload());
                    return new UserMessage(MessageId.of(updateAvatarReply.getReqId()), updateAvatarReply);
                }
                case DESIRED_DELTA:
                case REPORTED_DELTA: {
                    Delta delta = Delta.parseFrom(mqttMessage.getPayload());
                    return new UserMessage(MessageId.next(), delta);
                }
                default: {
                    throw new RuntimeException("Can not resolve mqtt message, topic:" + topic.toString());
                }
            }
        } catch (InvalidProtocolBufferException e) {
            // TODO specific exception
            throw new RuntimeException(e);
        }
    }

    public MqttMessage toMqttMessage() {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(content.toByteArray());
        mqttMessage.setQos(1);
        return mqttMessage;
    }
}
