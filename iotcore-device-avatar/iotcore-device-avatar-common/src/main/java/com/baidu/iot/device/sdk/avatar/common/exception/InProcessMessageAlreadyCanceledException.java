// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.exception;

import com.baidu.iot.device.sdk.avatar.common.EntityId;
import lombok.RequiredArgsConstructor;

/**
 * Author zhangxiao18
 * Date 2020/10/15
 */
@RequiredArgsConstructor
public class InProcessMessageAlreadyCanceledException extends RuntimeException {

    private final EntityId entityId;

}
