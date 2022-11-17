// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.shared.sub.transport;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.observers.DisposableMaybeObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import com.google.common.annotations.VisibleForTesting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.reactivestreams.Publisher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.baidu.iot.mqtt.common.MqttConfig;
import com.baidu.iot.shared.sub.transport.disruptor.IMessageDisruptor;
import com.baidu.iot.shared.sub.transport.disruptor.MessageDisruptorImpl;
import com.baidu.iot.shared.sub.transport.enums.Qos;
import com.baidu.iot.shared.sub.transport.enums.TransportState;
import com.baidu.iot.shared.sub.transport.model.TransportId;
import com.baidu.iot.shared.sub.transport.model.TransportMessage;

@Slf4j
public class MqttSharedSubTransport implements ISharedSubTransport {

    @VisibleForTesting
    static final int RETRY_TIMES = 5;
    @VisibleForTesting
    static final long RETRY_INTERVAL_IN_MS = 5000;
    @VisibleForTesting
    static final String SHARE_TOPIC_PREFIX = "$share/";
    @VisibleForTesting
    static final String OSHARE_TOPIC_PREFIX = "$oshare/";

    private final TransportId transportId;
    private final MqttConnectOptions options;
    private final long timeoutInMs;

    private final Set<IMqttAsyncClient> clientPool = new HashSet<>();
    private final Set<SharedSubTopic> sharedSubTopics = new HashSet<>();
    private final Map<String, Disposable> subRecoveryDisposables = new HashMap<>();

    private final AtomicReference<Completable> closeTask = new AtomicReference<>();
    private final AtomicReference<TransportState> currentState = new AtomicReference<>();

    private final BehaviorSubject<TransportState> stateSubject = BehaviorSubject.create();
    private final Subject<TransportMessage> sharedMessageSubject = PublishSubject.create();

    private final IMessageDisruptor messageDisruptor;
    private final Scheduler scheduler;

    private static class SharedSubTopic {

        private final String topicFilter;
        private final Qos qos;

        public SharedSubTopic(String topicFilter, Qos qos) {
            if (qos.value() >= Qos.EXACTLY_ONCE.value()) {
                throw new IllegalArgumentException("Only qos0 and qos1 is supported");
            }
            this.topicFilter = topicFilter;
            this.qos = qos;
        }
    }

    /**
     * Create a mqtt share sub transport using specific config.
     */
    public static ISharedSubTransport create(MqttTransportConfig config,
                                             Scheduler scheduler) {
        try {
            Set<IMqttAsyncClient> clients = new HashSet<>();
            for (int i = 0; i < config.getClientPoolSize(); i++) {
                IMqttAsyncClient client = new MqttAsyncClient(config.getMqttConfig().getUri().toString(),
                        config.getTransportId().genMqttClientId(), null);
                clients.add(client);
            }
            return create(config, clients, scheduler);
        } catch (MqttException e) {
            throw new RuntimeException("Create mqtt client failed, config=" + config.toString(), e);
        }
    }

    public static ISharedSubTransport create(MqttTransportConfig config,
                                             Set<IMqttAsyncClient> clients,
                                             Scheduler scheduler) {
        if (clients.size() > 100) {
            throw new IllegalArgumentException("Too many mqtt clients, count=" + clients.size());
        }
        Set<SharedSubTopic> sharedSubTopics = new HashSet<>();
        for (String topicFilter : config.getTopicFilters()) {
            if (!topicFilter.startsWith(SHARE_TOPIC_PREFIX) && !topicFilter.startsWith(OSHARE_TOPIC_PREFIX)) {
                topicFilter = SHARE_TOPIC_PREFIX + config.getTransportId().getGroupKey() + "/" + topicFilter;
            }
            sharedSubTopics.add(new SharedSubTopic(topicFilter, config.getQos()));
        }
        return new MqttSharedSubTransport(config.getTransportId(),
                config.getMqttConfig(),
                config.getMaxInflightMessageNum(),
                config.getTimeoutInMs(),
                clients,
                sharedSubTopics,
                scheduler);
    }

    private MqttSharedSubTransport(TransportId transportId,
                                   MqttConfig mqttConfig,
                                   int maxInflightMessageNum,
                                   long timeoutInMs,
                                   Set<IMqttAsyncClient> clients,
                                   Set<SharedSubTopic> sharedSubTopics,
                                   Scheduler scheduler) {
        this.transportId = transportId;
        this.timeoutInMs = timeoutInMs;
        this.clientPool.addAll(clients);
        this.sharedSubTopics.addAll(sharedSubTopics);
        this.messageDisruptor = MessageDisruptorImpl.getInstance();
        this.options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setMaxReconnectDelay(10000);
        options.setCleanSession(true);
        if (mqttConfig.getSslSocketFactory() != null) {
            options.setSocketFactory(mqttConfig.getSslSocketFactory());
        }
        options.setUserName(mqttConfig.getUsername());
        options.setPassword(mqttConfig.getPassword());
        options.setMaxInflight(maxInflightMessageNum);
        options.setConnectionTimeout((int) Math.max(1, timeoutInMs / 1000));
        this.scheduler = scheduler != null ? scheduler : Schedulers.from(new ThreadPoolExecutor(
                2, 2, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10240),
                r -> {
                    Thread thread = new Thread(r, "Transport-rx-thread-" + transportId.getId());
                    thread.setDaemon(Thread.currentThread().isDaemon());
                    return thread;
                }
        ));
    }

    /**
     * Start the transport, complete when all clients connect and subs completed.
     * Connection and subs would retry{@link #RETRY_TIMES} when failed.
     */
    @Override
    public Completable start() {
        if (currentState.compareAndSet(null, TransportState.CONNECTING)) {
            stateSubject.onNext(TransportState.CONNECTING);
            CompletableSubject result = CompletableSubject.create();
            AtomicInteger successCount = new AtomicInteger();
            List<Single<Boolean>> initResults = clientPool.stream().map(client ->
                    Single.<Boolean>create(emitter -> {
                        Completable connWithRetry = Completable.defer(() -> internalConnect(client))
                                .subscribeOn(scheduler)
                                .retryWhen(new RetryWithDelay(RETRY_TIMES, RETRY_INTERVAL_IN_MS, scheduler));
                        Completable subWithRetry = Completable.defer(() -> internalSubscribe(client))
                                .subscribeOn(scheduler)
                                .retryWhen(new RetryWithDelay(RETRY_TIMES, RETRY_INTERVAL_IN_MS, scheduler));
                        connWithRetry.andThen(subWithRetry)
                                .doOnComplete(successCount::incrementAndGet)
                                .observeOn(scheduler)
                                .subscribe(new DisposableCompletableObserver() {
                                    @Override
                                    public void onComplete() {
                                        emitter.onSuccess(true);
                                        dispose();
                                    }

                                    @Override
                                    public void onError(@NonNull Throwable e) {
                                        emitter.onSuccess(false);
                                        dispose();
                                    }
                                });
                    })).collect(Collectors.toList());
            Flowable.fromIterable(initResults)
                    .flatMap(Flowable::fromSingle)
                    .reduce((a, b) -> a | b)
                    .observeOn(scheduler)
                    .subscribe(new DisposableMaybeObserver<Boolean>() {
                        @Override
                        public void onSuccess(@NonNull Boolean r) {
                            if (r) {
                                log.info("Transport started, success mqtt client count={}", successCount.get());
                                messageDisruptor.register(transportId.getId(), sharedMessageSubject);
                                result.onComplete();
                            } else {
                                result.onError(new RuntimeException("Failed to init any mqtt client"));
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            dispose();
                        }

                        @Override
                        public void onComplete() {
                            dispose();
                        }
                    });
            return result;
        } else {
            return Completable.error(new RuntimeException(String.format("Transport: %s already starting or started",
                    transportId.getId())));
        }
    }

    @Override
    public Observable<TransportMessage> listen() {
        return sharedMessageSubject.observeOn(scheduler);
    }

    @Override
    public Observable<TransportState> transportState() {
        return stateSubject.observeOn(scheduler);
    }

    @Override
    public Completable close() {
        closeTask.compareAndSet(null, Completable.mergeDelayError(
                clientPool.stream()
                        .map(this::internalDisconnect)
                        .collect(Collectors.toList())
                ).doFinally(() -> {
                    stateSubject.onComplete();
                    sharedMessageSubject.onComplete();
                    messageDisruptor.unregister(transportId.getId());
                    scheduler.shutdown();
                })
        );
        updateState();
        return closeTask.get();
    }

    private Completable internalConnect(IMqttAsyncClient client) {
        if (closeTask.get() != null) {
            return Completable.complete();
        }
        client.setCallback(new RecoveryMqttCallBack(this, client));
        return Completable.create(
                emitter -> {
                    try {
                        client.connect(options, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                emitter.onComplete();
                                updateState();
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                log.error("Mqtt client connect failed, transportId={}", transportId.getId(), exception);
                                emitter.onError(exception);
                                updateState();
                            }
                        });
                    } catch (MqttException e) {
                        log.error("Mqtt client connect failed, transportId={}", transportId.getId(), e);
                        emitter.onError(e);
                    }
                });
    }

    /**
     * Sub all sharedTopics, failed when any single subscribe error or granted 128.
     */
    private Completable internalSubscribe(IMqttAsyncClient client) {
        if (closeTask.get() != null) {
            return Completable.complete();
        }
        return Completable.merge(
                sharedSubTopics.stream().map(sharedSubTopic -> Completable.create(emitter ->
                        client.subscribe(sharedSubTopic.topicFilter, sharedSubTopic.qos.value(), null,
                                new IMqttActionListener() {
                                    @Override
                                    public void onSuccess(IMqttToken asyncActionToken) {
                                        int grantedQos = asyncActionToken.getGrantedQos()[0];
                                        if (grantedQos <= sharedSubTopic.qos.value()) {
                                            log.debug("Mqtt subscribe success, clientId={}, topic={}, grantedQos={}",
                                                    client.getClientId(), sharedSubTopic.topicFilter, grantedQos);
                                            emitter.onComplete();
                                        } else {
                                            log.error("Mqtt subscribe failed, clientId={}, topic={}, getGrantedQos={}",
                                                    client.getClientId(), sharedSubTopic.topicFilter, grantedQos);
                                            emitter.onError(new RuntimeException("Get getGrantedQos " + grantedQos));
                                        }
                                    }

                                    @Override
                                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                        log.error("Mqtt subscribe error, clientId={}, topic={}",
                                                client.getClientId(), sharedSubTopic.topicFilter, exception);
                                        emitter.onError(exception);
                                    }
                                })).timeout(timeoutInMs, TimeUnit.MILLISECONDS, scheduler)
                ).collect(Collectors.toList()));
    }

    private Disposable recoverySubscribe(IMqttAsyncClient client) {
        return Completable.defer(() -> internalSubscribe(client))
                .subscribeOn(scheduler)
                .retryWhen(new RetryWithDelay(Integer.MAX_VALUE, RETRY_INTERVAL_IN_MS, scheduler))
                .doOnComplete(() -> this.subRecoveryDisposables.remove(client.getClientId()))
                .subscribe();
    }

    private Completable internalDisconnect(IMqttAsyncClient client) {
        subRecoveryDisposables.computeIfPresent(client.getClientId(),
                (k, d) -> {
                    d.dispose();
                    return null;
                });
        return Completable.create(
                emitter -> client.disconnect(timeoutInMs, null,
                        new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                log.debug("Mqtt client disconnect success, clientId={}", client.getClientId());
                                emitter.onComplete();
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                log.warn("Mqtt client disconnect error, clientId={}", client.getClientId(), exception);
                                emitter.onError(exception);
                            }
                        }))
                .observeOn(scheduler)
                .doOnError(throwable -> client.disconnectForcibly())
                .onErrorResumeNext(throwable -> Completable.complete())
                .doFinally(client::close);
    }

    private void updateState() {
        currentState.getAndUpdate(transportState -> {
            TransportState stateNow = closeTask.get() != null ? TransportState.SHUTDOWN
                    : clientPool.stream().anyMatch(IMqttAsyncClient::isConnected)
                    ? TransportState.READY : TransportState.FAILURE;
            if (transportState != stateNow) {
                stateSubject.onNext(stateNow);
            }
            return stateNow;
        });
    }

    static class RetryWithDelay implements Function<Flowable<Throwable>, Publisher<Long>> {
        private final int retryTimes;
        private final long retryIntervalInMs;
        private final Scheduler scheduler;

        private final AtomicInteger count = new AtomicInteger();

        public RetryWithDelay(int retryTimes, long retryIntervalInMs, Scheduler scheduler) {
            this.retryTimes = retryTimes;
            this.retryIntervalInMs = retryIntervalInMs;
            this.scheduler = scheduler;
        }

        @Override
        public Publisher<Long> apply(Flowable<Throwable> throwableFlowable) throws Throwable {
            return throwableFlowable.flatMap(error -> {
                if (count.incrementAndGet() < retryTimes) {
                    log.warn("Failed and retry #{} times", count.get(), error);
                    return Flowable.timer(retryIntervalInMs, TimeUnit.MILLISECONDS, scheduler);
                } else {
                    return Flowable.<Long>error(error);
                }
            });
        }
    }

    @RequiredArgsConstructor
    static class RecoveryMqttCallBack implements MqttCallbackExtended {

        private final MqttSharedSubTransport transport;
        private final IMqttAsyncClient client;

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            log.debug("Mqtt client connect success, reconnect={}, transportId={}",
                    reconnect, transport.transportId.getId());
            transport.updateState();
            if (reconnect) {
                transport.subRecoveryDisposables.computeIfAbsent(client.getClientId(),
                        k -> transport.recoverySubscribe(client));
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            log.warn("Mqtt connection lost, will try reconnect, transportId={}", transport.transportId.getId(), cause);
            transport.updateState();
            transport.subRecoveryDisposables.computeIfPresent(client.getClientId(),
                    (k, d) -> {
                        d.dispose();
                        return null;
                    });
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            // handle shared message here for that shared message would not trigger sub callback
            transport.messageDisruptor.publish(transport.transportId.getId(), topic,
                    Qos.valueOf(message.getQos()), message.getPayload());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }
}
