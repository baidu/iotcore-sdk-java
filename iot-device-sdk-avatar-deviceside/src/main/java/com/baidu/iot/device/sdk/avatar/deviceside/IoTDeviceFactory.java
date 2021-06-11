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

import io.reactivex.rxjava3.core.Single;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.internal.AvatarSchedulers;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.MqttTransportConfig;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.mqtt.MqttAvatarTransport;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.AvatarConsistenceStrategy;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.AvatarController;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.AvatarReporter;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.DeviceSideAvatar;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IAvatarConsistenceStrategy;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IAvatarController;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IAvatarReporter;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.IDeviceSideAvatar;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.ILocalAvatarHolder;
import com.baidu.iot.device.sdk.avatar.deviceside.internal.LocalAvatarHolder;
import com.baidu.iot.mqtt.common.MqttConfig;

/**
 * Author zhangxiao18
 * Date 2020/10/7
 */
@Slf4j
public class IoTDeviceFactory {

    @Builder
    @Getter
    public static class Config {
        private final String iotCoreId;

        @Builder.Default
        private final long inProcessMessageExpireTimeMs = 10000L;
        @Builder.Default
        private final int maxInProcessMessageCount = 1000;
        @Builder.Default
        private final long reportWaitingTimeAfterFullInProcessMessageQueueMs = 10000L;
        @Builder.Default
        private final long avatarActiveTimeWatershedInMs = 30 * 60 * 1000L;
        @Builder.Default
        private final long avatarMinReloadIntervalInMs = 3000L;
        @Builder.Default
        private final long controlQueueCrowdedMaxDurationInMs = 30000L;
        @Builder.Default
        private final long avatarValueReEmitIntervalInMs = 3000L;
        @Builder.Default
        private final long avatarVersionReloadTimeoutMs = 10000L;
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
        private final int avatarMaxReportTimePerSecond = 10;
    }

    private final Config config;

    private final IAvatarReporter avatarReporter;

    private final IAvatarController avatarController;

    private final InProcessMessageQueue inProcessMessageQueue;

    public IoTDeviceFactory(Config config) {
        this.config = config;
        this.avatarReporter = new AvatarReporter(config.iotCoreId, config.avatarMaxReportTimePerSecond,
                config.reportWaitingTimeAfterFullInProcessMessageQueueMs);
        this.avatarController = new AvatarController(config.iotCoreId, config.avatarMinReloadIntervalInMs, config.controlQueueCrowdedMaxDurationInMs);
        this.inProcessMessageQueue = new InProcessMessageQueue(config.iotCoreId, config.maxInProcessMessageCount, config.inProcessMessageExpireTimeMs);
    }

    private final ConcurrentHashMap<String, Single<Device>> registeredDevices = new ConcurrentHashMap<>();

    public Single<Device> getDevice(String deviceName, MqttConfig mqttConfig) {
        return registeredDevices.computeIfAbsent(deviceName, k -> {
            IAvatarConsistenceStrategy consistenceStrategy = new AvatarConsistenceStrategy(config.avatarActiveTimeWatershedInMs);
            AvatarId avatarId = new AvatarId(config.iotCoreId, deviceName);
            Single<IAvatarTransport> avatarTransport = MqttAvatarTransport.create(
                    new MqttTransportConfig(
                            avatarId.getDeviceSideEntityId(),
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
                    config.maxInProcessMessageCount);

            return avatarTransport.map(transport -> {
                IDeviceSideAvatar deviceSideAvatar = new DeviceSideAvatar(
                        avatarId, consistenceStrategy, transport, inProcessMessageQueue, config.avatarVersionReloadTimeoutMs);
                ILocalAvatarHolder localAvatarHolder = new LocalAvatarHolder(
                        avatarId, avatarController, deviceSideAvatar, config.avatarValueReEmitIntervalInMs);
                return new Device(config.iotCoreId, deviceName, this,
                        deviceSideAvatar, avatarReporter, localAvatarHolder, avatarController);
            });
        });
    }

    public void close() {
        avatarReporter.close()
                .andThen(avatarController.close())
                .andThen(inProcessMessageQueue.close())
                .blockingAwait();
    }

    public void closeSchedulers() {
        AvatarSchedulers.close(config.iotCoreId);
    }

    void closeDevice(String deviceName) {
        registeredDevices.remove(deviceName);
    }

}
