// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.method;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;

/**
 * Author zhangxiao18
 * Date 2020/10/13
 */
public class ObserveReportedDeltaMethod extends ObserveDeltaAvatarMethod {
    public ObserveReportedDeltaMethod(EntityId entityId, AvatarId avatarId, IAvatarTransport avatarTransport) {
        super(entityId, Topic.genReportedDeltaTopic(avatarId), avatarTransport);
    }

    @Override
    protected String getMethodName() {
        return "ObserveReportedDelta";
    }
}
