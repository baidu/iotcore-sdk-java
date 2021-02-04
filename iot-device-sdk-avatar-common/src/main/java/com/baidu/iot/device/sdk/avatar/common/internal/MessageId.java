/*
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * @Author zhangxiao18
 * @Date 2020/9/25
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
        /*
        try {
            return new String(Ints.toByteArray(id), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        */
    }

    public static MessageId of(String id) {
        return new MessageId(Integer.parseInt(id));
        /*
        return new MessageId(Ints.fromByteArray(id.getBytes()));
        */
    }

    private static final class IdGenerator {
        private static final int Prefix = (1 << 31) - 1;
        private static final AtomicInteger seq = new AtomicInteger(0);

        private static int generate() {
            int timeSec = TimeUtil.getCurrentTimeSec();
            int s = seq.getAndIncrement() & 1023;
            return generate(timeSec, s);
        }

        static int generate(int timeSec, int seq) {
            return Prefix & (timeSec << 10 | seq);
        }
    }
}
