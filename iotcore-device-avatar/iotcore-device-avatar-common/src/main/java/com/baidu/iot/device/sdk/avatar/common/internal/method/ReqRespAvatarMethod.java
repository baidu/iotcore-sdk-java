// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.method;

import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.exception.AvatarMethodTimeoutException;
import com.baidu.iot.device.sdk.avatar.common.exception.InProcessMessageAlreadyCanceledException;
import com.baidu.iot.device.sdk.avatar.common.exception.TooManyInProcessMessageException;
import com.baidu.iot.device.sdk.avatar.common.internal.AvatarSchedulers;
import com.baidu.iot.device.sdk.avatar.common.internal.CommandMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.Message;
import com.baidu.iot.device.sdk.avatar.common.internal.MessageId;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * Author zhangxiao18
 * Date 2020/10/12
 */
@Slf4j
public abstract class ReqRespAvatarMethod<ReqT, RespT> implements IAvatarMethod {
    private final EntityId entityId;

    private volatile Disposable responseTopicMessageObserveDisposable;

    private final Topic requestTopic;

    private final Topic responseTopic;

    private final InProcessMessageQueue inProcessMessageQueue;

    private final IAvatarTransport transport;

    private final Completable inited;

    protected abstract UserMessage convertRequest(ReqT req);

    protected abstract RespT convertResponse(UserMessage userMessage);

    protected abstract String getMethodName();

    @Override
    public Completable close() {
        responseTopicMessageObserveDisposable.dispose();
        return Completable.complete();
    }

    ReqRespAvatarMethod(EntityId entityId, Topic requestTopic, Topic responseTopic,
                        IAvatarTransport avatarTransport, InProcessMessageQueue inProcessMessageQueue) {
        this.inProcessMessageQueue = inProcessMessageQueue;
        this.transport = avatarTransport;
        this.entityId = entityId;
        this.requestTopic = requestTopic;
        this.responseTopic = responseTopic;

        CompletableFuture<Void> initFuture = new CompletableFuture<>();
        transport.sub(responseTopic)
                .observeOn(AvatarSchedulers.io(entityId.getIotCoreId()))
                .subscribe(new Observer<Message>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        responseTopicMessageObserveDisposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Message message) {
                        initFuture.complete(null);
                        if (message instanceof CommandMessage) {
                            if (message != CommandMessage.ready()) {
                                log.warn("Receive command message from topic {}, entityId={}, messageContent={}",
                                        responseTopic, entityId, message.getContent());
                            } else {
                                log.debug("Receive READY command message from topic {}, entityId={}",
                                        responseTopic, entityId);
                            }
                        } else {
                            UserMessage userMessage = (UserMessage) message;
                            log.debug("Receive message from topic {}, entityId={}, messageId={}",
                                    responseTopic, entityId, userMessage.getId());
                            inProcessMessageQueue.ackMessage(userMessage);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        log.error("Entity {} get error from observing topic {}", entityId, responseTopic, throwable);
                        responseTopicMessageObserveDisposable.dispose();
                    }

                    @Override
                    public void onComplete() {
                        responseTopicMessageObserveDisposable.dispose();
                    }
                });
        this.inited = Completable.fromCompletionStage(initFuture);
    }

    public Single<RespT> call(ReqT req) {
        return inited.andThen(Single.<UserMessage>create(
                emitter -> {
                    UserMessage userMessage = convertRequest(req);
                    InProcessMessageQueue.InProcessMessageCallBack callBack =
                            new InProcessMessageQueue.InProcessMessageCallBack() {
                        @Override
                        public void onExpire(MessageId messageId) {
                            emitter.onError(new AvatarMethodTimeoutException(entityId, messageId, getMethodName()));
                        }

                        @Override
                        public void onCancel(MessageId messageId) {
                            emitter.onError(new InProcessMessageAlreadyCanceledException(entityId));
                        }

                        @Override
                        public void onError(MessageId messageId, Throwable throwable) {
                            emitter.onError(throwable);
                        }

                        @Override
                        public void onComplete(UserMessage message) {
                            emitter.onSuccess(message);
                        }
                    };

                    if (inProcessMessageQueue.add(userMessage.getId(), callBack)) {
                        transport.pub(requestTopic, userMessage)
                                .observeOn(AvatarSchedulers.io(entityId.getIotCoreId()))
                                .subscribe(new DisposableCompletableObserver() {
                                    @Override
                                    public void onComplete() {
                                        dispose();
                                    }

                                    @Override
                                    public void onError(@NonNull Throwable e) {
                                        inProcessMessageQueue.failMessage(userMessage.getId(), e);
                                        dispose();
                                    }
                                });

                    } else {
                        throw new TooManyInProcessMessageException();
                    }
                }).map(this::convertResponse));
    }
}
