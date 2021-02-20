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
