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

package com.baidu.iot.device.sdk.avatar.common.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.Constants;
import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.exception.EntityAlreadyClosedException;
import com.baidu.iot.device.sdk.avatar.common.exception.ReloadAvatarVersionFailedException;
import com.baidu.iot.device.sdk.avatar.common.internal.method.GetAvatarMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.model.Avatar;
import com.baidu.iot.device.sdk.avatar.common.internal.utils.AvatarHelper;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Author zhangxiao18
 * Date 2020/10/13
 */
@Slf4j
public class AvatarVersionHolder implements IAvatarVersionHolder {

    private final EntityId entityId;

    private final AvatarId avatarId;

    private final GetAvatarMethod getAvatarMethod;

    private final long reloadTimeoutInMs;

    private final AtomicInteger reportedVersion = new AtomicInteger(0);

    private final AtomicInteger desiredVersion = new AtomicInteger(0);

    private final AtomicReference<CompletableFuture<Void>> reloading = new AtomicReference<>();

    private volatile boolean shutdown = false;

    public AvatarVersionHolder(
            EntityId entityId, AvatarId avatarId, GetAvatarMethod getAvatarMethod, long reloadTimeoutInMs) {
        this.entityId = entityId;
        this.avatarId = avatarId;
        this.getAvatarMethod = getAvatarMethod;
        this.reloadTimeoutInMs = reloadTimeoutInMs;
        triggerReload();
    }

    @Override
    public Single<Integer> getNextReportedVersion() {
        if (shutdown) {
            return Single.error(new EntityAlreadyClosedException(entityId));
        }

        return waitingForReload().andThen(Single.fromCallable(reportedVersion::incrementAndGet));
    }

    @Override
    public Single<Integer> getNextDesiredVersion() {
        if (shutdown) {
            return Single.error(new EntityAlreadyClosedException(entityId));
        }

        return waitingForReload().andThen(Single.fromCallable(desiredVersion::incrementAndGet));
    }

    @Override
    public Completable triggerReload() {
        if (shutdown) {
            return Completable.error(new EntityAlreadyClosedException(entityId));
        }

        CompletableFuture<Void> old = reloading.get();
        if (old == null || old.isDone()) {
            reload();
        }
        return waitingForReload();
    }

    private void reload() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        reloading.set(future);
        log.debug("Start to reload version, entityId={}, avatarId={}", entityId, avatarId);
        getAvatarMethod.call(GetAvatarRequest.newBuilder().build())
                .observeOn(AvatarSchedulers.task(entityId.getIotCoreId()))
                .subscribe(new DisposableSingleObserver<GetAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull GetAvatarReply getAvatarReply) {
                        if (getAvatarReply.getStatus() == Status.SUCCESS) {
                            try {
                                Avatar avatar = AvatarHelper.buildAvatar(avatarId.getId(), getAvatarReply.getAvatar());
                                int curReportedVersion = reportedVersion.get();
                                int newReportedVersion =  avatar.getReported().get(Constants.AVATAR_VERSION_FIELD_NAME).intValue();
                                reportedVersion.compareAndSet(curReportedVersion, newReportedVersion);

                                int curDesiredVersion = desiredVersion.get();
                                int newDesiredVersion =  avatar.getDesired().get(Constants.AVATAR_VERSION_FIELD_NAME).intValue();
                                desiredVersion.compareAndSet(curDesiredVersion, newDesiredVersion);
                                log.debug("Reload version success, current reported version={}, current desired version={}",
                                        reportedVersion.get(), desiredVersion.get());
                                future.complete(null);
                            } catch (JsonProcessingException e) {
                                log.warn("Can not resolve avatar, entityId={}, avatarId={}", entityId, avatarId, e);
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (!shutdown) {
                            log.warn("Reload version failed, entityId={}, avatarId={}", entityId, avatarId, e);
                            // reload before complete last future, so new request will get the newest version
                            reload();
                        }
                        future.completeExceptionally(e);
                        dispose();
                    }
                });
    }

    private Completable waitingForReload() {
        CompletableFuture<Void> future = reloading.get();
        if (future == null) {
            return Completable.complete();
        }
        return Completable.fromCompletionStage(future)
                .timeout(reloadTimeoutInMs, TimeUnit.MILLISECONDS, AvatarSchedulers.task(entityId.getIotCoreId()))
                .onErrorResumeNext(throwable -> Completable.error(
                        new ReloadAvatarVersionFailedException(entityId, avatarId, throwable)));
    }

    @Override
    public Completable close() {
        shutdown = true;
        Completable reloadingCompletable = waitingForReload();
        if (reloadingCompletable == null) {
            return Completable.complete();
        } else {
            return reloadingCompletable.onErrorResumeNext(throwable -> Completable.complete());
        }
    }
}
