package com.baidu.iot.device.sdk.avatar.common.internal.method;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.exception.AvatarMethodTimeoutException;
import com.baidu.iot.device.sdk.avatar.common.exception.InProcessMessageAlreadyCanceledException;
import com.baidu.iot.device.sdk.avatar.common.exception.TooManyInProcessMessageException;
import com.baidu.iot.device.sdk.avatar.common.internal.CommandMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.Message;
import com.baidu.iot.device.sdk.avatar.common.internal.MessageId;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author zhangxiao18
 * @Date 2020/11/18
 */
public class ReqRespAvatarMethodTest {

    @Test
    public void testInit(@Mocked IAvatarTransport avatarTransport) {
        BehaviorSubject<Message> messagePipeline = BehaviorSubject.create();
        Assert.assertFalse(messagePipeline.hasObservers());

        new Expectations() {{
            avatarTransport.sub((Topic) any);
            result = messagePipeline;
        }};

        AvatarId avatarId = new AvatarId("test", "test");
        GetAvatarMethod mockedReqRespAvatarMethod = new GetAvatarMethod(
                avatarId.getDeviceSideEntityId(),
                avatarId,
                avatarTransport,
                new InProcessMessageQueue("test", 1000, 1000));

        Assert.assertTrue(messagePipeline.hasObservers());
    }

    @Test
    public void testCallSuccess(@Mocked IAvatarTransport avatarTransport) throws InterruptedException {
        BehaviorSubject<Message> messagePipeline = BehaviorSubject.create();
        MessageId staticId = MessageId.of("123");

        new Expectations() {{
            avatarTransport.sub((Topic) any);
            result = messagePipeline;

            avatarTransport.pub((Topic) any, (UserMessage) any);
            result = Completable.complete();
        }};

        new MockUp<GetAvatarMethod>() {
            @Mock
            protected UserMessage convertRequest(GetAvatarRequest getAvatarRequest) {
                return new UserMessage(staticId, getAvatarRequest);
            }
        };

        AvatarId avatarId = new AvatarId("test", "test");
        GetAvatarMethod method = new GetAvatarMethod(
                avatarId.getDeviceSideEntityId(),
                avatarId,
                avatarTransport,
                new InProcessMessageQueue("test", 1000, 1000));

        AtomicBoolean success = new AtomicBoolean(false);
        method.call(GetAvatarRequest.newBuilder().build())
                .subscribe(new DisposableSingleObserver<GetAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull GetAvatarReply getAvatarReply) {
                        success.set(true);
                        Assert.assertEquals(staticId.toString(), getAvatarReply.getReqId());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

        messagePipeline.onNext(new UserMessage(staticId, GetAvatarReply.newBuilder().setReqId(staticId.toString()).build()));

        Thread.sleep(10);
        Assert.assertTrue(success.get());
        new Verifications() {{
            avatarTransport.pub((Topic) any, (UserMessage) any);
            times = 1;

            avatarTransport.sub((Topic) any);
            times = 1;
        }};

        method.close().blockingAwait();

    }

    @Test
    public void testCallTimeout(@Mocked IAvatarTransport avatarTransport) throws InterruptedException {
        BehaviorSubject<Message> messagePipeline = BehaviorSubject.create();

        new Expectations() {{
            avatarTransport.sub((Topic) any);
            result = messagePipeline;

            avatarTransport.pub((Topic) any, (UserMessage) any);
            result = Completable.complete();
        }};

        AvatarId avatarId = new AvatarId("test", "test");
        long messageTimeoutMs = 1000;
        GetAvatarMethod method = new GetAvatarMethod(
                avatarId.getDeviceSideEntityId(),
                avatarId,
                avatarTransport,
                new InProcessMessageQueue("test", 1000, messageTimeoutMs));
        messagePipeline.onNext(CommandMessage.ready());

        AtomicBoolean failure = new AtomicBoolean(false);
        method.call(GetAvatarRequest.newBuilder().build())
                .subscribe(new DisposableSingleObserver<GetAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull GetAvatarReply getAvatarReply) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                        Assert.assertTrue(e instanceof AvatarMethodTimeoutException);
                    }
                });

        Thread.sleep(2 * messageTimeoutMs);
        Assert.assertTrue(failure.get());

    }

    @Test
    public void testCallWithPubFailure(@Mocked IAvatarTransport avatarTransport) throws InterruptedException {
        BehaviorSubject<Message> messagePipeline = BehaviorSubject.create();
        new Expectations() {{
            avatarTransport.sub((Topic) any);
            result = messagePipeline;

            avatarTransport.pub((Topic) any, (UserMessage) any);
            result = Completable.error(new RuntimeException());
        }};

        AvatarId avatarId = new AvatarId("test", "test");
        GetAvatarMethod method = new GetAvatarMethod(
                avatarId.getDeviceSideEntityId(),
                avatarId,
                avatarTransport,
                new InProcessMessageQueue("test", 1000, 1000));

        messagePipeline.onNext(CommandMessage.ready());
        AtomicBoolean failure = new AtomicBoolean(false);
        method.call(GetAvatarRequest.newBuilder().build())
                .subscribe(new DisposableSingleObserver<GetAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull GetAvatarReply getAvatarReply) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                    }
                });

        Thread.sleep(10);
        Assert.assertTrue(failure.get());
        new Verifications() {{
            avatarTransport.pub((Topic) any, (UserMessage) any);
            times = 1;
        }};

    }

    @Test
    public void testCallWithSubMessageError(@Mocked IAvatarTransport avatarTransport) throws InterruptedException {
        BehaviorSubject<Message> messagePipeline = BehaviorSubject.create();

        new Expectations() {{
            avatarTransport.sub((Topic) any);
            result = messagePipeline;

            avatarTransport.pub((Topic) any, (UserMessage) any);
            result = Completable.complete();
        }};

        AvatarId avatarId = new AvatarId("test", "test");
        GetAvatarMethod method = new GetAvatarMethod(
                avatarId.getDeviceSideEntityId(),
                avatarId,
                avatarTransport,
                new InProcessMessageQueue("test", 1000, 1000));
        messagePipeline.onNext(CommandMessage.ready());

        AtomicBoolean failure = new AtomicBoolean(false);
        method.call(GetAvatarRequest.newBuilder().build())
                .subscribe(new DisposableSingleObserver<GetAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull GetAvatarReply getAvatarReply) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                        Assert.assertTrue(e instanceof AvatarMethodTimeoutException);
                    }
                });

        Thread.sleep(2000);
        Assert.assertTrue(failure.get());
    }

    @Test
    public void testCallWithInProcessQueueClosed(@Mocked IAvatarTransport avatarTransport) throws InterruptedException {
        BehaviorSubject<Message> subject = BehaviorSubject.create();
        new Expectations() {{
            avatarTransport.sub((Topic) any);
            result = subject;

            avatarTransport.pub((Topic) any, (UserMessage) any);
            result = Completable.complete();
        }};

        AvatarId avatarId = new AvatarId("test", "test");
        InProcessMessageQueue inProcessMessageQueue = new InProcessMessageQueue("test", 1000, 1000);
        GetAvatarMethod method = new GetAvatarMethod(
                avatarId.getDeviceSideEntityId(),
                avatarId,
                avatarTransport,
                inProcessMessageQueue);

        subject.onNext(CommandMessage.ready());
        AtomicBoolean failure = new AtomicBoolean(false);
        method.call(GetAvatarRequest.newBuilder().build())
                .subscribe(new DisposableSingleObserver<GetAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull GetAvatarReply getAvatarReply) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                        Assert.assertTrue(e instanceof InProcessMessageAlreadyCanceledException);
                    }
                });

        inProcessMessageQueue.close().blockingAwait();
        Thread.sleep(10);
        Assert.assertTrue(failure.get());

    }

    @Test
    public void testCallWithInProcessQueueFull(@Mocked IAvatarTransport avatarTransport) throws InterruptedException {
        BehaviorSubject<Message> messagePipeline = BehaviorSubject.create();
        new Expectations() {{
            avatarTransport.sub((Topic) any);
            result = messagePipeline;
        }};

        AvatarId avatarId = new AvatarId("test", "test");
        InProcessMessageQueue inProcessMessageQueue = new InProcessMessageQueue("test", 0, 1000);
        GetAvatarMethod method = new GetAvatarMethod(
                avatarId.getDeviceSideEntityId(),
                avatarId,
                avatarTransport,
                inProcessMessageQueue);
        messagePipeline.onNext(CommandMessage.ready());

        AtomicBoolean failure = new AtomicBoolean(false);
        method.call(GetAvatarRequest.newBuilder().build())
                .subscribe(new DisposableSingleObserver<GetAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull GetAvatarReply getAvatarReply) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                        Assert.assertTrue(e instanceof TooManyInProcessMessageException);
                    }
                });

        Thread.sleep(10);
        Assert.assertTrue(failure.get());

    }

}
