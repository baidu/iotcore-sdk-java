package com.baidu.iot.device.sdk.avatar.common.internal.transport.mqtt;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.internal.CommandMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.Message;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;

/**
 * Author zhangxiao18
 * Date 2020/11/20
 */
public class MqttAvatarTransportTest {

    @Test
    public void testInitSuccess(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException {
        EntityId entityId = new EntityId("test", "test");

        new Expectations() {{
           client.connect();
           result = Completable.complete();
        }};

        AtomicBoolean success = new AtomicBoolean(false);
        MqttAvatarTransport.create(entityId, 0, client)
                .subscribe(new DisposableSingleObserver<IAvatarTransport>() {
                    @Override
                    public void onSuccess(@NonNull IAvatarTransport avatarTransport) {
                        success.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

        Thread.sleep(10);
        Assert.assertTrue(success.get());

        new Verifications() {{
           client.connect();
           times = 1;

           client.setCallback((MqttCallback) any);
           times = 1;
        }};
    }

    @Test
    public void testInitError(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException {
        EntityId entityId = new EntityId("test", "test");

        new Expectations() {{
            client.connect();
            result = Completable.error(new RuntimeException());
        }};

        AtomicBoolean failure = new AtomicBoolean(false);
        MqttAvatarTransport.create(entityId, 0, client)
                .subscribe(new DisposableSingleObserver<IAvatarTransport>() {
                    @Override
                    public void onSuccess(@NonNull IAvatarTransport avatarTransport) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                    }
                });

        Thread.sleep(10);
        Assert.assertTrue(failure.get());

        new Verifications() {{
            client.connect();
            times = 1;

            client.setCallback((MqttCallback) any);
            times = 0;
        }};
    }

    @Test
    public void testCloseSuccess(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException, MqttException {
        EntityId entityId = new EntityId("test", "test");
        IAvatarTransport avatarTransport = create(entityId, 0, client);

        new Expectations() {{
           client.disconnect();
           result = Completable.complete();
        }};

        AtomicBoolean success = new AtomicBoolean(false);
        avatarTransport.close()
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        success.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

        Thread.sleep(10);
        Assert.assertTrue(success.get());

        new Verifications() {{
            client.disconnect();
            times = 1;

            client.disconnectForcibly();
            times = 0;

            client.close();
            times = 1;
        }};
    }

    @Test
    public void testCloseError(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException, MqttException {
        EntityId entityId = new EntityId("test", "test");
        IAvatarTransport avatarTransport = create(entityId, 0, client);

        new Expectations() {{
            client.disconnect();
            result = Completable.error(new RuntimeException());
        }};

        AtomicBoolean failure = new AtomicBoolean(false);
        avatarTransport.close()
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        // will not throw error
                        failure.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }
                });

        Thread.sleep(10);
        Assert.assertTrue(failure.get());

        new Verifications() {{
            client.disconnect();
            times = 1;

            client.disconnectForcibly();
            times = 1;

            client.close();
            times = 1;
        }};
    }

    @Test
    public void testPubSuccess(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException, MqttException {
        EntityId entityId = new EntityId("test", "test");
        IAvatarTransport avatarTransport = create(entityId, 0, client);

        new Expectations() {{
            client.publish((Topic) any, (UserMessage) any);
            result = Completable.complete();
        }};

        AtomicBoolean success = new AtomicBoolean(false);
        avatarTransport.pub(Topic.genGetTopic(new AvatarId("test", "test")),
                UserMessage.genGetMessage(GetAvatarRequest.newBuilder().build()))
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        success.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

        Thread.sleep(10);
        Assert.assertTrue(success.get());

        new Verifications() {{
            client.publish((Topic) any, (UserMessage) any);
            times = 1;
        }};
    }

    @Test
    public void test() throws InterruptedException {
        PublishSubject<Boolean> subject = PublishSubject.create();

        Completable result = Completable.create(emitter -> {
            subject.observeOn(Schedulers.computation())
                    .subscribe(r -> {
                        if (r) {
                            emitter.onComplete();
                        } else {
                            emitter.onError(new RuntimeException());
                        }
                    });
        });

        result.retry(1).subscribe(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {

            }

            @Override
            public void onError(@NonNull Throwable e) {

            }
        });
        System.out.println(subject.hasObservers());
        subject.onNext(false);
        Thread.sleep(30);
        subject.onNext(true);

        Thread.sleep(200);
    }

    @Test
    public void testPubRetry(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException, MqttException {
        EntityId entityId = new EntityId("test", "test");
        IAvatarTransport avatarTransport = create(entityId, 2, client);

        AtomicBoolean r = new AtomicBoolean(false);
        AtomicInteger count = new AtomicInteger(0);
        new Expectations() {{
            client.publish((Topic) any, (UserMessage) any);
            result = Completable.create(emitter -> {
                count.incrementAndGet();
                if (r.get()) {
                    emitter.onComplete();
                } else {
                    emitter.onError(new RuntimeException());
                    r.set(true);
                }
            });
        }};

        AtomicBoolean success = new AtomicBoolean(false);
        avatarTransport.pub(Topic.genGetTopic(new AvatarId("test", "test")),
                UserMessage.genGetMessage(GetAvatarRequest.newBuilder().build()))
                .observeOn(Schedulers.computation())
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        success.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }
                });

        Thread.sleep(10);
        Assert.assertTrue(success.get());
        Assert.assertEquals(2, count.get());

        new Verifications() {{
            client.publish((Topic) any, (UserMessage) any);
            times = 1;
        }};
    }

    @Test
    public void testSubSuccess(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException {
        EntityId entityId = new EntityId("test", "test");
        IAvatarTransport avatarTransport = create(entityId, 2, client);

        PublishSubject<Message> subject = PublishSubject.create();
        new Expectations() {{
           client.subscribe((Topic) any);
           result = Single.just(subject);
        }};

        AtomicBoolean success = new AtomicBoolean(false);
        avatarTransport.sub(new Topic("test"))
                .subscribe(new DisposableObserver<Message>() {
                    @Override
                    public void onNext(@NonNull Message message) {
                        success.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        subject.onNext(UserMessage.genGetMessage(GetAvatarRequest.newBuilder().build()));
        Thread.sleep(10);
        Assert.assertTrue(success.get());
    }

    @Test
    public void testSubscribeSameTopicTwice(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException {
        EntityId entityId = new EntityId("test", "test");
        IAvatarTransport avatarTransport = create(entityId, 2, client);

        new Expectations() {{
            client.subscribe((Topic) any);
            result = Single.just(PublishSubject.create());
        }};

        BehaviorSubject<Message> messageObservable1 = avatarTransport.sub(new Topic("test"));
        BehaviorSubject<Message> messageObservable2 = avatarTransport.sub(new Topic("test"));

        Assert.assertSame(messageObservable1, messageObservable2);
        new Verifications() {{
            client.subscribe((Topic) any);
            times = 1;
        }};
    }

    @Test
    public void testSubSuccessButOnError(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException {
        EntityId entityId = new EntityId("test", "test");
        IAvatarTransport avatarTransport = create(entityId, 2, client);

        PublishSubject<Message> subject = PublishSubject.create();
        new Expectations() {{
            client.subscribe((Topic) any);
            result = Single.just(subject);
        }};

        AtomicBoolean receiveError = new AtomicBoolean(false);
        AtomicBoolean onError = new AtomicBoolean(false);
        avatarTransport.sub(new Topic("test"))
                .subscribe(new DisposableObserver<Message>() {
                    @Override
                    public void onNext(@NonNull Message message) {
                        if (message instanceof CommandMessage && message != CommandMessage.ready()) {
                            receiveError.set(true);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        onError.set(true);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        subject.onError(new RuntimeException());
        Thread.sleep(10);
        Assert.assertTrue(receiveError.get());
        Assert.assertFalse(onError.get());
    }

    @Test
    public void testSubError(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException {
        EntityId entityId = new EntityId("test", "test");
        IAvatarTransport avatarTransport = create(entityId, 2, client);

        new Expectations() {{
            client.subscribe((Topic) any);
            result = Single.error(new RuntimeException());
        }};

        AtomicBoolean receiveError = new AtomicBoolean(false);
        AtomicBoolean onError = new AtomicBoolean(false);
        avatarTransport.sub(new Topic("test"))
                .subscribe(new DisposableObserver<Message>() {
                    @Override
                    public void onNext(@NonNull Message message) {
                        if (message instanceof CommandMessage && message != CommandMessage.ready()) {
                            receiveError.set(true);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        onError.set(true);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        Thread.sleep(10);
        Assert.assertTrue(receiveError.get());
        Assert.assertFalse(onError.get());

        avatarTransport.close();
    }

    @Test
    public void testSubSuccessAfterSubError(@Mocked ReactiveStreamsMqttClientWrapper client) throws InterruptedException {
        EntityId entityId = new EntityId("test", "test");
        IAvatarTransport avatarTransport = create(entityId, 2, client);

        PublishSubject<Message> successSubject = PublishSubject.create();

        CompletableFuture<PublishSubject<Message>> errorFuture = new CompletableFuture<>();
        new Expectations() {{
            client.subscribe((Topic) any);
            // the error must throw async
            returns(Single.fromCompletionStage(errorFuture), Single.just(successSubject));
        }};

        AtomicBoolean receiveError = new AtomicBoolean(false);
        AtomicBoolean onError = new AtomicBoolean(false);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicBoolean receiveUserMessage = new AtomicBoolean(false);
        avatarTransport.sub(new Topic("test"))
                .subscribe(new DisposableObserver<Message>() {
                    @Override
                    public void onNext(@NonNull Message message) {
                        if (message instanceof CommandMessage) {
                            if (message == CommandMessage.ready()) {
                                success.set(true);
                            } else {
                                receiveError.set(true);
                            }
                        } else {
                            receiveUserMessage.set(true);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        onError.set(true);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        errorFuture.completeExceptionally(new RuntimeException());
        Thread.sleep(10);
        Assert.assertTrue(receiveError.get());
        Assert.assertFalse(onError.get());

        // sleep for resubscribe
        Thread.sleep(6000);
        successSubject.onNext(UserMessage.genGetMessage(GetAvatarRequest.newBuilder().build()));
        Thread.sleep(10);
        Assert.assertTrue(receiveUserMessage.get());
        Assert.assertTrue(success.get());

        new Verifications() {{
            client.subscribe((Topic) any);
            times = 2;
        }};
    }

    @Test
    public void testAvatarReconnectCallback(@Mocked ReactiveStreamsMqttClientWrapper client,
                                            @Mocked IMqttDeliveryToken token) throws Exception {
        EntityId entityId = new EntityId("test", "test");
        MqttAvatarTransport avatarTransport = (MqttAvatarTransport) create(entityId, 2, client);

        MqttAvatarTransport.AvatarReconnectCallback callback =
                new MqttAvatarTransport.AvatarReconnectCallback(avatarTransport);
        callback.connectionLost(new RuntimeException());
        callback.messageArrived("test", new MqttMessage());
        callback.deliveryComplete(token);
        callback.connectComplete(false, null);

        // no topic sub
        callback.connectComplete(true, null);
        new Verifications() {{
           client.subscribe((Topic) any);
           times = 0;
        }};

        // reconnect with topic subscribing
        PublishSubject<Message> resubscribedMessagePipeline = PublishSubject.create();
        PublishSubject<Message> originMessagePipeline = PublishSubject.create();
        AtomicReference<Disposable> subscribedDisposable = new AtomicReference<>();
        new Expectations() {{
           client.subscribe((Topic) any);
           returns(
                   Single.just(originMessagePipeline.doOnSubscribe(subscribedDisposable::set)),
                   Single.just(resubscribedMessagePipeline));
        }};

        AtomicBoolean terminated = new AtomicBoolean(false);
        AtomicBoolean getMessageAfterReconnect = new AtomicBoolean(false);
        avatarTransport.sub(new Topic("test"))
                .subscribe(new DisposableObserver<Message>() {
                    @Override
                    public void onNext(@NonNull Message message) {
                        getMessageAfterReconnect.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        terminated.set(true);
                    }

                    @Override
                    public void onComplete() {
                        terminated.set(true);
                    }
                });
        Thread.sleep(100);
        Assert.assertFalse(subscribedDisposable.get().isDisposed());
        new Verifications() {{
            client.subscribe((Topic) any);
            times = 1;
        }};

        callback.connectComplete(true, null);
        // sleep for resubscribe
        Thread.sleep(6000);
        Assert.assertTrue(subscribedDisposable.get().isDisposed());

        resubscribedMessagePipeline.onNext(UserMessage.genGetMessage(GetAvatarRequest.newBuilder().build()));
        Thread.sleep(10);
        Assert.assertTrue(getMessageAfterReconnect.get());
        Assert.assertFalse(terminated.get());

        new Verifications() {{
            client.subscribe((Topic) any);
            times = 2;
        }};
    }

    private IAvatarTransport create(EntityId entityId, int publishRetryTime, ReactiveStreamsMqttClientWrapper client) {
        new Expectations() {{
            client.connect();
            result = Completable.complete();
        }};

        return MqttAvatarTransport.create(entityId, publishRetryTime, client).blockingGet();
    }
}
