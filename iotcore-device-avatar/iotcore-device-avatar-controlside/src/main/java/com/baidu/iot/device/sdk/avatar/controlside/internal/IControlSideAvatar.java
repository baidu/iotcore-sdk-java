// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.controlside.internal;

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
 * Date 2020/11/16
 */
public interface IControlSideAvatar {

    Completable init(AvatarId avatarId);

    Single<GetAvatarReply> getAvatar(AvatarId avatarId);

    Single<UpdateAvatarReply> updateDesired(AvatarId avatarId, Map<PropertyKey, PropertyValue> properties);

    Observable<Delta> observeReportedDelta(AvatarId avatarId);

    Completable close();

}
