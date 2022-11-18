// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandMessage implements Message {

    private static final CommandMessage READY = new CommandMessage(null);

    public static CommandMessage ready() {
        return READY;
    }

    public static CommandMessage error(Throwable throwable) {
        return new CommandMessage(throwable);
    }

    @Getter
    private final Object content;
}
