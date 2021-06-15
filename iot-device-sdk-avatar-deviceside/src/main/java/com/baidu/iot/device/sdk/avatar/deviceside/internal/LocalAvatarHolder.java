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

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.Constants;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.common.internal.AvatarSchedulers;
import com.baidu.iot.mqtt.common.utils.JsonHelper;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.GeneratedMessageV3;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Author zhangxiao18
 * Date 2020/10/7
 */
@RequiredArgsConstructor
@Slf4j
public class LocalAvatarHolder implements ILocalAvatarHolder {

    private final AvatarId avatarId;

    private final IAvatarController avatarController;

    private final IDeviceSideAvatar deviceSideAvatar;

    private final long avatarValueReEmitIntervalInMs;

    private final ConcurrentHashMap<PropertyKey, AvatarValueHolder> avatar = new ConcurrentHashMap<>();

    private AtomicBoolean init = new AtomicBoolean(false);

    private volatile int localVersion = 0;

    @Getter
    @RequiredArgsConstructor
    private static class AvatarValueHolder {
        private final AvatarId avatarId;
        private final PropertyKey key;
        private final long avatarValueReEmitIntervalInMs;
        private final BehaviorProcessor<PropertyValue> processor = BehaviorProcessor.create();
        private final AtomicReference<PropertyValue> latestValue = new AtomicReference<>();
        private AtomicBoolean reEmitting = new AtomicBoolean(false);

        void apply(PropertyValue value) {
            PropertyValue toEmit = value == null ? PropertyValue.EMPTY : value;
            latestValue.set(toEmit);
            if (!processor.offer(toEmit)) {
                scheduleReEmit();
            }
        }

        private void scheduleReEmit() {
            if (processor.hasThrowable() || processor.hasComplete()) {
                return;
            }
            log.warn("Downstream consumes value too slow, schedule reEmit, avatarId={}, key={}", avatarId, key);
            if (reEmitting.compareAndSet(false, true)) {
                Completable.timer(avatarValueReEmitIntervalInMs, TimeUnit.MILLISECONDS, AvatarSchedulers.task(avatarId.getIotCoreId()))
                        .observeOn(AvatarSchedulers.task(avatarId.getIotCoreId()))
                        .subscribe(() -> {
                            log.debug("Start to reEmit value, avatarId={}, key={}", avatarId, key);
                            if (!processor.offer(latestValue.get())) {
                                reEmitting.set(false);
                                scheduleReEmit();
                            } else {
                                reEmitting.compareAndSet(true, false);
                            }
                        });
            }
        }
    }

    private boolean isLocalAvatarInited() {
        return localVersion != 0;
    }

    @Override
    public BehaviorProcessor<PropertyValue> observeDesired(PropertyKey key) {
        if (!init.get() && init.compareAndSet(false, true)) {
            observeAvatarMessage();
        }

        return avatar.computeIfAbsent(key, k -> new AvatarValueHolder(avatarId, key, avatarValueReEmitIntervalInMs)).getProcessor();
    }

    private void observeAvatarMessage() {
        avatarController.observe(deviceSideAvatar)
                .observeOn(AvatarSchedulers.io(avatarId.getIotCoreId()))
                .subscribe(new DisposableSubscriber<GeneratedMessageV3>() {
                    @Override
                    public void onNext(GeneratedMessageV3 messageV3) {
                        if (messageV3 instanceof GetAvatarReply) {
                            handleGetMessage((GetAvatarReply) messageV3);
                        } else {
                            handleDeltaMessage((Delta) messageV3);
                        }
                        request(1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        avatar.forEach((key, avatarValueHolder) -> avatarValueHolder.getProcessor().onError(t));
                        dispose();
                    }

                    @Override
                    public void onComplete() {
                        avatar.forEach((key, avatarValueHolder) -> avatarValueHolder.getProcessor().onComplete());
                        dispose();
                    }
                });
        // trigger reload
        deviceSideAvatar.setAvatarConsistence(false);
    }

    // called on ioThread, so need not to synchronize
    private void handleGetMessage(GetAvatarReply getAvatarReply) {
        try {
            ObjectNode root = (ObjectNode) JsonHelper.toJsonNode(getAvatarReply.getAvatar());
            ObjectNode desired = (ObjectNode) root.get(Constants.AVATAR_DESIRED_FIELD_NAME);
            int version = desired.get(Constants.AVATAR_VERSION_FIELD_NAME).intValue();
            if (version < localVersion) {
                return;
            } else {
                // apply message
                localVersion = version;
                PropertyValue desiredValue = new PropertyValue(desired);
                avatar.keySet().forEach(key -> {
                    PropertyValue newValue = desiredValue.getValue(key);
                    AvatarValueHolder holder = avatar.get(key);
                    if (holder != null) {
                        if (holder.getLatestValue().get() == null || !holder.getLatestValue().get().equals(newValue)) {
                            holder.apply(newValue);
                        }
                    }
                });
                deviceSideAvatar.setAvatarConsistence(true);
                deviceSideAvatar.recordLatestDesiredVersion(localVersion);
            }
        } catch (Exception e) {
            log.warn("Get unknown error when handling avatar reply, reply={}", getAvatarReply, e);
        }
    }

    // called on ioThread, so need not to synchronize
    private void handleDeltaMessage(Delta delta) {
        if (!isLocalAvatarInited()) {
            localVersion = delta.getVersion();
        } else {
            if (delta.getVersion() <= localVersion) {
                return;
            }
            // apply message
            localVersion = delta.getVersion();
            delta.getOperationsList().forEach(operation -> {
                applyValue(new PropertyKey(operation.getKey()), new PropertyValue(operation.getValue()));
            });
            deviceSideAvatar.recordLatestDesiredVersion(localVersion);
        }
    }

    private void applyValue(PropertyKey key, PropertyValue value) {
        if (avatar.containsKey(key)) {
            AvatarValueHolder holder = avatar.get(key);
            holder.apply(value);
        }

        Set<PropertyKey> childrenKeys = key.findChildren(avatar.keySet());
        if (!childrenKeys.isEmpty()) {
            childrenKeys.forEach(childKey ->
                    applyValue(childKey, value.getValue(childKey.removePrefixEntry(key.getEntries().size()))));
        }
    }
}
