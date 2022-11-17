// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author zhangxiao18
 * Date 2020/10/6
 */
@RequiredArgsConstructor
public class AvatarConsistenceStrategy implements IAvatarConsistenceStrategy {
    private final long maxMessageInterval;

    private AtomicBoolean isConsistent = new AtomicBoolean(false);
    // include pub success and update version
    private volatile long latestMessageTime = System.currentTimeMillis();
    private volatile int latestVersion = -1;

    @Override
    public void recordReportResult(boolean success) {
        if (success) {
            latestMessageTime = System.currentTimeMillis();
        } else {
            isConsistent.set(false);
        }
    }

    @Override
    public void recordLatestDesiredVersion(int version) {
        latestMessageTime = System.currentTimeMillis();
        if (latestVersion != -1 && version > this.latestVersion + 1) {
            isConsistent.set(false);
        }
        this.latestVersion = version;
    }

    @Override
    public void force(boolean isConsistent) {
        this.latestMessageTime = System.currentTimeMillis();
        this.latestVersion = -1;
        this.isConsistent.set(isConsistent);
    }

    @Override
    public boolean isConsistent() {
        if (System.currentTimeMillis() - latestMessageTime > maxMessageInterval) {
            isConsistent.set(false);
        }
        return isConsistent.get();
    }
}
