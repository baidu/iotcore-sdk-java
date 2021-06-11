/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.iot.log.sdk;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import mockit.Expectations;
import mockit.Mocked;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import com.baidu.iot.shared.sub.transport.ISharedSubTransport;
import com.baidu.iot.shared.sub.transport.enums.Qos;
import com.baidu.iot.shared.sub.transport.model.TransportMessage;
import com.baidu.iot.type.log.LogEntries;
import com.baidu.iot.type.log.LogEntry;

/**
 * Created by mafei01 in 6/8/21 1:55 PM
 */
public class IotCoreLoggerTest {

    @Mocked
    private ISharedSubTransport transport;

    @Test
    public void testLoggerReceive() throws InterruptedException {
        List<LogEntry> logs = new ArrayList<>();
        LogEntries logEntries = LogEntries.newBuilder()
                .addEntries(LogEntry.newBuilder().setLevel(1).build())
                .addEntries(LogEntry.newBuilder().setLevel(2).build())
                .build();
        Subject<TransportMessage> messageSubject = PublishSubject.create();
        new Expectations() {{
            transport.listen();
            result = messageSubject;
        }};
        IotCoreLogger logger = new IotCoreLogger(transport);
        logger.receive().subscribe(logs::add);
        messageSubject.onNext(TransportMessage.builder()
                .topic("test")
                .qos(Qos.AT_LEAST_ONCE)
                .payload(logEntries.toByteArray())
                .build());
        Thread.sleep(30);
        Assert.assertEquals(2, logs.size());
        Assert.assertEquals(1, logs.get(0).getLevel());
    }

    @Test
    public void testLoggerReceiveFakeMessage() throws InterruptedException {
        List<LogEntry> logs = new ArrayList<>();
        Subject<TransportMessage> messageSubject = PublishSubject.create();
        new Expectations() {{
            transport.listen();
            result = messageSubject;
        }};
        IotCoreLogger logger = new IotCoreLogger(transport);
        logger.receive().subscribe(logs::add);
        messageSubject.onNext(TransportMessage.builder()
                .topic("test")
                .qos(Qos.AT_LEAST_ONCE)
                .payload(new byte[]{1})
                .build());
        Thread.sleep(30);
        Assert.assertEquals(0, logs.size());
    }

    @Test
    public void testLoggerClose() {
        IotCoreLogger logger = new IotCoreLogger(transport);
        new Expectations() {{
            transport.close();
            result = Completable.complete();
        }};
        logger.close().blockingSubscribe();
    }
}
