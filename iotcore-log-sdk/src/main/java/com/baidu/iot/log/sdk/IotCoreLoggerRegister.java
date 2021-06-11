/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.iot.log.sdk;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.subjects.SingleSubject;

import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baidu.iot.log.sdk.enums.LogLevel;
import com.baidu.iot.shared.sub.transport.ISharedSubTransport;
import com.baidu.iot.shared.sub.transport.MqttSharedSubTransport;
import com.baidu.iot.shared.sub.transport.MqttTransportConfig;
import com.baidu.iot.shared.sub.transport.model.TransportId;

/**
 * Created by mafei01 in 6/4/21 11:21 AM
 */
@Slf4j
public class IotCoreLoggerRegister {

    @VisibleForTesting
    final Map<Config, Single<IotCoreLogger>> loggerMap = new ConcurrentHashMap<>();

    /**
     * Register a IotCoreLogger according to the specific config, same config would get the same IotCoreLogger.
     *
     * @param config
     *
     * @return
     */
    public Single<IotCoreLogger> registerLogger(Config config) {
        return loggerMap.computeIfAbsent(config, c -> {
            ISharedSubTransport transport = MqttSharedSubTransport.create(
                    MqttTransportConfig.builder()
                            .transportId(new TransportId(config.getIotCoreId(), config.getGroupKey(), null))
                            .clientPoolSize(config.getClientPoolSize())
                            .mqttConfig(config.getMqttConfig())
                            .topicFilters(convertLogTopics(
                                    config.getLogLevel(),
                                    config.getDeviceKeys(),
                                    config.isIncludeLowerLevel()
                            )).build(), null);
            Single<IotCoreLogger> single = Single.create(emitter ->
                    transport.start().subscribe(new DisposableCompletableObserver() {
                        @Override
                        public void onComplete() {
                            emitter.onSuccess(new IotCoreLogger(transport));
                            dispose();
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            emitter.onError(e);
                            dispose();
                        }
                    }));
            SingleSubject<IotCoreLogger> subject = SingleSubject.create();
            single.subscribe(subject);
            return subject;
        });
    }

    public Completable close() {
        log.info("Start to close IotCoreLoggerRegister, size={}", loggerMap.size());
        return Completable.mergeDelayError(loggerMap.values().stream()
                .map(s -> s.timeout(5, TimeUnit.SECONDS).blockingGet().close())
                .collect(Collectors.toList()))
                .doOnComplete(() -> log.info("Close IotCoreLoggerRegister successfully"))
                .doOnError(e -> log.error("Failed to close IotCoreLoggerRegister", e));
    }

    private String[] convertLogTopics(LogLevel level, String[] deviceKeys, boolean includeLower) {
        List<String> topicPrefixList = new ArrayList<>();
        topicPrefixList.add(level.topic);
        for (LogLevel value : LogLevel.values()) {
            if (includeLower && value.code < level.code) {
                topicPrefixList.add(value.topic);
            }
        }
        if (deviceKeys == null || deviceKeys.length == 0) {
            return topicPrefixList.stream().map(t -> t + "#").toArray(String[]::new);
        } else {
            return topicPrefixList.stream()
                    .flatMap(t -> Arrays.stream(deviceKeys).map(d -> t + "+/" + d + "/#"))
                    .toArray(String[]::new);
        }
    }
}
