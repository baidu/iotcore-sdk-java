/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.iot.log.sdk;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;

import com.baidu.iot.shared.sub.transport.ISharedSubTransport;
import com.baidu.iot.shared.sub.transport.model.TransportMessage;
import com.baidu.iot.type.log.LogEntries;
import com.baidu.iot.type.log.LogEntry;

/**
 * Created by mafei01 in 6/4/21 11:24 AM
 */
@Slf4j
public class IotCoreLogger {

    private final ISharedSubTransport transport;

    private final Observable<LogEntry> logEntrySink;

    IotCoreLogger(ISharedSubTransport transport) {
        this.transport = transport;
        this.logEntrySink = transport.listen().flatMap(this::convert);
        transport.transportState().subscribe(state -> log.debug("Transport state={}", state));
    }

    public Observable<LogEntry> receive() {
        return logEntrySink;
    }

    public Completable close() {
        return transport.close();
    }

    private Observable<LogEntry> convert(TransportMessage transportMessage) {
        try {
            LogEntries logEntries = LogEntries.parseFrom(transportMessage.getPayload());
            return Observable.fromIterable(logEntries.getEntriesList());
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to decode transport message", e);
            return Observable.empty();
        }
    }

}
