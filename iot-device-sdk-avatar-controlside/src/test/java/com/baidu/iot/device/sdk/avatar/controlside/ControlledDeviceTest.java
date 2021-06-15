package com.baidu.iot.device.sdk.avatar.controlside;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.Constants;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.common.internal.model.Avatar;
import com.baidu.iot.device.sdk.avatar.common.internal.utils.AvatarHelper;
import com.baidu.iot.mqtt.common.utils.JsonHelper;
import com.baidu.iot.device.sdk.avatar.controlside.exception.GetReportedFailedException;
import com.baidu.iot.device.sdk.avatar.controlside.internal.IControlSideAvatar;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.Operation;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author zhangxiao18
 * Date 2020/11/23
 */
public class ControlledDeviceTest {

    @Mocked
    private IControlSideAvatar controlSideAvatar;

    @Mocked
    private IoTDeviceController controller;

    @Before
    public void init() {
        new Expectations() {{
            controlSideAvatar.init((AvatarId) any);
            result = Completable.complete();
        }};
    }

    @Test
    public void testUpdateDesired() {
        AvatarId avatarId = new AvatarId("test", "test");
        ControlledDevice controlledDevice = 
                ControlledDevice.create(controller, controlSideAvatar, avatarId).blockingGet();

        new Expectations() {{
           controlSideAvatar.updateDesired(avatarId, (Map) any);
           result = Single.just(UpdateAvatarReply.newBuilder().setStatus(Status.SUCCESS).build());
        }};

        controlledDevice.updateDesired(new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test"), new PropertyValue("\"test123\""));
        }}).blockingSubscribe(new DisposableSingleObserver<Status>() {
            @Override
            public void onSuccess(@NonNull Status status) {
                Assert.assertEquals(Status.SUCCESS, status);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Test
    public void testGetReportedFailed() {
        AvatarId avatarId = new AvatarId("test", "test");
        ControlledDevice controlledDevice = 
                ControlledDevice.create(controller, controlSideAvatar, avatarId).blockingGet();

        new Expectations() {{
           controlSideAvatar.getAvatar(avatarId);
           result = Single.just(GetAvatarReply.newBuilder().setStatus(Status.MISMATCHED_VERSION).build());
        }};

        controlledDevice.getReported().blockingSubscribe(new DisposableSingleObserver<VersionedPropertyValue>() {
            @Override
            public void onSuccess(@NonNull VersionedPropertyValue versionedPropertyValue) {
                throw new RuntimeException();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Assert.assertTrue(e instanceof GetReportedFailedException);
            }
        });
    }

    @Test
    public void testGetReportWithDefaultJson() {
        AvatarId avatarId = new AvatarId("test", "test");
        ControlledDevice controlledDevice = 
                ControlledDevice.create(controller, controlSideAvatar, avatarId).blockingGet();

        new Expectations() {{
            controlSideAvatar.getAvatar(avatarId);
            result = Single.just(GetAvatarReply.newBuilder()
                    .setStatus(Status.SUCCESS)
                    .setAvatar(AvatarHelper.buildDefaultAvatarJson())
                    .build());
        }};

        controlledDevice.getReported().blockingSubscribe(new DisposableSingleObserver<VersionedPropertyValue>() {
            @Override
            public void onSuccess(@NonNull VersionedPropertyValue versionedPropertyValue) {
                Assert.assertEquals(0, versionedPropertyValue.getVersion());
            }

            @Override
            public void onError(@NonNull Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testGetReport() {
        AvatarId avatarId = new AvatarId("test", "test");
        ControlledDevice controlledDevice = 
                ControlledDevice.create(controller, controlSideAvatar, avatarId).blockingGet();

        int version = ThreadLocalRandom.current().nextInt();
        Avatar avatar = AvatarHelper.buildDefaultAvatar(avatarId.getId());
        ((ObjectNode) avatar.getReported()).put(Constants.AVATAR_VERSION_FIELD_NAME, version);

        new Expectations() {{
            controlSideAvatar.getAvatar(avatarId);
            try {
                result = Single.just(GetAvatarReply.newBuilder()
                        .setStatus(Status.SUCCESS)
                        .setAvatar(JsonHelper.toJson(avatar))
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }};

        controlledDevice.getReported().blockingSubscribe(new DisposableSingleObserver<VersionedPropertyValue>() {
            @Override
            public void onSuccess(@NonNull VersionedPropertyValue versionedPropertyValue) {
                Assert.assertEquals(version, versionedPropertyValue.getVersion());
            }

            @Override
            public void onError(@NonNull Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testObserveDelta() throws InterruptedException {
        AvatarId avatarId = new AvatarId("test", "test");
        ControlledDevice controlledDevice = 
                ControlledDevice.create(controller, controlSideAvatar, avatarId).blockingGet();

        BehaviorSubject<Delta> deltas = BehaviorSubject.create();
        new Expectations() {{
            controlSideAvatar.observeReportedDelta(avatarId);
            result = deltas;
        }};

        int version = ThreadLocalRandom.current().nextInt();
        AtomicBoolean success = new AtomicBoolean(false);
        controlledDevice.observeReportedDelta(new PropertyKey("test"))
                .subscribe(new DisposableSubscriber<VersionedPropertyValue>() {
                    @Override
                    public void onNext(VersionedPropertyValue versionedPropertyValue) {
                        success.set(true);
                        Assert.assertEquals(version, versionedPropertyValue.getVersion());
                        Assert.assertEquals("\"test123456\"", versionedPropertyValue.toJsonNode().toString());
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        deltas.onNext(Delta.newBuilder()
                .addOperations(Operation.newBuilder()
                        .setKey("test")
                        .setValue("\"test123456\"")
                        .build())
                .setVersion(version)
                .build());
        Thread.sleep(10);
        Assert.assertTrue(success.get());
    }

    @Test
    public void testClose() {
        AvatarId avatarId = new AvatarId("test", "test");
        ControlledDevice controlledDevice = 
                ControlledDevice.create(controller, controlSideAvatar, avatarId).blockingGet();
        controlledDevice.close();

        new Verifications() {{
            controller.closeControlledDevice(avatarId.getId());
            times = 1;
        }};
    }

}
