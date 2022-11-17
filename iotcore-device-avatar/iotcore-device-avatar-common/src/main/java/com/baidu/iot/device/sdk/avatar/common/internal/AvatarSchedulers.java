// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Author zhangxiao18
 * Date 2020/10/7
 */
@Slf4j
public class AvatarSchedulers {

    private final String iotCoreId;

    private AvatarSchedulers(String iotCoreId) {
        this.iotCoreId = iotCoreId;
    }

    private ExecutorService task = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, iotCoreId + "-task-thread");
        }
    });

    private ExecutorService io = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, iotCoreId + "-io-thread");
        }
    });

    static ConcurrentHashMap<String, AvatarSchedulers> sdkSchedules = new ConcurrentHashMap<>();

    public static Scheduler task(String iotCoreId) {
        return Schedulers.from(sdkSchedules.computeIfAbsent(iotCoreId, k -> new AvatarSchedulers(iotCoreId)).task);
    }

    public static Scheduler io(String iotCoreId) {
        return Schedulers.from(sdkSchedules.computeIfAbsent(iotCoreId, k -> new AvatarSchedulers(iotCoreId)).io);
    }

    public static ScheduledExecutorService taskExecutorService(String iotCoreId) {
        return (ScheduledExecutorService) sdkSchedules
                .computeIfAbsent(iotCoreId, k -> new AvatarSchedulers(iotCoreId)).task;
    }

    public static void close(String iotCoreId) {
        AvatarSchedulers schedulers = sdkSchedules.remove(iotCoreId);
        if (schedulers != null) {
            try {
                schedulers.task.shutdown();
                schedulers.task.awaitTermination(2, TimeUnit.MINUTES);
                schedulers.io.shutdown();
                schedulers.io.awaitTermination(2, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.warn("Close schedulers of {} timeout. ", iotCoreId);
                schedulers.io.shutdownNow();
                schedulers.task.shutdownNow();
            }
        }
    }
}
