package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.common.exception.TooManyInProcessMessageException;
import com.baidu.iot.device.sdk.avatar.deviceside.TestKit;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Author zhangxiao18
 * Date 2020/10/22
 */
public class AvatarReporterTest {

    @Mocked
    private IDeviceSideAvatar deviceSideAvatar;

    @Test
    public void testbasicReport() {
        AvatarId avatarId = TestKit.genRandomAvatarId();
        AvatarReporter avatarReporter = new AvatarReporter(avatarId.getIotCoreId(), 100, 1000);

        Map<PropertyKey, PropertyValue> properties = new HashMap<>();
        properties.put(new PropertyKey("test"), new PropertyValue(TestKit.genRandomJsonStr()));

        new Expectations() {{
            deviceSideAvatar.getAvatarId();
            result = avatarId;

            deviceSideAvatar.updateReported((Map<PropertyKey, PropertyValue>) any);
            result = Single.just(UpdateAvatarReply.newBuilder().setStatus(Status.SUCCESS).build());
        }};

        avatarReporter.updateReported(deviceSideAvatar, properties)
                .timeout(3, TimeUnit.SECONDS)
                .blockingSubscribe(new DisposableSingleObserver<Status>() {
                    @Override
                    public void onSuccess(@NonNull Status status) {
                        Assert.assertEquals(Status.SUCCESS, status);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        throw new RuntimeException(e);
                    }
                });

        new Verifications() {{
            deviceSideAvatar.updateReported((Map<PropertyKey, PropertyValue>) any);
            times = 1;
        }};
    }

    @Test
    public void testReportWithFullInProcessQueue() {
        AvatarId avatarId = TestKit.genRandomAvatarId();
        AvatarReporter avatarReporter = new AvatarReporter(avatarId.getIotCoreId(), 100, 10);

        Map<PropertyKey, PropertyValue> properties = new HashMap<>();
        properties.put(new PropertyKey("test"), new PropertyValue(TestKit.genRandomJsonStr()));

        new Expectations() {{
            deviceSideAvatar.getAvatarId();
            result = avatarId;

            deviceSideAvatar.updateReported((Map<PropertyKey, PropertyValue>) any);
            returns(Single.error(new TooManyInProcessMessageException()),
                    Single.error(new TooManyInProcessMessageException()),
                    Single.just(UpdateAvatarReply.newBuilder().setStatus(Status.SUCCESS).build()));
        }};

        avatarReporter.updateReported(deviceSideAvatar, properties)
                .timeout(3, TimeUnit.SECONDS)
                .blockingSubscribe(new DisposableSingleObserver<Status>() {
                    @Override
                    public void onSuccess(@NonNull Status status) {
                        Assert.assertEquals(Status.SUCCESS, status);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        throw new RuntimeException(e);
                    }
                });

        new Verifications() {{
            deviceSideAvatar.updateReported((Map<PropertyKey, PropertyValue>) any);
            times = 3;
        }};
    }

    @Test
    public void testReportManyTimes() {
        AvatarId avatarId = TestKit.genRandomAvatarId();
        AvatarReporter avatarReporter = new AvatarReporter(avatarId.getIotCoreId(), 100, 10);

        new Expectations() {{
            deviceSideAvatar.getAvatarId();
            result = avatarId;

            deviceSideAvatar.updateReported((Map<PropertyKey, PropertyValue>) any);
            result = Single.just(UpdateAvatarReply.newBuilder().setStatus(Status.SUCCESS).build());
        }};

        Single<Status> result1 = avatarReporter.updateReported(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test"), new PropertyValue(TestKit.genRandomJsonStr()));
        }});

        Single<Status> result2 = avatarReporter.updateReported(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test1"), new PropertyValue(TestKit.genRandomJsonStr()));
        }});
        Single<Status> result3 = avatarReporter.updateReported(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test2"), new PropertyValue(TestKit.genRandomJsonStr()));
        }});

        Single.mergeArray(result1, result2, result3)
                .timeout(3, TimeUnit.SECONDS)
                .blockingSubscribe(new DisposableSubscriber<Status>() {
                    @Override
                    public void onNext(Status status) {
                        Assert.assertEquals(Status.SUCCESS, status);
                    }

                    @Override
                    public void onError(Throwable t) {
                        throw new RuntimeException(t);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        new Verifications() {{
            deviceSideAvatar.updateReported((Map<PropertyKey, PropertyValue>) any);
            times = 3;
        }};
    }

    @Test
    public void testReportManyTimesWithTooManyInProcessMessages() throws InterruptedException {
        AvatarId avatarId = TestKit.genRandomAvatarId();
        AvatarReporter avatarReporter = new AvatarReporter(avatarId.getIotCoreId(), 100, 100);

        new Expectations() {{
            deviceSideAvatar.getAvatarId();
            result = avatarId;

            deviceSideAvatar.updateReported((Map<PropertyKey, PropertyValue>) any);
            returns(
                    Single.error(new TooManyInProcessMessageException()),
                    Single.error(new TooManyInProcessMessageException()),
                    Single.just(UpdateAvatarReply.newBuilder().setStatus(Status.SUCCESS).build())
            );
        }};

        Single<Status> result1 = avatarReporter.updateReported(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test"), new PropertyValue(TestKit.genRandomJsonStr()));
        }});
        Thread.sleep(10);

        Single<Status> result2 = avatarReporter.updateReported(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test2"), new PropertyValue(TestKit.genRandomJsonStr()));
        }});
        Single<Status> result3 = avatarReporter.updateReported(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test3"), new PropertyValue(TestKit.genRandomJsonStr()));
        }});
        Single<Status> result4 = avatarReporter.updateReported(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test4"), new PropertyValue(TestKit.genRandomJsonStr()));
        }});
        Single<Status> result5 = avatarReporter.updateReported(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
            put(new PropertyKey("test5"), new PropertyValue(TestKit.genRandomJsonStr()));
        }});

        Single.mergeArray(result1, result2, result3, result4, result5)
                .timeout(3, TimeUnit.SECONDS)
                .blockingSubscribe(new DisposableSubscriber<Status>() {
                    @Override
                    public void onNext(Status status) {
                        Assert.assertEquals(Status.SUCCESS, status);
                    }

                    @Override
                    public void onError(Throwable t) {
                        throw new RuntimeException(t);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        new Verifications() {{
            deviceSideAvatar.updateReported((Map<PropertyKey, PropertyValue>) any);
            times = 4;
        }};
    }

    public static class WaitingReportQueueTest {

        @Test
        public void testPutAndRemove() {
            AvatarReporter.WaitingReportQueue waitingReportQueue = new AvatarReporter.WaitingReportQueue();
            Assert.assertTrue(waitingReportQueue.isEmpty());

            AvatarId avatarId = TestKit.genRandomAvatarId();
            waitingReportQueue.put(genDeviceSideAvatar(avatarId), new HashMap<>(), new CompletableFuture<>());
            Assert.assertTrue(waitingReportQueue.keySet().contains(avatarId));
            Assert.assertEquals(1, waitingReportQueue.keySet().size());
            Assert.assertFalse(waitingReportQueue.isEmpty());

            waitingReportQueue.remove(avatarId);
            Assert.assertTrue(waitingReportQueue.isEmpty());
        }

        @Test
        public void testPollOldest() throws InterruptedException {
            AvatarReporter.WaitingReportQueue waitingReportQueue = new AvatarReporter.WaitingReportQueue();

            Assert.assertNull(waitingReportQueue.pollOldestWaitingReport());

            AvatarId avatarId = TestKit.genRandomAvatarId();
            waitingReportQueue.put(genDeviceSideAvatar(avatarId), new HashMap<>(), new CompletableFuture<>());

            Thread.sleep(1);
            AvatarId a1 = TestKit.genRandomAvatarId();
            waitingReportQueue.put(genDeviceSideAvatar(a1), new HashMap<>(), new CompletableFuture<>());

            Thread.sleep(1);
            AvatarId a2 = TestKit.genRandomAvatarId();
            waitingReportQueue.put(genDeviceSideAvatar(a2), new HashMap<>(), new CompletableFuture<>());

            Assert.assertEquals(avatarId, waitingReportQueue.pollOldestWaitingReport().getAvatarId());
        }

        @Test
        public void testPutDuplicateKey() {
            AvatarReporter.WaitingReportQueue waitingReportQueue = new AvatarReporter.WaitingReportQueue();
            AvatarId avatarId = TestKit.genRandomAvatarId();
            IDeviceSideAvatar deviceSideAvatar = genDeviceSideAvatar(avatarId);

            String key = TestKit.genRandomStr();
            String oldValue = TestKit.genRandomJsonStr();
            String newValue = TestKit.genRandomJsonStr();

            waitingReportQueue.put(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
                put(new PropertyKey(key), new PropertyValue(oldValue));
            }}, new CompletableFuture<>());
            Assert.assertFalse(waitingReportQueue.isEmpty());

            waitingReportQueue.put(deviceSideAvatar, new HashMap<PropertyKey, PropertyValue>() {{
                put(new PropertyKey(key), new PropertyValue(newValue));
            }}, new CompletableFuture<>());

           Assert.assertEquals(newValue, waitingReportQueue.pollOldestWaitingReport().getProperties().get(new PropertyKey(key)).toString());
        }

        private static IDeviceSideAvatar genDeviceSideAvatar(AvatarId avatarId) {
            return new IDeviceSideAvatar() {

                @Override
                public boolean isAvatarConsistent() {
                    return false;
                }

                @Override
                public void setAvatarConsistence(boolean isConsistent) {

                }

                @Override
                public void recordLatestDesiredVersion(int version) {

                }

                @Override
                public AvatarId getAvatarId() {
                    return avatarId;
                }

                @Override
                public Single<GetAvatarReply> getAvatar() {
                    return null;
                }

                @Override
                public Single<UpdateAvatarReply> updateReported(Map<PropertyKey, PropertyValue> properties) {
                    return null;
                }

                @Override
                public Observable<Delta> observeDesiredDelta() {
                    return null;
                }

                @Override
                public Completable close() {
                    return null;
                }
            };
        }

    }

}
