/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.iot.shared.sub.transport.disruptor;

import io.reactivex.rxjava3.subjects.Subject;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import com.baidu.iot.shared.sub.transport.enums.Qos;
import com.baidu.iot.shared.sub.transport.model.TransportMessage;

/**
 * Created by mafei01 in 6/3/21 4:38 PM
 */
@Slf4j
public class MessageDisruptorImpl implements IMessageDisruptor {

    private static final MessageDisruptorImpl messageDisruptor = new MessageDisruptorImpl();

    private final Map<String, Subject<TransportMessage>> subjectMap = new HashMap<>();
    private final Disruptor<TransportMessage> disruptor;
    private final RingBuffer<TransportMessage> ringBuffer;

    private MessageDisruptorImpl() {
        this.disruptor = new Disruptor<>(
                () -> TransportMessage.builder().build(),
                1024 * 64,
                r -> {
                    Thread thread = new Thread(r, "message-disruptor");
                    thread.setDaemon(true);
                    return thread;
                },
                ProducerType.MULTI,
                new YieldingWaitStrategy());
        this.ringBuffer = disruptor.getRingBuffer();
        this.disruptor.handleEventsWith(
                (event, sequence, endOfBatch) ->
                        subjectMap.computeIfPresent(event.getTransportId(), (k, s) -> {
                            s.onNext(event);
                            return s;
                        }));
        this.disruptor.start();
    }

    public static MessageDisruptorImpl getInstance() {
        return messageDisruptor;
    }

    @Override
    public void register(String id, Subject<TransportMessage> messageSubject) {
        subjectMap.put(id, messageSubject);
    }

    @Override
    public void unregister(String id) {
        subjectMap.remove(id);
    }

    @Override
    public void publish(String id, String topic, Qos qos, byte[] payload) {
        ringBuffer.publishEvent((event, sequence) -> {
            event.setTransportId(id);
            event.setTopic(topic);
            event.setQos(qos);
            event.setPayload(payload);
        });
    }

}
