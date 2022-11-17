// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.common.internal.AvatarVersionHolder;
import com.baidu.iot.device.sdk.avatar.common.internal.IAvatarVersionHolder;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.method.GetAvatarMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.method.ObserveDesiredDeltaMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.method.UpdateReportedMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;
import com.baidu.iot.thing.avatar.operation.model.Operation;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarRequest;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hold all resources of device side avatar.
 *
 * Author zhangxiao18
 * Date 2020/10/14
 */
@Slf4j
public class DeviceSideAvatar implements IDeviceSideAvatar {

    @Getter
    private final AvatarId avatarId;

    private final IAvatarVersionHolder avatarVersionHolder;

    private final IAvatarConsistenceStrategy avatarConsistenceStrategy;

    private final IAvatarTransport avatarTransport;

    private final GetAvatarMethod getAvatarMethod;

    private final UpdateReportedMethod updateReportedMethod;

    private final ObserveDesiredDeltaMethod observeDesiredDeltaMethod;

    public DeviceSideAvatar(AvatarId avatarId, IAvatarConsistenceStrategy consistenceStrategy,
                             IAvatarTransport avatarTransport, InProcessMessageQueue inProcessMessageQueue,
                             long avatarVersionReloadTimeoutMs) {
        this.avatarId = avatarId;
        this.avatarConsistenceStrategy = consistenceStrategy;
        this.avatarTransport = avatarTransport;
        this.getAvatarMethod = new GetAvatarMethod(avatarId.getDeviceSideEntityId(),
                avatarId, avatarTransport, inProcessMessageQueue);
        this.updateReportedMethod = new UpdateReportedMethod(avatarId.getDeviceSideEntityId(),
                avatarId, avatarTransport, inProcessMessageQueue);
        this.observeDesiredDeltaMethod = new ObserveDesiredDeltaMethod(avatarId.getDeviceSideEntityId(),
                avatarId, avatarTransport);
        this.avatarVersionHolder = new AvatarVersionHolder(
                avatarId.getDeviceSideEntityId(), avatarId, getAvatarMethod, avatarVersionReloadTimeoutMs);
    }

    @Override
    public boolean isAvatarConsistent() {
        return this.avatarConsistenceStrategy.isConsistent();
    }

    @Override
    public void setAvatarConsistence(boolean isConsistent) {
        this.avatarConsistenceStrategy.force(isConsistent);
    }

    @Override
    public void recordLatestDesiredVersion(int version) {
        this.avatarConsistenceStrategy.recordLatestDesiredVersion(version);
    }

    @Override
    public Single<GetAvatarReply> getAvatar() {
        return getAvatarMethod.call(GetAvatarRequest.newBuilder().build());
    }

    @Override
    public Single<UpdateAvatarReply> updateReported(Map<PropertyKey, PropertyValue> properties) {
        return avatarVersionHolder.getNextReportedVersion()
                .flatMap(version -> {
                    log.debug("Update reported for {}, version={}", avatarId, version);
                    List<Operation> operations = new ArrayList<>();
                    properties.forEach((key, value) -> {
                        Operation operation = Operation.newBuilder()
                                .setKey(key.toString())
                                .setValue(value.toString())
                                .build();
                        operations.add(operation);
                    });
                    return updateReportedMethod.call(UpdateAvatarRequest.newBuilder()
                            .addAllOperations(operations)
                            .setVersion(version)
                            .build())
                            .doOnSuccess(updateAvatarReply -> {
                                avatarConsistenceStrategy.recordReportResult(true);
                                if (updateAvatarReply.getStatus() == Status.MISMATCHED_VERSION) {
                                    avatarVersionHolder.triggerReload();
                                }
                            })
                            .doOnError(s -> avatarConsistenceStrategy.recordReportResult(false));
                });
    }

    @Override
    public Observable<Delta> observeDesiredDelta() {
        return observeDesiredDeltaMethod.observeDelta();
    }

    @Override
    public Completable close() {
        return avatarVersionHolder.close()
                .andThen(getAvatarMethod.close())
                .andThen(updateReportedMethod.close())
                .andThen(observeDesiredDeltaMethod.close())
                .andThen(avatarTransport.close())
                .doOnComplete(() -> log.debug("DeviceSideAvatar close success, avatarId={}", avatarId));
    }

}
