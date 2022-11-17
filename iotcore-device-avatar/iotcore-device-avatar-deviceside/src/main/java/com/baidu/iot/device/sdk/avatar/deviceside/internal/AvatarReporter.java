// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.common.exception.InProcessMessageAlreadyCanceledException;
import com.baidu.iot.device.sdk.avatar.common.exception.TooManyInProcessMessageException;
import com.baidu.iot.device.sdk.avatar.common.internal.AvatarSchedulers;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.google.common.util.concurrent.RateLimiter;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 *
 * Author zhangxiao18
 * Date 2020/9/24
 */
@Slf4j
@RequiredArgsConstructor
public class AvatarReporter implements IAvatarReporter {

    private final String iotCoreId;

    private final int avatarMaxReportTimePerSecond;

    private final long waitingTimeAfterFullInProcessMessageQueueMs;

    private final WaitingReportQueue waitingReportQueue = new WaitingReportQueue();

    private final AtomicBoolean reportWaitingRunning = new AtomicBoolean(false);

    private final Map<AvatarId, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    @Override
    public Single<Status> updateReported(IDeviceSideAvatar deviceSideAvatar,
                                         Map<PropertyKey, PropertyValue> avatarProperties) {
        if (!rateLimiters.computeIfAbsent(deviceSideAvatar.getAvatarId(),
                k -> RateLimiter.create(avatarMaxReportTimePerSecond)).tryAcquire()) {
            log.debug("Avatar report too frequently, waiting for next slot, avatarId={}",
                    deviceSideAvatar.getAvatarId());
            return waitingReport(deviceSideAvatar, avatarProperties);
        }
        if (reportWaitingRunning.get() || !waitingReportQueue.isEmpty()) {
            log.debug("There are too many inProcess messages, waiting for next slot, avatarId={}",
                    deviceSideAvatar.getAvatarId());
            return waitingReport(deviceSideAvatar, avatarProperties);
        }

        CompletableFuture<UpdateAvatarReply> result = new CompletableFuture<>();
        log.debug("Start to report avatar {}", deviceSideAvatar.getAvatarId());
        deviceSideAvatar.updateReported(avatarProperties)
                .observeOn(AvatarSchedulers.task(deviceSideAvatar.getAvatarId().getIotCoreId()))
                .subscribe(new DisposableSingleObserver<UpdateAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull UpdateAvatarReply updateAvatarReply) {
                        result.complete(updateAvatarReply);
                        dispose();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (e instanceof TooManyInProcessMessageException) {
                            log.debug("There are too many inProcess messages, waiting for next slot, avatarId={}",
                                    deviceSideAvatar.getAvatarId());
                            waitingReportQueue.put(deviceSideAvatar, avatarProperties, result);
                            scheduleReportWaitingAvatar();
                        } else {
                            result.completeExceptionally(e);
                        }
                        dispose();
                    }
                });
        return Single.fromCompletionStage(result).map(UpdateAvatarReply::getStatus);
    }

    private Single<Status> waitingReport(
            IDeviceSideAvatar deviceSideAvatar, Map<PropertyKey, PropertyValue> avatarProperties) {
        CompletableFuture<UpdateAvatarReply> result = new CompletableFuture<>();
        waitingReportQueue.put(deviceSideAvatar, avatarProperties, result);
        scheduleReportWaitingAvatar();
        return Single.fromCompletionStage(result).map(UpdateAvatarReply::getStatus);
    }

    @Override
    public Completable close(AvatarId avatarId) {
        WaitingReport waitingReport = waitingReportQueue.remove(avatarId);
        if (waitingReport != null) {
            waitingReport.cancel();
            waitingReport.getTracks().forEach(future -> future.completeExceptionally(
                    new InProcessMessageAlreadyCanceledException(avatarId.getDeviceSideEntityId())));
        }
        return Completable.complete();
    }

    @Override
    public Completable close() {
       return Completable.merge(waitingReportQueue.keySet()
                .stream()
                .map(this::close)
                .collect(Collectors.toList()))
               .doOnComplete(() -> log.debug("Avatar reporter close success, iotCoreId={}", iotCoreId));
    }

    private void scheduleReportWaitingAvatar() {
        if (!reportWaitingRunning.get() && reportWaitingRunning.compareAndSet(false, true)) {
            WaitingReport waitingReport = waitingReportQueue.pollOldestWaitingReport();
            if (waitingReport != null) {
                reportAfterDelay(waitingReport, 0);
            } else {
                reportWaitingRunning.compareAndSet(true, false);
            }
        }
    }

    private void reportAfterDelay(WaitingReport waitingReport, long delayInMs) {
        if (waitingReport.isCanceled()) {
            reportWaitingRunning.set(false);
            return;
        }

        Completable.timer(delayInMs, TimeUnit.MILLISECONDS, AvatarSchedulers.task(iotCoreId))
                .doOnComplete(() -> log.debug("Start to report waiting avatar {}", waitingReport.getAvatarId()))
                .andThen(waitingReport.getDeviceSideAvatar().updateReported(waitingReport.getProperties()))
                .observeOn(AvatarSchedulers.task(iotCoreId))
                .subscribe(new DisposableSingleObserver<UpdateAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull UpdateAvatarReply updateAvatarReply) {
                        waitingReport.getTracks().forEach(track -> track.complete(updateAvatarReply));
                        reportWaitingRunning.set(false);
                        if (!waitingReportQueue.isEmpty()) {
                            scheduleReportWaitingAvatar();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (e instanceof TooManyInProcessMessageException) {
                            // waiting time for slot
                            log.debug("There are too many inProcess messages, waiting {}ms for next slot, avatarId={}",
                                    waitingTimeAfterFullInProcessMessageQueueMs, waitingReport.getAvatarId());
                            reportAfterDelay(waitingReport, waitingTimeAfterFullInProcessMessageQueueMs);
                        } else {
                            waitingReport.getTracks().forEach(track -> track.completeExceptionally(e));
                            reportWaitingRunning.set(false);
                            if (!waitingReportQueue.isEmpty()) {
                                scheduleReportWaitingAvatar();
                            }
                        }
                    }
                });
    }

    @Getter
    @RequiredArgsConstructor
    static class WaitingReport {

        private volatile boolean canceled = false;

        private final AvatarId avatarId;

        private final IDeviceSideAvatar deviceSideAvatar;

        private final long waitStartTime = System.currentTimeMillis();

        private final ConcurrentHashMap<PropertyKey, PropertyValue> properties = new ConcurrentHashMap<>();
        // limit?
        private final LinkedBlockingQueue<CompletableFuture<UpdateAvatarReply>> tracks = new LinkedBlockingQueue<>();

        public void cancel() {
            canceled = true;
        }

        public boolean isCanceled() {
            return canceled;
        }
    }

    static class WaitingReportQueue {

        // max size is the num of avatar.
        private ConcurrentHashMap<AvatarId, WaitingReport> waitingQueue = new ConcurrentHashMap<>();

        boolean isEmpty() {
            return waitingQueue.isEmpty();
        }

        WaitingReport remove(AvatarId avatarId) {
            return waitingQueue.remove(avatarId);
        }

        ConcurrentHashMap.KeySetView<AvatarId, WaitingReport> keySet() {
            return waitingQueue.keySet();
        }

        void put(IDeviceSideAvatar deviceSideAvatar, Map<PropertyKey, PropertyValue> properties,
                 CompletableFuture<UpdateAvatarReply> track) {
            WaitingReport waitingReport = waitingQueue.computeIfAbsent(
                    deviceSideAvatar.getAvatarId(),
                    k -> new WaitingReport(deviceSideAvatar.getAvatarId(), deviceSideAvatar));

            // overwrite old value.
            // TODO here may lost some report when other thread call poll.
            properties.forEach((key, value) -> {
                PropertyValue oldValue = waitingReport.getProperties().put(key, value);
                if (log.isDebugEnabled() && oldValue != null) {
                    log.debug("The old value {} of key {} will be dropped "
                            + "since the new value {} will be send, avatarId={}",
                            oldValue, key, value, deviceSideAvatar.getAvatarId());
                }
            });
            waitingReport.getTracks().offer(track);
        }

        WaitingReport pollOldestWaitingReport() {
            if (waitingQueue.isEmpty()) {
                return null;
            }
            Iterator<Map.Entry<AvatarId, WaitingReport>> iterator = waitingQueue.entrySet().iterator();
            long oldestTs = System.currentTimeMillis();
            AvatarId oldestAvatar = null;
            while (iterator.hasNext()) {
                Map.Entry<AvatarId, WaitingReport> entry = iterator.next();
                if (entry.getValue().getWaitStartTime() <= oldestTs) {
                    oldestTs = entry.getValue().getWaitStartTime();
                    oldestAvatar = entry.getKey();
                }
            }
            if (oldestAvatar != null) {
                return waitingQueue.remove(oldestAvatar);
            }
            return null;
        }
    }
}
