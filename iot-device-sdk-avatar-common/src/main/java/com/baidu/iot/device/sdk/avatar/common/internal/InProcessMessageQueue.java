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

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

/**
 * Hold in process message.
 *
 * Author zhangxiao18
 * Date 2020/10/10
 */
@ThreadSafe
@Slf4j
public class InProcessMessageQueue {

    private final static int expirationRoundsNum = 10;

    private final int capacity;

    private final Disposable checkExpirationTask;

    private final List<Queue<MessageId>> expirationRounds;

    private volatile int currentExpiredRound = 0;

    private final StampedLock roundMoveLock = new StampedLock();

    public InProcessMessageQueue(String iotCoreId, int capacity, long expireTimeInMs) {
        this.capacity = capacity;
        this.expirationRounds = new ArrayList<>();
        for (int i = 0; i < expirationRoundsNum; i ++) {
            this.expirationRounds.add(new LinkedBlockingQueue<>());
        }

        checkExpirationTask = Observable.interval(expireTimeInMs / expirationRoundsNum,
                TimeUnit.MILLISECONDS, AvatarSchedulers.task(iotCoreId))
                .subscribe(aLong -> {
                    Queue<MessageId> messageIds = expirationRounds.get(currentExpiredRound);
                    Iterator<MessageId> iterator = messageIds.iterator();
                    while (iterator.hasNext()) {
                        MessageId toExpire = iterator.next();
                        InProcessMessageCallBack callBack = cache.remove(toExpire);
                        if (callBack != null) {
                            count.decrementAndGet();
                            callBack.doExpire(toExpire);
                        }
                        iterator.remove();
                    }
                    moveToNextRound();
                });
    }

    private void moveToNextRound() {
        long ts = roundMoveLock.writeLock();
        currentExpiredRound ++;
        if (currentExpiredRound == expirationRounds.size()) {
            currentExpiredRound = 0;
        }
        roundMoveLock.unlockWrite(ts);
    }

    private int getCurrentAddRound() {
        while (true) {
            long ts = roundMoveLock.tryOptimisticRead();
            int addRound = currentExpiredRound - 1;
            if (addRound < 0) {
                addRound = expirationRounds.size() - 1;
            }
            if (roundMoveLock.validate(ts)) {
                return addRound;
            }
        }
    }

    public Completable close() {
        checkExpirationTask.dispose();
        cache.forEach((messageId, inProcessMessageCallBack) -> inProcessMessageCallBack.doCancel(messageId));
        cache.clear();
        return Completable.complete();
    }

    private final AtomicInteger count = new AtomicInteger(0);

    private final Map<MessageId, InProcessMessageCallBack> cache = new ConcurrentHashMap<>();

    /**
     * add message to inProcess queue
     *
     * @param messageId the id of message
     * @param callBack the callback of messageId
     * @return false if size limit
     */
    public boolean add(MessageId messageId, InProcessMessageCallBack callBack) {
        if (count.get() >= capacity) {
            return false;
        } else {
            expirationRounds.get(getCurrentAddRound()).add(messageId);
            count.incrementAndGet();
            cache.put(messageId, callBack);
            return true;
        }
    }

    public void ackMessage(UserMessage userMessage) {
        InProcessMessageCallBack callBack = cache.remove(userMessage.getId());
        if (callBack != null) {
            count.decrementAndGet();
            callBack.doComplete(userMessage);
        }
    }

    public void failMessage(MessageId messageId, Throwable t) {
        InProcessMessageCallBack callBack = cache.remove(messageId);
        if (callBack != null) {
            count.decrementAndGet();
            callBack.doError(messageId, t);
        }
    }

    public static abstract class InProcessMessageCallBack {
        private final AtomicBoolean done = new AtomicBoolean(false);

        private boolean done() {
            return done.compareAndSet(false, true);
        }

        private void doExpire(MessageId messageId) {
            if (done()) {
                onExpire(messageId);
            }
        }

        private void doError(MessageId messageId, Throwable throwable) {
            if (done()) {
                onError(messageId, throwable);
            }
        }

        private void doComplete(UserMessage userMessage) {
            if (done()) {
                onComplete(userMessage);
            }
        }

        private void doCancel(MessageId messageId) {
            if (done()) {
                onCancel(messageId);
            }
        }

        public abstract void onExpire(MessageId messageId);

        public abstract void onCancel(MessageId messageId);

        public abstract void onError(MessageId messageId, Throwable throwable);

        public abstract void onComplete(UserMessage userMessage);
    }

}
