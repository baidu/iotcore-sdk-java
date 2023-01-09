// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.google.protobuf.GeneratedMessageV3;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.processors.PublishProcessor;

/**
 * Author zhangxiao18
 * Date 2020/10/6
 */
public interface IAvatarController {

    PublishProcessor<GeneratedMessageV3> observe(IDeviceSideAvatar deviceSideAvatar);

    Completable close(AvatarId avatarId);

    Completable close();

}
