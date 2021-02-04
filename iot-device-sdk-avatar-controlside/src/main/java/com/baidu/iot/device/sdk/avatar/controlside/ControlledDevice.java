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

package com.baidu.iot.device.sdk.avatar.controlside;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.Constants;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.common.internal.model.Avatar;
import com.baidu.iot.device.sdk.avatar.common.internal.utils.AvatarHelper;
import com.baidu.iot.device.sdk.avatar.controlside.exception.GetReportedFailedException;
import com.baidu.iot.device.sdk.avatar.controlside.internal.IControlSideAvatar;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author zhangxiao18
 * @Date 2020/11/6
 */
public class ControlledDevice {

    private final IoTDeviceController controller;

    private final IControlSideAvatar controlSideAvatar;

    @Getter
    private final AvatarId avatarId;

    public static Single<ControlledDevice> create(
            IoTDeviceController controller, IControlSideAvatar controlSideAvatar, AvatarId avatarId) {
        ControlledDevice controlledDevice = new ControlledDevice(controller, controlSideAvatar, avatarId);
        return controlSideAvatar.init(avatarId)
                .onErrorResumeNext(throwable -> Completable.complete())
                .andThen(Single.just(controlledDevice));
    }

    private ControlledDevice(IoTDeviceController controller, IControlSideAvatar controlSideAvatar, AvatarId avatarId) {
        this.controller = controller;
        this.controlSideAvatar = controlSideAvatar;
        this.avatarId = avatarId;
    }

    private final Map<PropertyKey, Flowable<VersionedPropertyValue>> observedReportedDeltaMap =
            new ConcurrentHashMap<>();

    public Single<Status> updateDesired(Map<PropertyKey, PropertyValue> properties) {
        return controlSideAvatar.updateDesired(avatarId, properties)
                .map(UpdateAvatarReply::getStatus);
    }

    public Single<VersionedPropertyValue> getReported() {
        return controlSideAvatar.getAvatar(avatarId)
                .map(getAvatarReply -> {
                    if (getAvatarReply.getStatus() != Status.SUCCESS) {
                        throw new GetReportedFailedException(getAvatarReply.getStatus());
                    } else {
                        Avatar avatar = AvatarHelper.buildAvatar(avatarId.getId(), getAvatarReply.getAvatar());
                        ObjectNode reported = (ObjectNode) avatar.getReported();
                        int version = reported.remove(Constants.AVATAR_VERSION_FIELD_NAME).intValue();
                        return new VersionedPropertyValue(reported, version);
                    }
                });
    }

    public Flowable<VersionedPropertyValue> observeReportedDelta(PropertyKey propertyKey) {
        return observedReportedDeltaMap.computeIfAbsent(propertyKey,
                k -> Flowable.fromObservable(
                        controlSideAvatar.observeReportedDelta(avatarId)
                                .flatMap(delta -> {
                                    List<VersionedPropertyValue> values = new ArrayList<>();
                                    delta.getOperationsList().forEach(operation -> {
                                        if (operation.getKey().equals(propertyKey.toString())) {
                                            values.add(new VersionedPropertyValue(
                                                    operation.getValue(), delta.getVersion()));
                                        }
                                    });
                                    return Observable.fromIterable(values);
                                }),
                        BackpressureStrategy.LATEST));
    }

    public void close() {
        controller.closeControlledDevice(avatarId.getId());
    }
}
