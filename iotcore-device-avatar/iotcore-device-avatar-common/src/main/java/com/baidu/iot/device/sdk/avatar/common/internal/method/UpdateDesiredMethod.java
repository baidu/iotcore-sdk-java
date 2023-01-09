// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.method;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarRequest;

/**
 * Author zhangxiao18
 * Date 2020/10/13
 */
public class UpdateDesiredMethod extends ReqRespAvatarMethod<UpdateAvatarRequest, UpdateAvatarReply> {

    public UpdateDesiredMethod(EntityId entityId, AvatarId avatarId,
                               IAvatarTransport avatarTransport, InProcessMessageQueue inProcessMessageQueue) {
        super(entityId,
                Topic.genUpdateDesiredTopic(avatarId),
                Topic.genUpdateDesiredReplyTopic(avatarId),
                avatarTransport, inProcessMessageQueue);
    }

    @Override
    protected UserMessage convertRequest(UpdateAvatarRequest request) {
        return UserMessage.genUpdateReportedMessage(request);
    }

    @Override
    protected UpdateAvatarReply convertResponse(UserMessage userMessage) {
        return (UpdateAvatarReply) userMessage.getContent();
    }

    @Override
    protected String getMethodName() {
        return " UpdateDesired";
    }
}
