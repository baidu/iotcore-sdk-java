// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.method;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.internal.Message;
import com.baidu.iot.device.sdk.avatar.common.internal.MessageId;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author zhangxiao18
 * Date 2020/11/19
 */
public class ObserveDeltaAvatarMethodTest {

    @Test
    public void testObserveSuccess(@Mocked IAvatarTransport avatarTransport) {
        AvatarId avatarId = new AvatarId("test", "test");
        BehaviorSubject<Message> messagePipeline = BehaviorSubject.create();
        AtomicBoolean success = new AtomicBoolean(false);

        new Expectations() {{
           avatarTransport.sub((Topic) any);
           result = messagePipeline;
        }};

        ObserveReportedDeltaMethod method =
                new ObserveReportedDeltaMethod(avatarId.getDeviceSideEntityId(), avatarId, avatarTransport);

        method.observeDelta()
                .subscribe(new DisposableObserver<Delta>() {
                    @Override
                    public void onNext(@NonNull Delta delta) {
                        success.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

        messagePipeline.onNext(new UserMessage(MessageId.next(), Delta.getDefaultInstance()));

        Assert.assertTrue(success.get());

        new Verifications() {{
            avatarTransport.sub((Topic) any);
            times = 1;
        }};
    }

    @Test
    public void testObserveError(@Mocked IAvatarTransport avatarTransport) {
        AvatarId avatarId = new AvatarId("test", "test");
        BehaviorSubject<Message> messagePipeline = BehaviorSubject.create();
        AtomicBoolean failure = new AtomicBoolean(false);

        new Expectations() {{
            avatarTransport.sub((Topic) any);
            result = messagePipeline;
        }};

        ObserveReportedDeltaMethod method =
                new ObserveReportedDeltaMethod(avatarId.getDeviceSideEntityId(), avatarId, avatarTransport);

        method.observeDelta()
                .subscribe(new DisposableObserver<Delta>() {
                    @Override
                    public void onNext(@NonNull Delta delta) {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        messagePipeline.onError(new RuntimeException());

        Assert.assertTrue(failure.get());

        new Verifications() {{
            avatarTransport.sub((Topic) any);
            times = 1;
        }};
    }

}
