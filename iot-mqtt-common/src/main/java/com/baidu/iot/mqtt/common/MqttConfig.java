/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.iot.mqtt.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.net.URI;

import javax.net.ssl.SSLSocketFactory;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@ToString
public class MqttConfig {

    private final URI uri;

    private final SSLSocketFactory sslSocketFactory;

    private final String username;

    private final char[] password;

}
