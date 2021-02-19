package com.baidu.iot.device.sdk.avatar.common.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.Constants;
import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.TestKit;
import com.baidu.iot.device.sdk.avatar.common.exception.EntityAlreadyClosedException;
import com.baidu.iot.device.sdk.avatar.common.exception.ReloadAvatarVersionFailedException;
import com.baidu.iot.device.sdk.avatar.common.internal.method.GetAvatarMethod;
import com.baidu.iot.device.sdk.avatar.common.internal.model.Avatar;
import com.baidu.iot.device.sdk.avatar.common.internal.utils.AvatarHelper;
import com.baidu.iot.device.sdk.avatar.common.utils.JsonHelper;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;
import com.baidu.iot.thing.avatar.operation.model.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author zhangxiao18
 * @Date 2020/10/16
 */
public class AvatarVersionHolderTest {

    private AvatarId avatarId = new AvatarId(TestKit.genRandomIotCoreId(), TestKit.genRandomDeviceName());

    private EntityId entityId = avatarId.getDeviceSideEntityId();

    @Injectable
    private AvatarId avatarIdToInject;

    @Injectable
    private EntityId entityIdToInject;

    @Injectable
    private GetAvatarMethod getAvatarMethod;

    @Injectable(value = "100")
    private long reloadTimeout;

    @Tested
    private AvatarVersionHolder avatarVersionHolder;

    @Before
    public void setUp() {
        new Expectations() {{
            avatarIdToInject.getIotCoreId();
            result = avatarId.getIotCoreId();
            minTimes = 0;

            avatarIdToInject.getId();
            result = avatarId.getId();
            minTimes = 0;

            entityIdToInject.getIotCoreId();
            result = entityId.getIotCoreId();
            minTimes = 0;

            entityIdToInject.genMqttClientId();
            result = entityId.genMqttClientId();
            minTimes = 0;

            getAvatarMethod.call((GetAvatarRequest) any);
            result = Single.just(genGetReply(avatarId, 1, 1, Status.SUCCESS));
        }};
    }

    @Test
    public void testGetNextVersion() throws InterruptedException {
        avatarVersionHolder.getNextReportedVersion().blockingGet();

        int reportedVersion = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        int desiredVersion = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);

        new Expectations() {{
            getAvatarMethod.call((GetAvatarRequest) any);
            result = Single.just(genGetReply(avatarId, reportedVersion, desiredVersion, Status.SUCCESS));
        }};

        avatarVersionHolder.triggerReload().blockingAwait();

        Assert.assertEquals(Integer.valueOf(reportedVersion + 1), avatarVersionHolder.getNextReportedVersion().blockingGet());
        Assert.assertEquals(Integer.valueOf(desiredVersion + 1), avatarVersionHolder.getNextDesiredVersion().blockingGet());
    }

    @Test
    public void testTriggerReload() {
        Assert.assertEquals(Integer.valueOf(2), avatarVersionHolder.getNextReportedVersion().blockingGet());
        Assert.assertEquals(Integer.valueOf(2), avatarVersionHolder.getNextDesiredVersion().blockingGet());

        int reportedVersion = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        int desiredVersion = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);

        new Expectations() {{
            getAvatarMethod.call((GetAvatarRequest) any);
            result = Single.just(genGetReply(avatarId, reportedVersion, desiredVersion, Status.SUCCESS));
        }};
        avatarVersionHolder.triggerReload().blockingAwait();

        AtomicBoolean success = new AtomicBoolean(false);
        avatarVersionHolder.getNextReportedVersion()
                .blockingSubscribe(new DisposableSingleObserver<Integer>() {
                    @Override
                    public void onSuccess(@NonNull Integer integer) {
                        success.set(true);
                        Assert.assertEquals(reportedVersion + 1, integer.intValue());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
        avatarVersionHolder.getNextDesiredVersion()
                .blockingSubscribe(new DisposableSingleObserver<Integer>() {
                    @Override
                    public void onSuccess(@NonNull Integer integer) {
                        success.set(true);
                        Assert.assertEquals(desiredVersion + 1, integer.intValue());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

    }

    @Test
    public void testCallAfterClose() {
        avatarVersionHolder.close();
        avatarVersionHolder.triggerReload()
                .blockingSubscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        throw new RuntimeException();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Assert.assertTrue(e instanceof EntityAlreadyClosedException);
                    }
                });

        avatarVersionHolder.getNextDesiredVersion()
                .blockingSubscribe(new DisposableSingleObserver<Integer>() {

                    @Override
                    public void onSuccess(@NonNull Integer integer) {
                        throw new RuntimeException();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Assert.assertTrue(e instanceof EntityAlreadyClosedException);
                    }
                });

        avatarVersionHolder.getNextReportedVersion()
                .blockingSubscribe(new DisposableSingleObserver<Integer>() {

                    @Override
                    public void onSuccess(@NonNull Integer integer) {
                        throw new RuntimeException();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Assert.assertTrue(e instanceof EntityAlreadyClosedException);
                    }
                });
    }

    private static GetAvatarReply genGetReply(AvatarId avatarId, int reportedVersion, int desiredVersion, Status status) {
        Avatar avatar = AvatarHelper.buildDefaultAvatar(avatarId.getId());
        ObjectNode reported = (ObjectNode) avatar.getReported();
        reported.put(Constants.AVATAR_VERSION_FIELD_NAME, reportedVersion);

        ObjectNode desired = (ObjectNode) avatar.getDesired();
        desired.put(Constants.AVATAR_VERSION_FIELD_NAME, desiredVersion);

        try {
            return GetAvatarReply.newBuilder()
                    .setStatus(status)
                    .setAvatar(JsonHelper.toJson(avatar))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
