// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IAvatarController;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IAvatarReporter;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IDeviceSideAvatar;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.ILocalAvatarHolder;
import com.baidu.iot.thing.avatar.operation.model.Status;

/**
 * Author zhangxiao18
 * Date 2020/10/7
 */
@RequiredArgsConstructor
public class Device {

    private final String iotCoreId;

    private final String deviceName;

    private final IoTDeviceFactory ioTDeviceFactory;

    private final IDeviceSideAvatar deviceSideAvatar;

    private final IAvatarReporter avatarReporter;

    private final ILocalAvatarHolder localAvatarHolder;

    private final IAvatarController avatarController;

    public Single<Status> updateReported(Map<PropertyKey, PropertyValue> properties) {
        return avatarReporter.updateReported(deviceSideAvatar, properties);
    }

    public BehaviorProcessor<PropertyValue> observeDesired(PropertyKey key) {
        return localAvatarHolder.observeDesired(key);
    }

    public void close() {
        AvatarId avatarId = new AvatarId(iotCoreId, deviceName);
        ioTDeviceFactory.closeDevice(deviceName);
        Completable.merge(Lists.newArrayList(
                deviceSideAvatar.close(), avatarReporter.close(avatarId), avatarController.close(avatarId)))
                .blockingAwait();
    }

}
