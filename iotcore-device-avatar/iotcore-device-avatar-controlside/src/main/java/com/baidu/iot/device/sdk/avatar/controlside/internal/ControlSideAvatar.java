// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.controlside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.common.internal.AvatarVersionHolder;
import com.baidu.iot.device.sdk.avatar.common.internal.IAvatarVersionHolder;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.method.GetAvatarMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.method.ObserveReportedDeltaMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.method.UpdateDesiredMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;
import com.baidu.iot.thing.avatar.operation.model.Operation;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarRequest;
import com.google.common.collect.Lists;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Author zhangxiao18
 * Date 2020/11/16
 */
@RequiredArgsConstructor
public class ControlSideAvatar implements IControlSideAvatar {

    private final EntityId entityId;

    private final IAvatarTransport avatarTransport;

    private final InProcessMessageQueue inProcessMessageQueue;

    private final long avatarVersionReloadTimeoutMs;

    @Setter(value = AccessLevel.PACKAGE)
    private ConcurrentHashMap<AvatarId, ControlledAvatar> controlledAvatars = new ConcurrentHashMap<>();

    @Override
    public Completable init(AvatarId avatarId) {
        return getOrInitMethods(avatarId).avatarVersionHolder.triggerReload();
    }

    @Override
    public Single<GetAvatarReply> getAvatar(AvatarId avatarId) {
        return getOrInitMethods(avatarId).getAvatarMethod.call(GetAvatarRequest.newBuilder().build());
    }

    @Override
    public Single<UpdateAvatarReply> updateDesired(AvatarId avatarId, Map<PropertyKey, PropertyValue> properties) {
        return getOrInitMethods(avatarId).avatarVersionHolder.getNextDesiredVersion()
                .flatMap(version -> {
                    List<Operation> operations = new ArrayList<>();
                    properties.forEach((key, value) -> {
                        Operation operation = Operation.newBuilder()
                                .setKey(key.toString())
                                .setValue(value.toString())
                                .build();
                        operations.add(operation);
                    });
                    return getOrInitMethods(avatarId).updateDesiredMethod.call(UpdateAvatarRequest.newBuilder()
                            .addAllOperations(operations)
                            .setVersion(version)
                            .build());
                }).doOnSuccess(updateAvatarReply -> {
                    if (updateAvatarReply.getStatus() == Status.MISMATCHED_VERSION) {
                        getOrInitMethods(avatarId).avatarVersionHolder.triggerReload();
                    }
                });
    }

    @Override
    public Observable<Delta> observeReportedDelta(AvatarId avatarId) {
        return getOrInitMethods(avatarId).observeReportedDeltaMethod.observeDelta();
    }

    @Override
    public Completable close() {
        List<Completable> closeCompletables = controlledAvatars.values().stream()
                .map(ControlledAvatar::close).collect(Collectors.toList());
        closeCompletables.add(avatarTransport.close());
        return Completable.merge(closeCompletables);
    }

    private ControlledAvatar getOrInitMethods(AvatarId avatarId) {
        return controlledAvatars.computeIfAbsent(avatarId,
                k -> new ControlledAvatar(entityId, avatarId,
                        avatarVersionReloadTimeoutMs, avatarTransport, inProcessMessageQueue));
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    static class ControlledAvatar {
        private final IAvatarVersionHolder avatarVersionHolder;
        private final GetAvatarMethod getAvatarMethod;
        private final UpdateDesiredMethod updateDesiredMethod;
        private final ObserveReportedDeltaMethod observeReportedDeltaMethod;

        ControlledAvatar(EntityId entityId, AvatarId avatarId, long avatarVersionReloadTimeoutMs,
                         IAvatarTransport avatarTransport, InProcessMessageQueue inProcessMessageQueue) {
            this.getAvatarMethod = new GetAvatarMethod(entityId,
                    avatarId, avatarTransport, inProcessMessageQueue);
            this.updateDesiredMethod = new UpdateDesiredMethod(entityId,
                    avatarId, avatarTransport, inProcessMessageQueue);
            this.observeReportedDeltaMethod = new ObserveReportedDeltaMethod(entityId,
                    avatarId, avatarTransport);
            this.avatarVersionHolder = new AvatarVersionHolder(entityId,
                    avatarId, getAvatarMethod, avatarVersionReloadTimeoutMs);
        }

        Completable close() {
            return Completable.merge(Lists.newArrayList(avatarVersionHolder.close(), getAvatarMethod.close(),
                    updateDesiredMethod.close(), observeReportedDeltaMethod.close()));
        }
    }
}
