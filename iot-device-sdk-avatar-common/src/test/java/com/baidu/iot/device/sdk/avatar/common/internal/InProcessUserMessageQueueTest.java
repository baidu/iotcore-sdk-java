package com.baidu.iot.device.sdk.avatar.common.internal;

import com.baidu.iot.device.sdk.avatar.common.TestKit;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Author zhangxiao18
 * @Date 2020/10/18
 */
public class InProcessUserMessageQueueTest {

    @Test
    public void testAck() {
        InProcessMessageQueue inProcessMessageQueue = new InProcessMessageQueue(
                TestKit.genRandomIotCoreId(), 10, 10000);
        MessageId messageId = MessageId.next();

        CompletableFuture<Void> result = new CompletableFuture<>();
        inProcessMessageQueue.add(messageId, new InProcessMessageQueue.InProcessMessageCallBack() {
            @Override
            public void onExpire(MessageId messageId) {

            }

            @Override
            public void onCancel(MessageId messageId) {

            }

            @Override
            public void onError(MessageId messageId, Throwable throwable) {

            }

            @Override
            public void onComplete(UserMessage userMessage) {
                result.complete(null);
            }
        });
        inProcessMessageQueue.ackMessage(new UserMessage(messageId, null));

        inProcessMessageQueue.failMessage(messageId, new RuntimeException());

        Completable.fromCompletionStage(result).timeout(100, TimeUnit.MILLISECONDS).blockingAwait();
    }

    @Test
    public void testFail() {
        InProcessMessageQueue inProcessMessageQueue = new InProcessMessageQueue(
                TestKit.genRandomIotCoreId(), 10, 10000);
        MessageId messageId = MessageId.next();

        CompletableFuture<Void> result = new CompletableFuture<>();
        inProcessMessageQueue.add(messageId, new InProcessMessageQueue.InProcessMessageCallBack() {
            @Override
            public void onExpire(MessageId messageId) {

            }

            @Override
            public void onCancel(MessageId messageId) {

            }

            @Override
            public void onError(MessageId messageId, Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onComplete(UserMessage userMessage) {
            }
        });
        inProcessMessageQueue.failMessage(messageId, new RuntimeException("Failed"));

        // will perform nothing
        inProcessMessageQueue.ackMessage(new UserMessage(messageId, null));

        Completable.fromCompletionStage(result)
                .blockingSubscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        throw new RuntimeException();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Assert.assertTrue(e.getMessage().contains("Failed"));
                    }
                });
    }

    @Test
    public void testExpired() {
        InProcessMessageQueue inProcessMessageQueue = new InProcessMessageQueue(
                TestKit.genRandomIotCoreId(), 10, 100);
        MessageId messageId = MessageId.next();

        CompletableFuture<Void> result = new CompletableFuture<>();
        inProcessMessageQueue.add(messageId, new InProcessMessageQueue.InProcessMessageCallBack() {
            @Override
            public void onExpire(MessageId messageId) {
                result.completeExceptionally(new RuntimeException("Expired"));
            }

            @Override
            public void onCancel(MessageId messageId) {

            }

            @Override
            public void onError(MessageId messageId, Throwable throwable) {

            }

            @Override
            public void onComplete(UserMessage userMessage) {

            }
        });

        Completable.fromCompletionStage(result)
                .timeout(2000, TimeUnit.MILLISECONDS)
                .blockingSubscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        throw new RuntimeException();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Assert.assertTrue(e.getMessage().contains("Expired"));
                    }
                });
    }

    @Test
    public void testClose() {
        InProcessMessageQueue inProcessMessageQueue = new InProcessMessageQueue(
                TestKit.genRandomIotCoreId(), 10, 100);
        MessageId messageId = MessageId.next();

        CompletableFuture<Void> result = new CompletableFuture<>();
        inProcessMessageQueue.add(messageId, new InProcessMessageQueue.InProcessMessageCallBack() {
            @Override
            public void onExpire(MessageId messageId) {

            }

            @Override
            public void onCancel(MessageId messageId) {
                result.completeExceptionally(new RuntimeException("Canceled"));
            }

            @Override
            public void onError(MessageId messageId, Throwable throwable) {

            }

            @Override
            public void onComplete(UserMessage userMessage) {

            }
        });

        inProcessMessageQueue.close().blockingAwait();

        // will perform nothing
        inProcessMessageQueue.ackMessage(new UserMessage(messageId, null));

        Completable.fromCompletionStage(result)
                .timeout(1000, TimeUnit.MILLISECONDS)
                .blockingSubscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        throw new RuntimeException();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Assert.assertTrue(e.getMessage().contains("Canceled"));
                    }
                });
    }

    @Test
    public void testCapacity() {
        int capacity = 10;
        InProcessMessageQueue inProcessMessageQueue = new InProcessMessageQueue(
                TestKit.genRandomIotCoreId(),  capacity, 100);

        InProcessMessageQueue.InProcessMessageCallBack callBack = new InProcessMessageQueue.InProcessMessageCallBack() {
            @Override
            public void onExpire(MessageId messageId) {

            }

            @Override
            public void onCancel(MessageId messageId) {

            }

            @Override
            public void onError(MessageId messageId, Throwable throwable) {

            }

            @Override
            public void onComplete(UserMessage message) {

            }
        };
        for (int i = 0; i < capacity; i ++) {
            Assert.assertTrue(inProcessMessageQueue.add(MessageId.next(), callBack));
        }
        Assert.assertFalse(inProcessMessageQueue.add(MessageId.next(), callBack));
    }
}
