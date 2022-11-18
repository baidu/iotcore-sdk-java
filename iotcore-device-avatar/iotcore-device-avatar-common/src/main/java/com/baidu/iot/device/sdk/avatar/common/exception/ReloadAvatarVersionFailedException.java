// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.exception;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.EntityId;
import lombok.ToString;

/**
 * Author zhangxiao18
 * Date 2020/10/13
 */
@ToString
public class ReloadAvatarVersionFailedException extends RuntimeException {

    private final EntityId entityId;

    private final AvatarId avatarId;

    public ReloadAvatarVersionFailedException(EntityId entityId, AvatarId avatarId, Throwable cause) {
        super(cause);
        this.entityId = entityId;
        this.avatarId = avatarId;
    }

    @Override
    public String getMessage() {
        return String.format("EntityId=%s, avatarId=%s", entityId, avatarId);
    }
}
