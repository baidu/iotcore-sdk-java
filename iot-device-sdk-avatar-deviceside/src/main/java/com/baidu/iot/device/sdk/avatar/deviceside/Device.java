/*
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.iot.device.sdk.avatar.deviceside;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IAvatarController;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IAvatarReporter;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IDeviceSideAvatar;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.ILocalAvatarHolder;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.google.common.collect.Lists;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import lombok.RequiredArgsConstructor;

import java.util.Map;

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
