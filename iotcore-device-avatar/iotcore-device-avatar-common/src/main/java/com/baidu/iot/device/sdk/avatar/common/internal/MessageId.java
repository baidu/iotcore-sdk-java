// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal;

import com.baidu.iot.device.sdk.avatar.common.internal.utils.TimeUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chronological messageId。
 * The first digit is fixed to 0, the next 21 digits are a time stamp with a precision of seconds,
 * and the last 10 digits are used as the offset:
 * No repeated id generation within 24 days, up to 1024 messages per second
 *
 * Author zhangxiao18
 * Date 2020/9/25
 */
@EqualsAndHashCode
@Getter
public class MessageId {

    private MessageId(int id) {
        this.id = id;
    }

    private final int id;

    public static MessageId next() {
        return new MessageId(IdGenerator.generate());
    }

    // TODO optimize
    @Override
    public String toString() {
        return String.valueOf(id);
    }

    public static MessageId of(String id) {
        return new MessageId(Integer.parseInt(id));
        /*
        return new MessageId(Ints.fromByteArray(id.getBytes()));
        */
    }

    private static final class IdGenerator {
        private static final int PREFIX = (1 << 31) - 1;
        private static final AtomicInteger SEQ = new AtomicInteger(0);

        private static int generate() {
            int timeSec = TimeUtil.getCurrentTimeSec();
            int s = SEQ.getAndIncrement() & 1023;
            return generate(timeSec, s);
        }

        static int generate(int timeSec, int seq) {
            return PREFIX & (timeSec << 10 | seq);
        }
    }
}
