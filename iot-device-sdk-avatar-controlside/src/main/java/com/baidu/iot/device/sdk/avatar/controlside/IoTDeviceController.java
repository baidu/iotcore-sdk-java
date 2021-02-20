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
import com.baidu.iot.device.sdk.avatar.controlside.internal.IControlSideAvatar;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author zhangxiao18
 * Date 2020/11/6
 */
@RequiredArgsConstructor
public class IoTDeviceController {

    private final IoTDeviceControllerFactory factory;

    private final String iotCoreId;

    private final String controllerName;

    private final IControlSideAvatar controlSideAvatar;

    private final Map<String, Single<ControlledDevice>> registeredDevices = new ConcurrentHashMap<>();

    public Single<ControlledDevice> registerDevice(String deviceName) {
        return registeredDevices.computeIfAbsent(
                        deviceName,
                        k -> ControlledDevice.create(
                                this, controlSideAvatar, new AvatarId(iotCoreId, deviceName)));
    }

    public void close() {
        registeredDevices.values().forEach(controlledDeviceSingle -> {
            controlledDeviceSingle.blockingSubscribe(new DisposableSingleObserver<ControlledDevice>() {
                @Override
                public void onSuccess(@NonNull ControlledDevice controlledDevice) {
                    controlledDevice.close();
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    // ignore
                }
            });
        });
        controlSideAvatar.close().blockingAwait();
        factory.closeIoTDeviceController(controllerName);
    }

    void closeControlledDevice(String deviceName) {
        registeredDevices.remove(deviceName);
    }

}
