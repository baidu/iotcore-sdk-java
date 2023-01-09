// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.logger.standalone;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableObserver;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.baidu.iot.log.sdk.Config;
import com.baidu.iot.log.sdk.IotCoreLogger;
import com.baidu.iot.log.sdk.IotCoreLoggerRegister;
import com.baidu.iot.log.sdk.enums.LogLevel;
import com.baidu.iot.mqtt.common.MqttConfig;
import com.baidu.iot.mqtt.common.MqttConfigFactory;
import com.baidu.iot.mqtt.common.utils.JsonHelper;
import com.baidu.iot.type.log.LogEntry;
import com.baidu.iot.type.mqtt.constants.LogDetailKey;

/**
 * Created by mafei01 in 3/15/21 4:22 PM
 */
@Slf4j
public class Main {

    public static void main(String[] args) {
        Args argJ = new Args();
        JCommander commander = JCommander.newBuilder()
                .addObject(argJ)
                .build();
        commander.parse(args);
        if (argJ.isHelp()) {
            commander.usage();
            System.exit(-1);
        }
        IotCoreLoggerRegister register = new IotCoreLoggerRegister();

        MqttConfig mqttConfig = StringUtils.isEmpty(argJ.getUri())
                ? MqttConfigFactory
                        .genPlainMqttConfig(argJ.getIotCoreId(), argJ.getUsername(), argJ.getPassword().toCharArray()) :
                MqttConfigFactory
                        .genPlainMqttConfig(URI.create(argJ.getUri()),
                                argJ.getUsername(), argJ.getPassword().toCharArray());

        Config config = Config.builder()
                .iotCoreId(argJ.getIotCoreId())
                .mqttConfig(mqttConfig)
                .logLevel(LogLevel.valueOf(argJ.getLevel()))
                .includeLowerLevel(argJ.isIncludeLowerLevel())
                .deviceKeys(argJ.getDeviceKeys())
                .clientPoolSize(argJ.getClientCount())
                .build();

        IotCoreLogger logger = register.registerLogger(config).blockingGet();
        logger.receive().subscribeWith(new DisposableObserver<LogEntry>() {
            @Override
            public void onNext(@NonNull LogEntry logEntry) {
                try {
                    log.info("Receive log: {}", JsonHelper.toJson(PlainLogEntry.parseFrom(logEntry)));
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse logEntry to plain json", e);
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                log.error("Logger error occurred, iotCoreId={}", argJ.getIotCoreId(), e);
                dispose();
            }

            @Override
            public void onComplete() {
                log.info("Logger completed, iotCoreId={}", argJ.getIotCoreId());
                dispose();
            }
        });
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> logger.close().blockingSubscribe(), "Logger-shutdown-hook"));
    }

    @Getter
    @Builder(toBuilder = true)
    static class PlainLogEntry {

        private LogLevel level;

        private String code;

        private Map<String, String> details;

        private int repeated;

        private long timestamp;

        static PlainLogEntry parseFrom(LogEntry logEntry) {
            Function<Integer, String> function;
            if (logEntry.getCode().startsWith("MQT")) {
                function = i -> LogDetailKey.forNumber(i) != null
                        ? LogDetailKey.forNumber(i).name() : String.valueOf(i);
            } else {
                function = String::valueOf;
            }
            Map<String, String> details = new HashMap<>();
            for (Map.Entry<Integer, String> entry : logEntry.getDetailsMap().entrySet()) {
                details.put(function.apply(entry.getKey()), entry.getValue());
            }
            return PlainLogEntry.builder()
                    .code(logEntry.getCode())
                    .details(details)
                    .level(LogLevel.fromCode(logEntry.getLevel()))
                    .repeated(logEntry.getRepeated())
                    .timestamp(logEntry.getTimestamp())
                    .build();
        }
    }
}
