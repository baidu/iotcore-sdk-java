// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

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
