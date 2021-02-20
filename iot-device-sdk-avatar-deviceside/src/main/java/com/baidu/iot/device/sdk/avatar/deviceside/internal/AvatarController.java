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
import com.baidu.iot.device.sdk.avatar.common.internal.AvatarSchedulers;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.google.protobuf.GeneratedMessageV3;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Author zhangxiao18
 * Date 2020/10/6
 */
@Slf4j
public class AvatarController implements IAvatarController {

    private final String iotCoreId;

    private final Disposable reloadTaskDisposable;

    private final long avatarReloadInterval;

    private final long crowdedMaxDuration;

    private final ConcurrentHashMap<AvatarId, ControlQueue> controlQueues = new ConcurrentHashMap<>();

    public AvatarController(String iotCoreId, long avatarReloadIntervalInMs, long crowdedMaxDurationInMs) {
        this.iotCoreId = iotCoreId;
        this.avatarReloadInterval = avatarReloadIntervalInMs;
        this.crowdedMaxDuration = crowdedMaxDurationInMs;

        this.reloadTaskDisposable = scheduleCheckReload();
    }

    private Disposable scheduleCheckReload() {
        return Observable.interval(avatarReloadInterval, TimeUnit.MILLISECONDS, AvatarSchedulers.task(iotCoreId))
                .observeOn(AvatarSchedulers.task(iotCoreId))
                .subscribe(ts -> {
                    controlQueues.forEach((avatarId, controlQueue) -> {
                        if (controlQueue.canReload() && !controlQueue.deviceSideAvatar.isAvatarConsistent()) {
                            log.debug("Reload avatar, avatarId={}", avatarId);
                            controlQueue.deviceSideAvatar.getAvatar()
                                    .observeOn(AvatarSchedulers.io(iotCoreId))
                                    .subscribe((getAvatarReply, throwable) -> {
                                        if (throwable != null) {
                                            log.error("Reload avatar error, avatarId={}", avatarId, throwable);
                                        } else {
                                            controlQueue.addMessage(getAvatarReply);
                                        }
                                    });
                        }
                    });
                });
    }

    @Getter
    private static class ControlQueue {
        ControlQueue(IDeviceSideAvatar deviceSideAvatar, long avatarReloadInterval, long crowdedMaxDuration) {
            this.waitingControlMessages = PublishProcessor.create();
            this.deviceSideAvatar = deviceSideAvatar;
            this.avatarReloadInterval = avatarReloadInterval;
            this.crowdedMaxDuration = crowdedMaxDuration;
        }

        private volatile long lastReloadTs = 0;

        private final long avatarReloadInterval;

        private final long crowdedMaxDuration;

        private final PublishProcessor<GeneratedMessageV3> waitingControlMessages;

        private final IDeviceSideAvatar deviceSideAvatar;

        private final AtomicLong crowdedStartTime = new AtomicLong(0L);

        boolean canReload() {
            if (System.currentTimeMillis() - lastReloadTs > avatarReloadInterval
                    && !waitingControlMessages.hasComplete()
                    && !waitingControlMessages.hasThrowable()) {
                lastReloadTs = System.currentTimeMillis();
                return true;
            }
            return false;
        }

        private void addMessageNormal(GeneratedMessageV3 messageV3) {
            boolean result = waitingControlMessages.offer(messageV3);
            if (!result) {
                log.debug("Control queue is over flow, enter crowded, avatarId={}", deviceSideAvatar.getAvatarId());
                crowdedStartTime.set(System.currentTimeMillis());
                deviceSideAvatar.setAvatarConsistence(false);
            }
        }

        private void addMessageCrowded(GeneratedMessageV3 messageV3) {
            if (messageV3 instanceof Delta) {
                log.warn("Drop delta message since control message queue is full, avatarId={}", deviceSideAvatar.getAvatarId());
            } else {
                // receive get message
                boolean result = waitingControlMessages.offer(messageV3);
                if (!result) {
                    crowdedStartTime.set(System.currentTimeMillis());
                    deviceSideAvatar.setAvatarConsistence(false);
                }
            }
        }

        void addMessage(GeneratedMessageV3 messageV3) {
            long lastCrowdedStartTime = crowdedStartTime.get();
            if (System.currentTimeMillis() - lastCrowdedStartTime > crowdedMaxDuration) {
                crowdedStartTime.compareAndSet(lastCrowdedStartTime, 0L);
            }
            if (crowdedStartTime.get() != 0L) {
                addMessageCrowded(messageV3);
            } else {
                addMessageNormal(messageV3);
            }
        }

        private void error(Throwable t) {
            waitingControlMessages.onError(t);
        }

        private void complete() {
            waitingControlMessages.onComplete();
        }
    }

    @Override
    public PublishProcessor<GeneratedMessageV3> observe(IDeviceSideAvatar deviceSideAvatar) {
        return controlQueues.computeIfAbsent(deviceSideAvatar.getAvatarId(), k -> {
            ControlQueue controlQueue = new ControlQueue(deviceSideAvatar, avatarReloadInterval, crowdedMaxDuration);
            deviceSideAvatar.observeDesiredDelta()
                    .observeOn(AvatarSchedulers.io(iotCoreId))
                    .subscribe(new DisposableObserver<Delta>() {
                        @Override
                        public void onNext(@NonNull Delta delta) {
                            log.debug("Receive delta message, avatarId={}", deviceSideAvatar.getAvatarId());
                            controlQueue.addMessage(delta);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            controlQueue.error(e);
                            controlQueues.remove(deviceSideAvatar.getAvatarId());
                            dispose();
                        }

                        @Override
                        public void onComplete() {
                            dispose();
                        }
                    });
            return controlQueue;
        }).getWaitingControlMessages();
    }

    @Override
    public Completable close(AvatarId avatarId) {
        ControlQueue controlQueue = controlQueues.remove(avatarId);
        if (controlQueue != null) {
            controlQueue.complete();
        }
        return Completable.complete();
    }

    @Override
    public Completable close() {
        reloadTaskDisposable.dispose();
        return Completable.merge(controlQueues.keySet().stream().map(this::close).collect(Collectors.toList()))
                .doOnComplete(() -> log.debug("Avatar controller close success, iotCoreId={}", iotCoreId));
    }

    boolean isCrowded(AvatarId avatarId) {
        return controlQueues.get(avatarId).getCrowdedStartTime().get() != 0L;
    }
}
