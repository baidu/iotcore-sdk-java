package com.baidu.iot.device.sdk.avatar.common.internal.method;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.MessageId;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarRequest;
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
 * Author zhangxiao18
 * Date 2020/11/19
 */
public class UpdateDesiredMethodTest {

    @Test
    public void testCallSuccess(@Mocked IAvatarTransport avatarTransport) throws InterruptedException {
        BehaviorSubject<UserMessage> messagePipeline = BehaviorSubject.create();
        MessageId staticId = MessageId.of("123");

        new Expectations() {{
            avatarTransport.sub((Topic) any);
            result = messagePipeline;

            avatarTransport.pub((Topic) any, (UserMessage) any);
            result = Completable.complete();
        }};

        new MockUp<UpdateDesiredMethod>() {
            @Mock
            protected UserMessage convertRequest(UpdateAvatarRequest updateAvatarRequest) {
                return new UserMessage(staticId, updateAvatarRequest);
            }
        };

        AvatarId avatarId = new AvatarId("test", "test");
        UpdateDesiredMethod method = new UpdateDesiredMethod(
                avatarId.getDeviceSideEntityId(),
                avatarId,
                avatarTransport,
                new InProcessMessageQueue("test", 1000, 1000));

        AtomicBoolean success = new AtomicBoolean(false);
        method.call(UpdateAvatarRequest.newBuilder().build())
                .subscribe(new DisposableSingleObserver<UpdateAvatarReply>() {
                    @Override
                    public void onSuccess(@NonNull UpdateAvatarReply updateAvatarReply) {
                        success.set(true);
                        Assert.assertEquals(staticId.toString(), updateAvatarReply.getReqId());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

        messagePipeline.onNext(new UserMessage(staticId, UpdateAvatarReply.newBuilder().setReqId(staticId.toString()).build()));

        Thread.sleep(10);
        Assert.assertTrue(success.get());
        new Verifications() {{
            avatarTransport.pub((Topic) any, (UserMessage) any);
            times = 1;

            avatarTransport.sub((Topic) any);
            times = 1;
        }};

    }

}
