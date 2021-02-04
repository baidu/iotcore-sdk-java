package com.baidu.iot.device.sdk.avatar.controlside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.common.internal.IAvatarVersionHolder;
import com.baidu.iot.device.sdk.avatar.common.internal.InProcessMessageQueue;
import com.baidu.iot.device.sdk.avatar.common.internal.method.GetAvatarMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.method.ObserveReportedDeltaMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.method.UpdateDesiredMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.IAvatarTransport;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarRequest;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author zhangxiao18
 * @Date 2020/11/23
 */
public class ControlSideAvatarTest {

    @Mocked
    private IAvatarTransport avatarTransport;
    @Mocked
    private IAvatarVersionHolder avatarVersionHolder;
    @Mocked
    private GetAvatarMethod getAvatarMethod;
    @Mocked
    private UpdateDesiredMethod updateDesiredMethod;
    @Mocked
    private ObserveReportedDeltaMethod observeReportedDeltaMethod;

    private ControlSideAvatar genControlSideAvatar(AvatarId avatarId) {
        ControlSideAvatar controlSideAvatar = new ControlSideAvatar(
                new EntityId("test", "test"), avatarTransport, new InProcessMessageQueue("test", 1000, 1000), 1000);
        ConcurrentHashMap<AvatarId, ControlSideAvatar.ControlledAvatar> controlledAvatars = new ConcurrentHashMap<>();
        ControlSideAvatar.ControlledAvatar controlledAvatar = new ControlSideAvatar.ControlledAvatar(
                avatarVersionHolder, getAvatarMethod, updateDesiredMethod, observeReportedDeltaMethod);
        controlledAvatars.put(avatarId, controlledAvatar);
        controlSideAvatar.setControlledAvatars(controlledAvatars);
        return controlSideAvatar;
    }

    @Test
    public void testInit() {
        ControlSideAvatar controlSideAvatar = genControlSideAvatar(new AvatarId("test", "test"));
        try {
            controlSideAvatar.getAvatar(new AvatarId("test", "test1234"))
                    .blockingSubscribe();
            throw new RuntimeException("Not throw exception");
        } catch (Exception e) {
            // must throw some exception since we do not mock any methods.
            Assert.assertTrue(e.getMessage() == null || !e.getMessage().contains("Not throw exception"));
        }
    }

    @Test
    public void testGetAvatar() {
        AvatarId avatarId = new AvatarId("test", "test1");

        ControlSideAvatar controlSideAvatar = genControlSideAvatar(avatarId);
        new Expectations() {{
           getAvatarMethod.call((GetAvatarRequest) any);
           result = Single.just(GetAvatarReply.newBuilder().build());
        }};

        AtomicBoolean success = new AtomicBoolean(false);
        controlSideAvatar.getAvatar(avatarId).blockingSubscribe(new DisposableSingleObserver<GetAvatarReply>() {
            @Override
            public void onSuccess(@NonNull GetAvatarReply getAvatarReply) {
                success.set(true);
            }

            @Override
            public void onError(@NonNull Throwable e) {

            }
        });
        Assert.assertTrue(success.get());
    }

    @Test
    public void testUpdateDesired() {
        AvatarId avatarId = new AvatarId("test", "test1");
        ControlSideAvatar controlSideAvatar = genControlSideAvatar(avatarId);

        new Expectations() {{
           avatarVersionHolder.getNextDesiredVersion();
           result = Single.just(123);

           updateDesiredMethod.call((UpdateAvatarRequest) any);
           result = Single.just(UpdateAvatarReply.newBuilder().build());
        }};

        AtomicBoolean success = new AtomicBoolean(false);
        controlSideAvatar.updateDesired(avatarId, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test"), new PropertyValue("\"abc\""));
        }}).blockingSubscribe(new DisposableSingleObserver<UpdateAvatarReply>() {
            @Override
            public void onSuccess(@NonNull UpdateAvatarReply updateAvatarReply) {
                success.set(true);
            }

            @Override
            public void onError(@NonNull Throwable e) {

            }
        });
        Assert.assertTrue(success.get());
    }

    @Test
    public void testUpdateDesiredWithMismatchVersion() {
        AvatarId avatarId = new AvatarId("test", "test1");
        ControlSideAvatar controlSideAvatar = genControlSideAvatar(avatarId);

        new Expectations() {{
            avatarVersionHolder.getNextDesiredVersion();
            result = Single.just(123);

            updateDesiredMethod.call((UpdateAvatarRequest) any);
            result = Single.just(UpdateAvatarReply.newBuilder().setStatus(Status.MISMATCHED_VERSION).build());

            avatarVersionHolder.triggerReload();
            result = Completable.complete();
        }};

        AtomicBoolean success = new AtomicBoolean(false);
        controlSideAvatar.updateDesired(avatarId, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test"), new PropertyValue("\"abc\""));
        }}).blockingSubscribe(new DisposableSingleObserver<UpdateAvatarReply>() {
            @Override
            public void onSuccess(@NonNull UpdateAvatarReply updateAvatarReply) {
                success.set(true);
            }

            @Override
            public void onError(@NonNull Throwable e) {

            }
        });
        Assert.assertTrue(success.get());

        new Verifications() {{
           avatarVersionHolder.triggerReload();
           times = 1;
        }};
    }

    @Test
    public void testObserveReportedDelta() throws InterruptedException {
        AvatarId avatarId = new AvatarId("test", "test1");
        ControlSideAvatar controlSideAvatar = genControlSideAvatar(avatarId);
        PublishSubject<Delta> deltas = PublishSubject.create();
        new Expectations() {{
            observeReportedDeltaMethod.observeDelta();
            result = deltas;
        }};

        AtomicBoolean success = new AtomicBoolean(false);
        controlSideAvatar.observeReportedDelta(avatarId)
                .subscribe(new DisposableObserver<Delta>() {
                    @Override
                    public void onNext(@NonNull Delta delta) {
                        success.set(true);
                    }

                    @Override
                    public void onError( @NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        deltas.onNext(Delta.newBuilder().build());

        Thread.sleep(10);
        Assert.assertTrue(success.get());
    }

}
