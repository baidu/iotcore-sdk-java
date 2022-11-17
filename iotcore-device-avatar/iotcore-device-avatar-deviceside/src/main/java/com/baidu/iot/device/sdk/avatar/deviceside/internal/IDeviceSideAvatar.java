// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

import java.util.Map;

/**
 * Author zhangxiao18
 * Date 2020/10/22
 */
public interface IDeviceSideAvatar {

    boolean isAvatarConsistent();

    void setAvatarConsistence(boolean isConsistent);

    void recordLatestDesiredVersion(int version);

    AvatarId getAvatarId();

    Single<GetAvatarReply> getAvatar();

    Single<UpdateAvatarReply> updateReported(Map<PropertyKey, PropertyValue> properties);

    Observable<Delta> observeDesiredDelta();

    Completable close();
}
