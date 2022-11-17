// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.exception;

import com.baidu.iot.device.sdk.avatar.common.EntityId;

/**
 * Author zhangxiao18
 * Date 2020/10/12
 */
@Deprecated
public class EntityOpenFailedException extends RuntimeException {

    private final EntityId entityId;

    public EntityOpenFailedException(EntityId entityId, Throwable throwable) {
        super(throwable);
        this.entityId = entityId;
    }

}
