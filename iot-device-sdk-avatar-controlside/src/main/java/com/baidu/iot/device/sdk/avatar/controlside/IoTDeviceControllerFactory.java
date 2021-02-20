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

import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.MqttConfig;
import com.baidu.iot.device.sdk.avatar.common.internal.AvatarSchedulers;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.MqttTransportConfig;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.mqtt.MqttAvatarTransport;
import com.baidu.iot.device.sdk.avatar.controlside.internal.ControlSideAvatar;
import com.baidu.iot.device.sdk.avatar.controlside.internal.IControlSideAvatar;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author zhangxiao18
 * Date 2020/11/6
 */
public class IoTDeviceControllerFactory {

    @Builder
    @Getter
    public static class Config {
        private final String iotCoreId;
        @Builder.Default
        private final long inProcessMessageExpireTimeMs = 10000L;
        @Builder.Default
        private final int maxInProcessMessageCount = 1000;
        @Builder.Default
        private final int actionMaxRetryTimes = 1;
        @Builder.Default
        private final long subscribeTimeoutMs = 10000L;
        @Builder.Default
        private final long publishTimeoutMs = 10000L;
        @Builder.Default
        private final int connectTimeoutSecond = 10;
        @Builder.Default
        private final long disconnectTimeoutMs = 10000L;
        @Builder.Default
        private final long avatarVersionReloadTimeoutMs = 10000L;
    }

    private final InProcessMessageQueue inProcessMessageQueue;

    private final Config config;

    private final Map<String, Single<IoTDeviceController>> createdControllers = new ConcurrentHashMap<>();

    public IoTDeviceControllerFactory(Config config) {
        this.config = config;
        this.inProcessMessageQueue = new InProcessMessageQueue(
                config.iotCoreId, config.maxInProcessMessageCount, config.inProcessMessageExpireTimeMs);
    }

    public void close() {
        createdControllers.values().forEach(ioTDeviceControllerSingle -> {
            ioTDeviceControllerSingle.blockingSubscribe(new DisposableSingleObserver<IoTDeviceController>() {
                @Override
                public void onSuccess(@NonNull IoTDeviceController ioTDeviceController) {
                    ioTDeviceController.close();
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    // ignore
                }
            });
        });
        inProcessMessageQueue.close().blockingAwait();
    }

    public void closeSchedulers() {
        AvatarSchedulers.close(config.iotCoreId);
    }

    public Single<IoTDeviceController> createIoTDeviceController(String controllerName, MqttConfig mqttConfig) {
        EntityId entityId = new EntityId(config.iotCoreId, controllerName);
        return createdControllers.computeIfAbsent(controllerName, k ->
                MqttAvatarTransport.create(
                        new MqttTransportConfig(
                                entityId,
                                mqttConfig.getUri(),
                                mqttConfig.getSslSocketFactory(),
                                mqttConfig.getUsername(),
                                mqttConfig.getPassword(),
                                config.subscribeTimeoutMs,
                                config.publishTimeoutMs,
                                config.connectTimeoutSecond,
                                config.disconnectTimeoutMs
                        ),
                        config.actionMaxRetryTimes,
                        config.maxInProcessMessageCount)
                        .map(avatarTransport -> {
                            IControlSideAvatar controlSideAvatar = new ControlSideAvatar(
                                    entityId, avatarTransport,
                                    inProcessMessageQueue, config.avatarVersionReloadTimeoutMs);
                    return new IoTDeviceController(this, config.iotCoreId, controllerName, controlSideAvatar);
                }));
    }

    void closeIoTDeviceController(String controllerName) {
        createdControllers.remove(controllerName);
    }

}
