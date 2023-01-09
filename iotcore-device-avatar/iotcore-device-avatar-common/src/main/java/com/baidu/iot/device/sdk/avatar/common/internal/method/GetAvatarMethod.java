// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.method;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;

/**
 * Author zhangxiao18
 * Date 2020/10/13
 */
public class GetAvatarMethod extends ReqRespAvatarMethod<GetAvatarRequest, GetAvatarReply> {

    public GetAvatarMethod(EntityId entityId, AvatarId avatarId,
                           IAvatarTransport avatarTransport, InProcessMessageQueue inProcessMessageQueue) {
        super(entityId, Topic.genGetTopic(avatarId),
                Topic.genGetReplyTopic(avatarId), avatarTransport, inProcessMessageQueue);
    }

    @Override
    protected UserMessage convertRequest(GetAvatarRequest getAvatarRequest) {
        return UserMessage.genGetMessage(getAvatarRequest);
    }

    @Override
    protected GetAvatarReply convertResponse(UserMessage userMessage) {
        return (GetAvatarReply) userMessage.getContent();
    }

    @Override
    protected String getMethodName() {
        return "GetAvatar";
    }
}
