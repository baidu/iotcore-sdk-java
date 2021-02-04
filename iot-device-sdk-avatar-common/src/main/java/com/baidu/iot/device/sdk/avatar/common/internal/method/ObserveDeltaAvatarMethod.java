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

package com.baidu.iot.device.sdk.avatar.common.internal.method;

import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author zhangxiao18
 * @Date 2020/10/13
 */
@Slf4j
public abstract class ObserveDeltaAvatarMethod implements IAvatarMethod {

    private final Observable<Delta> deltaObservable;

    protected abstract String getMethodName();

    public ObserveDeltaAvatarMethod(EntityId entityId, Topic deltaTopic, IAvatarTransport avatarTransport) {
        this.deltaObservable = avatarTransport
                .sub(deltaTopic)
                .filter(message -> message instanceof UserMessage)
                .map(message -> (Delta) message.getContent())
                .doOnError(throwable -> {
                    log.error("Method {} get error from observing topic {}, entityId={}",
                            getMethodName(), deltaTopic, entityId, throwable);
                });
    }

    @Override
    public Completable close() {
        return Completable.complete();
    }

    public Observable<Delta> observeDelta() {
        return deltaObservable;
    }
}
