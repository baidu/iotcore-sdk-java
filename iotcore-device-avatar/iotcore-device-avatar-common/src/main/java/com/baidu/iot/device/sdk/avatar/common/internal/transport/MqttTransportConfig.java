// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.transport;

import com.baidu.iot.device.sdk.avatar.common.EntityId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import javax.net.ssl.SSLSocketFactory;
import java.net.URI;

/**
 * Author zhangxiao18
 * Date 2020/11/18
 */
@Getter
@RequiredArgsConstructor
@ToString
public class MqttTransportConfig {

    private final EntityId entityId;

    private final URI uri;

    private final SSLSocketFactory sslSocketFactory;

    private final String username;

    private final char[] password;

    private final long subscribeTimeoutMs;

    private final long publishTimeoutMs;

    private final int connectTimeoutSecond;

    private final long disconnectTimeoutMs;
}
