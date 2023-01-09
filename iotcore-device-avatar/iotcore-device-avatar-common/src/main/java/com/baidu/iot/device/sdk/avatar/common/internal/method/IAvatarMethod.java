// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.method;

import io.reactivex.rxjava3.core.Completable;

/**
 * Avatar Method is an abstraction for transport.
 *
 * Use method instead of transport to request to server.
 *
 * Author zhangxiao18
 * Date 2020/10/12
 */
public interface IAvatarMethod {

    Completable close();

}
