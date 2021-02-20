package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.deviceside.TestKit;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.UpdateAvatarReply;
import com.google.protobuf.GeneratedMessageV3;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Author zhangxiao18
 * Date 2020/10/21
 */
public class AvatarControllerTest {

    @Test
    public void testControl() throws InterruptedException {
        String iotCoreId = TestKit.genRandomIotCoreId();
        AvatarId avatarId = new AvatarId(iotCoreId, TestKit.genRandomDeviceName());
        AvatarController avatarController = new AvatarController(iotCoreId, 100, 1000);
        AvatarConsistenceStrategy avatarConsistenceStrategy = new AvatarConsistenceStrategy(30000);
        AtomicReference<Emitter<Delta>> deltaEmitter = new AtomicReference<>();
        Observable<Delta> deltaObservable = Observable.create(deltaEmitter::set);
        AtomicInteger getAvatarTimes = new AtomicInteger(0);

        IDeviceSideAvatar deviceSideAvatar = new IDeviceSideAvatar() {
            @Override
            public boolean isAvatarConsistent() {
                return avatarConsistenceStrategy.isConsistent();
            }

            @Override
            public void setAvatarConsistence(boolean isConsistent) {
                avatarConsistenceStrategy.force(isConsistent);
            }

            @Override
            public void recordLatestDesiredVersion(int version) {
                avatarConsistenceStrategy.recordLatestDesiredVersion(version);
            }

            @Override
            public AvatarId getAvatarId() {
                return avatarId;
            }

            @Override
            public Single<GetAvatarReply> getAvatar() {
                getAvatarTimes.incrementAndGet();
                return Single.just(GetAvatarReply.getDefaultInstance());
            }

            @Override
            public Single<UpdateAvatarReply> updateReported(Map<PropertyKey, PropertyValue> properties) {
                return null;
            }

            @Override
            public Observable<Delta> observeDesiredDelta() {
                return deltaObservable;
            }

            @Override
            public Completable close() {
                return null;
            }
        };

        PublishProcessor<GeneratedMessageV3> messages = avatarController.observe(deviceSideAvatar);

        AtomicInteger deltaCount = new AtomicInteger(0);
        AtomicInteger getReplyCount = new AtomicInteger(0);
        messages.observeOn(Schedulers.computation()).subscribe(new DisposableSubscriber<GeneratedMessageV3>() {
            @Override
            public void onNext(GeneratedMessageV3 messageV3) {
                if (messageV3 instanceof Delta) {
                    deltaCount.incrementAndGet();
                } else {
                    avatarConsistenceStrategy.force(true);
                    getReplyCount.incrementAndGet();
                }
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {

            }
        });

        int count = 100;
        for (int i = 0; i < count; i ++) {
            Thread.sleep(1);
            deltaEmitter.get().onNext(Delta.getDefaultInstance());
        }
        Thread.sleep(10);

        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());
        Assert.assertFalse(avatarController.isCrowded(avatarId));
        Assert.assertEquals(count, deltaCount.get());
        Assert.assertEquals(1, getReplyCount.get());
        Assert.assertEquals(1, getAvatarTimes.get());
    }

    @Test
    public void testReload() throws InterruptedException {
        String iotCoreId = TestKit.genRandomIotCoreId();
        AvatarId avatarId = new AvatarId(iotCoreId, TestKit.genRandomDeviceName());
        AvatarController avatarController = new AvatarController(iotCoreId, 100, 10000);

        AvatarConsistenceStrategy avatarConsistenceStrategy = new AvatarConsistenceStrategy(10000);
        CompletableFuture<GetAvatarReply> getAvatarReplyCompletableFuture = new CompletableFuture<>();

        IDeviceSideAvatar deviceSideAvatar = new IDeviceSideAvatar() {
            @Override
            public boolean isAvatarConsistent() {
                return avatarConsistenceStrategy.isConsistent();
            }

            @Override
            public void setAvatarConsistence(boolean isConsistent) {
                avatarConsistenceStrategy.force(isConsistent);
            }

            @Override
            public void recordLatestDesiredVersion(int version) {
                avatarConsistenceStrategy.recordLatestDesiredVersion(version);
            }

            @Override
            public AvatarId getAvatarId() {
                return avatarId;
            }

            @Override
            public Single<GetAvatarReply> getAvatar() {
                return Single.fromCompletionStage(getAvatarReplyCompletableFuture).timeout(10, TimeUnit.SECONDS);
            }

            @Override
            public Single<UpdateAvatarReply> updateReported(Map<PropertyKey, PropertyValue> properties) {
                return null;
            }

            @Override
            public Observable<Delta> observeDesiredDelta() {
                return Observable.just(Delta.getDefaultInstance());
            }

            @Override
            public Completable close() {
                return null;
            }
        };

        avatarConsistenceStrategy.force(false);
        PublishProcessor<GeneratedMessageV3> messages = avatarController.observe(deviceSideAvatar);

        messages.observeOn(Schedulers.computation()).subscribe(new DisposableSubscriber<GeneratedMessageV3>() {
            @Override
            public void onNext(GeneratedMessageV3 messageV3) {
                if (messageV3 instanceof GetAvatarReply) {
                    avatarConsistenceStrategy.force(true);
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        getAvatarReplyCompletableFuture.complete(GetAvatarReply.getDefaultInstance());

        Thread.sleep(1000);
        Assert.assertTrue(avatarConsistenceStrategy.isConsistent());

        avatarController.close().blockingAwait();
    }

    @Test
    public void testControlTooOften() throws InterruptedException {
        String iotCoreId = TestKit.genRandomIotCoreId();
        AvatarId avatarId = new AvatarId(iotCoreId, TestKit.genRandomDeviceName());
        AvatarController avatarController = new AvatarController(iotCoreId, 100, 1000);
        AvatarConsistenceStrategy avatarConsistenceStrategy = new AvatarConsistenceStrategy(30000);
        avatarConsistenceStrategy.force(true);
        AtomicReference<Emitter<Delta>> deltaEmitter = new AtomicReference<>();
        Observable<Delta> deltaObservable = Observable.create(deltaEmitter::set);

        IDeviceSideAvatar deviceSideAvatar = new IDeviceSideAvatar() {
            @Override
            public boolean isAvatarConsistent() {
                return avatarConsistenceStrategy.isConsistent();
            }

            @Override
            public void setAvatarConsistence(boolean isConsistent) {
                avatarConsistenceStrategy.force(isConsistent);
            }

            @Override
            public void recordLatestDesiredVersion(int version) {
                avatarConsistenceStrategy.recordLatestDesiredVersion(version);
            }

            @Override
            public AvatarId getAvatarId() {
                return avatarId;
            }

            @Override
            public Single<GetAvatarReply> getAvatar() {
                return Single.just(GetAvatarReply.getDefaultInstance());
            }

            @Override
            public Single<UpdateAvatarReply> updateReported(Map<PropertyKey, PropertyValue> properties) {
                return null;
            }

            @Override
            public Observable<Delta> observeDesiredDelta() {
                return deltaObservable;
            }

            @Override
            public Completable close() {
                return null;
            }
        };

        PublishProcessor<GeneratedMessageV3> messages = avatarController.observe(deviceSideAvatar);

        AtomicInteger deltaCount = new AtomicInteger(0);
        AtomicInteger getReplyCount = new AtomicInteger(0);
        messages.observeOn(Schedulers.computation()).subscribe(new DisposableSubscriber<GeneratedMessageV3>() {
            @Override
            public void onNext(GeneratedMessageV3 messageV3) {
                if (messageV3 instanceof Delta) {
                    deltaCount.incrementAndGet();
                } else {
                    getReplyCount.incrementAndGet();
                }

                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                request(1);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {

            }
        });

        int count = 1000;
        for (int i = 0; i < count; i ++) {
            Thread.sleep(1);
            deltaEmitter.get().onNext(Delta.getDefaultInstance());
        }
        Assert.assertTrue(avatarController.isCrowded(avatarId));
        Assert.assertFalse(avatarConsistenceStrategy.isConsistent());

        Thread.sleep(1000);
        for (int i = 0; i < 1; i ++) {
            deltaEmitter.get().onNext(Delta.getDefaultInstance());
        }
        Assert.assertFalse(avatarController.isCrowded(avatarId));
        Assert.assertTrue(deltaCount.get() < count);

        avatarController.close().blockingAwait();
    }

}
