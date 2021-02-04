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

import com.baidu.iot.device.sdk.avatar.common.internal.AvatarSchedulers;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.MqttTransportConfig;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.TimeUnit;

/**
 * @Author zhangxiao18
 * @Date 2020/11/12
 */
@Slf4j
@RequiredArgsConstructor
public class ReactiveStreamsMqttClientWrapper {

    private final MqttTransportConfig config;

    private final int maxInflightMessageNum;

    private final IMqttAsyncClient client;

    public Single<Observable<UserMessage>> subscribe(Topic topic) {
        return Single.<Observable<UserMessage>>create(emitter -> {
            PublishSubject<UserMessage> messages = PublishSubject.create();
            try {
                client.subscribe(topic.getTopicFilter(), topic.getQos(), null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        if (asyncActionToken.getGrantedQos()[0] == 1) {
                            log.debug("Mqtt client subscribe success, entityId={}, topic={}", config.getEntityId(), topic);
                            emitter.onSuccess(messages);
                        } else {
                            log.error("Mqtt client subscribe error, entityId={}, topic={}, getGrantedQos={}",
                                    config.getEntityId(), topic, asyncActionToken.getGrantedQos()[0]);
                            emitter.onError(new RuntimeException("Get getGrantedQos " + asyncActionToken.getGrantedQos()[0]));
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        log.error("Mqtt client subscribe error, entityId={}, topic={}", config.getEntityId(), topic, exception);
                        emitter.onError(exception);
                    }
                }, new IMqttMessageListener() {
                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        messages.onNext(UserMessage.buildByMqttMessage(new Topic(topic), message));
                    }
                });
            } catch (MqttException e) {
                log.error("Mqtt client subscribe error, entityId={}, topic={}", config.getEntityId(), topic, e);
                emitter.onError(e);
            }
        }).timeout(config.getSubscribeTimeoutMs(), TimeUnit.MILLISECONDS, AvatarSchedulers.io(config.getEntityId().getIotCoreId()));
    }

    public Completable publish(Topic topic, UserMessage userMessage) {
        return Completable.create(
                emitter -> {
                    try {
                        client.publish(topic.getTopicFilter(), userMessage.toMqttMessage(), null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                log.debug("Mqtt client publish success, entityId={}, topic={}, messageId={}",
                                        config.getEntityId(), topic, userMessage.getId());
                                emitter.onComplete();
                            }
                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                log.error("Mqtt client publish error, entityId={}, topic={}, messageId={}",
                                        config.getEntityId(), topic, userMessage.getId(), exception);
                                emitter.onError(exception);
                            }
                        });
                    } catch (MqttException e) {
                        log.error("Mqtt client publish error, entityId={}, topic={}, messageId={}",
                                config.getEntityId(), topic, userMessage.getId(), e);
                        emitter.onError(e);
                    }
                })
                .timeout(config.getPublishTimeoutMs(), TimeUnit.MILLISECONDS, AvatarSchedulers.io(config.getEntityId().getIotCoreId()));
    }

    public Completable disconnect() {
        return Completable.create(
                emitter -> {
                    try {
                        client.disconnect(config.getDisconnectTimeoutMs(), null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                log.debug("Mqtt client disconnect success, entityId={}", config.getEntityId());
                                emitter.onComplete();
                            }
                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                log.warn("Mqtt client disconnect error, entityId={}", config.getEntityId(), exception);
                                emitter.onError(exception);
                            }
                        });
                    } catch (MqttException e) {
                        log.warn("Mqtt client disconnect error, entityId={}", config.getEntityId(), e);
                        emitter.onError(e);
                    }
                });
    }

    public Completable connect() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        if (config.getSslSocketFactory() != null) {
            options.setSocketFactory(config.getSslSocketFactory());
        } else {
            options.setUserName(config.getUsername());
            options.setPassword(config.getPassword());
        }
        options.setMaxInflight(maxInflightMessageNum);
        options.setConnectionTimeout(config.getConnectTimeoutSecond());
        return Completable.create(
                emitter -> {
                    try {
                        client.connect(options, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                log.debug("Mqtt client connect success, config={}", config);
                                emitter.onComplete();
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                log.error("Mqtt client connect failed, config={}", config, exception);
                                emitter.onError(exception);
                            }
                        });
                    } catch (MqttException e) {
                        log.error("Mqtt client connect failed, config={}", config, e);
                        emitter.onError(e);
                    }
                });
    }

    public void disconnectForcibly() throws MqttException {
        client.disconnectForcibly();
    }

    public void close() throws MqttException {
        client.close();
        log.debug("Mqtt client close success, entityId={}", config.getEntityId());
    }

    public void setCallback(MqttCallback mqttCallback) {
        client.setCallback(mqttCallback);
    }
}
