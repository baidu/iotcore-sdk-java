// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.shared.sub.transport.disruptor;

import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import com.baidu.iot.shared.sub.transport.enums.Qos;
import com.baidu.iot.shared.sub.transport.model.TransportMessage;

/**
 * Created by mafei01 in 6/8/21 11:48 AM
 */
public class MessageDisruptorImplTest {

    private String id_1 = "test_1";
    private String id_2 = "test_2";
    private Subject<TransportMessage> subject_1;
    private Subject<TransportMessage> subject_2;

    @Before
    public void setup() {
        subject_1 = PublishSubject.create();
        subject_2 = PublishSubject.create();
    }

    @Test
    public void testMessageDisruptor() throws InterruptedException {
        MessageDisruptorImpl disruptor = MessageDisruptorImpl.getInstance();
        byte[] payload = new byte[]{1};
        List<TransportMessage> list_1 = new ArrayList<>();
        List<TransportMessage> list_2 = new ArrayList<>();
        subject_1.subscribe(m -> list_1.add(m));
        subject_2.subscribe(m -> list_2.add(m));
        // register and pub
        disruptor.register(id_1, subject_1);
        disruptor.register(id_2, subject_2);
        for (int i = 0; i < 1000; i++) {
            disruptor.publish(id_1, "test", Qos.AT_LEAST_ONCE, payload);
        }
        Thread.sleep(10);
        Assert.assertEquals(1000, list_1.size());
        Assert.assertEquals(payload, list_1.get(0).getPayload());
        Assert.assertEquals(0, list_2.size());
        // unregister and pub again
        disruptor.unregister(id_1);
        disruptor.publish(id_1, "test", Qos.AT_LEAST_ONCE, payload);
        Assert.assertEquals(1000, list_1.size());
    }
}
