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
public class UpdateReportedMethod extends ReqRespAvatarMethod<UpdateAvatarRequest, UpdateAvatarReply> {

    public UpdateReportedMethod(EntityId entityId, AvatarId avatarId,
                                IAvatarTransport avatarTransport, InProcessMessageQueue inProcessMessageQueue) {
        super(entityId,
                Topic.genUpdateReportedTopic(avatarId),
                Topic.genUpdateReportedReplyTopic(avatarId),
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
        return "UpdateReported";
    }
}
