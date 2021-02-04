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

package com.baidu.iot.device.sdk.avatar.common.internal.transport.mqtt;

import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.internal.AvatarSchedulers;
import com.baidu.iot.device.sdk.avatar.common.internal.CommandMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.Message;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.MqttTransportConfig;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author zhangxiao18
 * @Date 2020/11/11
 */
@Slf4j
public class MqttAvatarTransport implements IAvatarTransport {

    private final EntityId entityId;
    private final ReactiveStreamsMqttClientWrapper client;
    private final int publishRetryTime;
    private final AtomicReference<Completable> closeTask = new AtomicReference<>();

    private final ConcurrentHashMap<Topic, SubscribedTopic> subscribedTopics = new ConcurrentHashMap<>();

    @RequiredArgsConstructor
    private static class SubscribedTopic {
        // We need to waiting for a moment after subscribed error.
        // the first waiting time will be 5s. Then double it when retry failed. Max waiting time will be 1min.
        private final AtomicInteger subscribeFailRetryWaitingTimeInSec = new AtomicInteger(0);
        private final AtomicReference<Disposable> originObservableDisposable = new AtomicReference<>();
        private final BehaviorSubject<Message> messages;
    }

    /**
     * Create an avatar transport using specific config.
     *
     * @param config
     * @param publishRetryTime
     * @return
     */
    public static Single<IAvatarTransport> create(MqttTransportConfig config,
                                                  int publishRetryTime,
                                                  int maxInflightMessageNum) {
        try {
            return create(config.getEntityId(), publishRetryTime,
                    new ReactiveStreamsMqttClientWrapper(config, maxInflightMessageNum,
                            new MqttAsyncClient(config.getUri().toString(), config.getEntityId().genMqttClientId())));
        } catch (MqttException e) {
            return Single.error(new RuntimeException("Create mqtt client failed, config=" + config.toString(), e));
        }
    }

    /**
     * Create an avatar transport using specific config.
     *
     * @param entityId
     * @param publishRetryTime
     * @param mqttAsyncClient
     * @return
     */
    public static Single<IAvatarTransport> create(EntityId entityId,
                                                  int publishRetryTime,
                                                  ReactiveStreamsMqttClientWrapper mqttAsyncClient) {
        return Single.create(emitter -> {
            MqttAvatarTransport transport = new MqttAvatarTransport(entityId, publishRetryTime, mqttAsyncClient);

            transport.client.connect()
                    .observeOn(AvatarSchedulers.io(entityId.getIotCoreId()))
                    .subscribe(new DisposableCompletableObserver() {
                        @Override
                        public void onComplete() {
                            transport.client.setCallback(new AvatarReconnectCallback(transport));
                            emitter.onSuccess(transport);
                            dispose();
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            // caller retry if failed.
                            emitter.onError(e);
                            dispose();
                        }
                    });
        });
    }

    private MqttAvatarTransport(
            EntityId entityId, int publishRetryTime, ReactiveStreamsMqttClientWrapper mqttAsyncClient) {
        this.entityId = entityId;
        this.client = mqttAsyncClient;
        this.publishRetryTime = publishRetryTime;
    }

    @Override
    public Completable close() {
        closeTask.compareAndSet(null, client.disconnect()
                .doOnError(throwable -> client.disconnectForcibly())
                .doFinally(() -> {
                    client.close();
                    subscribedTopics.values().forEach(subscribedTopic -> {
                        subscribedTopic.originObservableDisposable.get().dispose();
                        subscribedTopic.messages.onComplete();
                    });
                })
                .onErrorResumeNext(throwable -> {
                    log.error("Transport close failed, entityId={}", entityId, throwable);
                    return Completable.complete();
                }));
        return closeTask.get();
    }

    @Override
    public Completable pub(Topic topic, UserMessage userMessage) {
        return client.publish(topic, userMessage)
                // TODO handle different mqtt exception
                // TODO retry when.
                .retry(publishRetryTime);
    }

    @Override
    public BehaviorSubject<Message> sub(Topic topic) {
        AtomicBoolean isNewSubscription = new AtomicBoolean(false);
        SubscribedTopic subscribedTopic = subscribedTopics.computeIfAbsent(topic, k -> {
            isNewSubscription.set(true);
            BehaviorSubject<Message> subject = BehaviorSubject.create();
            return new SubscribedTopic(subject);
        });
        if (isNewSubscription.get()) {
            internalSubscribe(topic, subscribedTopic);
        }
        return subscribedTopic.messages;
    }

    private void internalSubscribe(Topic topic, SubscribedTopic subscribedTopic) {
        Completable.timer(subscribedTopic.subscribeFailRetryWaitingTimeInSec.get(),
                TimeUnit.SECONDS, AvatarSchedulers.io(entityId.getIotCoreId()))
                .subscribe(() -> {
                    if (closeTask.get() != null) {
                        return;
                    }
                    if (subscribedTopic.originObservableDisposable.get() != null) {
                        subscribedTopic.originObservableDisposable.get().dispose();
                    }
                    client.subscribe(topic)
                            .observeOn(AvatarSchedulers.io(entityId.getIotCoreId()))
                            .subscribe(new DisposableSingleObserver<Observable<UserMessage>>() {
                                @Override
                                public void onSuccess(@NonNull Observable<UserMessage> userMessageObservable) {
                                    subscribedTopic.subscribeFailRetryWaitingTimeInSec.set(0);
                                    subscribedTopic.originObservableDisposable.set(userMessageObservable
                                            .observeOn(AvatarSchedulers.io(entityId.getIotCoreId()))
                                            .subscribe(
                                                    subscribedTopic.messages::onNext,
                                                    throwable -> {
                                                        subscribedTopic.messages.onNext(CommandMessage.error(throwable));
                                                        internalSubscribe(topic, subscribedTopic);
                                                    },
                                                    () -> {
                                                        // do nothing
                                                    }));
                                    subscribedTopic.messages.onNext(CommandMessage.ready());
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    int curSubFailRetryWaitingTime =
                                            subscribedTopic.subscribeFailRetryWaitingTimeInSec.get();
                                    if (curSubFailRetryWaitingTime == 0) {
                                        subscribedTopic.subscribeFailRetryWaitingTimeInSec.set(5);
                                    } else {
                                        int newWaitingTime = curSubFailRetryWaitingTime * 2;
                                        subscribedTopic.subscribeFailRetryWaitingTimeInSec
                                                .set(Math.min(newWaitingTime, 120));
                                    }
                                    subscribedTopic.messages.onNext(CommandMessage.error(e));
                                    internalSubscribe(topic, subscribedTopic);
                                }
                            });
                });
    }


    @RequiredArgsConstructor
    static class AvatarReconnectCallback implements MqttCallbackExtended {

        private final MqttAvatarTransport mqttAvatarTransport;

        @Override
        public void connectionLost(Throwable cause) {
            mqttAvatarTransport.subscribedTopics.values().forEach(
                    subscribedTopic -> subscribedTopic.messages.onNext(CommandMessage.error(cause)));
            log.warn("Mqtt connection lost, try to reconnect, entityId={}", mqttAvatarTransport.entityId, cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            if (log.isTraceEnabled()) {
                log.trace("Receive message from topic {}, message: {}", topic, message.toString());
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (reconnect) {
                // we need to resubscribe since we use clean session true
                mqttAvatarTransport.subscribedTopics.forEach(mqttAvatarTransport::internalSubscribe);
            }
        }
    }
}
