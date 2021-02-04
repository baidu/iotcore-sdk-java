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

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author zhangxiao18
 * @Date 2020/10/6
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
