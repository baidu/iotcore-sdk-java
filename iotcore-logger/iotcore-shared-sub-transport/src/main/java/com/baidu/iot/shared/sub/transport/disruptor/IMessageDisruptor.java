// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.shared.sub.transport.disruptor;

import io.reactivex.rxjava3.subjects.Subject;

import com.baidu.iot.shared.sub.transport.enums.Qos;
import com.baidu.iot.shared.sub.transport.model.TransportMessage;

public interface IMessageDisruptor {

    void register(String id, Subject<TransportMessage> messageSubject);

    void unregister(String id);

    void publish(String id, String topic, Qos qos, byte[] payload);

}
