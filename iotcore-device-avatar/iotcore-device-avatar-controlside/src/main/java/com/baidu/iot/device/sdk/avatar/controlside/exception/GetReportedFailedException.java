// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.controlside.exception;

import com.baidu.iot.thing.avatar.operation.model.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Author zhangxiao18
 * Date 2020/11/17
 */
@RequiredArgsConstructor
@Getter
public class GetReportedFailedException extends RuntimeException {

    private final Status status;
}
