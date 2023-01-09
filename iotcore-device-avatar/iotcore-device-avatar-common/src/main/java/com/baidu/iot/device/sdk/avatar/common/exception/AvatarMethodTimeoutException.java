// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.exception;

import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.internal.MessageId;
import lombok.RequiredArgsConstructor;

/**
 * Author zhangxiao18
 * Date 2020/10/13
 */
@RequiredArgsConstructor
public class AvatarMethodTimeoutException extends RuntimeException {

    private final EntityId entityId;

    private final MessageId messageId;

    private final String methodName;

}
