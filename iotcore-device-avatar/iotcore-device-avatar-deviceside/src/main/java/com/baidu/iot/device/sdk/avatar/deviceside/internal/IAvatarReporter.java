// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.thing.avatar.operation.model.Status;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import java.util.Map;

/**
 * Author zhangxiao18
 * Date 2020/10/5
 */
public interface IAvatarReporter {

    Single<Status> updateReported(IDeviceSideAvatar deviceSideAvatar, Map<PropertyKey, PropertyValue> avatarProperties);

    Completable close(AvatarId avatarId);

    Completable close();

}
