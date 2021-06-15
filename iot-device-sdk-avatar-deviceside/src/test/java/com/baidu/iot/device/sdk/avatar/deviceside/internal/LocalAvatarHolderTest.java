package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.AvatarId;
import com.baidu.iot.device.sdk.avatar.common.Constants;
import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import com.baidu.iot.device.sdk.avatar.common.internal.model.Avatar;
import com.baidu.iot.device.sdk.avatar.common.internal.utils.AvatarHelper;
import com.baidu.iot.mqtt.common.utils.JsonHelper;
import com.baidu.iot.device.sdk.avatar.deviceside.TestKit;
import com.baidu.iot.thing.avatar.operation.model.Delta;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.Operation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.GeneratedMessageV3;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author zhangxiao18
 * Date 2020/10/23
 */
public class LocalAvatarHolderTest {

    @Mocked
    private IDeviceSideAvatar deviceSideAvatar;

    @Mocked
    private IAvatarController avatarController;

    @Test
    public void testInit() {
        AvatarId avatarId = TestKit.genRandomAvatarId();
        LocalAvatarHolder localAvatarHolder = new LocalAvatarHolder(avatarId, avatarController, deviceSideAvatar, 100);

        new Expectations() {{
            avatarController.observe(deviceSideAvatar);
            result = PublishProcessor.<GeneratedMessageV3>create();
        }};

        BehaviorProcessor<PropertyValue> processor = localAvatarHolder.observeDesired(new PropertyKey("test"));
        Assert.assertFalse(processor.hasComplete());

        new Verifications() {{
           deviceSideAvatar.setAvatarConsistence(false);
           times = 1;
        }};
    }

    @Test
    public void testReceiveGetMessage() throws InterruptedException {
        AvatarId avatarId = TestKit.genRandomAvatarId();
        LocalAvatarHolder localAvatarHolder = new LocalAvatarHolder(avatarId, avatarController, deviceSideAvatar, 100);
        PublishProcessor<GeneratedMessageV3> messagePipeline = PublishProcessor.create();

        new Expectations() {{
            avatarController.observe(deviceSideAvatar);
            result = messagePipeline;
        }};

        PropertyKey key1 = new PropertyKey("test1");
        BehaviorProcessor<PropertyValue> processor1 = localAvatarHolder.observeDesired(key1);

        List<PropertyValue> propertyValues = new ArrayList<>();
        processor1.observeOn(Schedulers.computation()).subscribe(new DisposableSubscriber<PropertyValue>() {
            @Override
            public void onNext(PropertyValue value) {
                propertyValues.add(value);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        PropertyValue propertyValue = new PropertyValue("\"test\"");
        messagePipeline.onNext(genReply(avatarId, new HashMap<>(), 1));
        messagePipeline.onNext(genReply(avatarId, new HashMap<PropertyKey, PropertyValue>() {{
            put(key1, propertyValue);
        }}, 3));
        messagePipeline.onNext(genReply(avatarId, new HashMap<PropertyKey, PropertyValue>() {{
            put(key1, new PropertyValue("\"ab\""));
        }}, 2));
        messagePipeline.onNext(genReply(avatarId, new HashMap<PropertyKey, PropertyValue>() {{
            put(key1, new PropertyValue("\"test123\""));
        }}, 5));

        // waiting for adding
        Thread.sleep(100);
        Assert.assertEquals(3, propertyValues.size());
        Assert.assertEquals("null", propertyValues.get(0).toString());
        Assert.assertEquals(propertyValue.toString(), propertyValues.get(1).toString());
        Assert.assertEquals("\"test123\"", propertyValues.get(2).toString());
    }

    @Test
    public void testReceiveDeltaMessage() throws InterruptedException {
        AvatarId avatarId = TestKit.genRandomAvatarId();
        LocalAvatarHolder localAvatarHolder = new LocalAvatarHolder(avatarId, avatarController, deviceSideAvatar, 100);
        PublishProcessor<GeneratedMessageV3> messagePipeline = PublishProcessor.create();
        new Expectations() {{
            avatarController.observe(deviceSideAvatar);
            result = messagePipeline;
        }};

        PropertyKey key = new PropertyKey("test");
        BehaviorProcessor<PropertyValue> valuePipeline = localAvatarHolder.observeDesired(key);
        List<PropertyValue> propertyValues = new ArrayList<>();
        valuePipeline.observeOn(Schedulers.computation()).subscribe(new DisposableSubscriber<PropertyValue>() {
            @Override
            public void onNext(PropertyValue value) {
                propertyValues.add(value);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        messagePipeline.onNext(genReply(avatarId, new HashMap<>(), 10));

        messagePipeline.onNext(Delta.newBuilder().addOperations(
                Operation.newBuilder().setKey("test").setValue("\"test\"").build()).setVersion(8).build());
        messagePipeline.onNext(Delta.newBuilder().addOperations(
                Operation.newBuilder().setKey("test").setValue("\"test1\"").build()).setVersion(11).build());
        messagePipeline.onNext(Delta.newBuilder().addOperations(
                Operation.newBuilder().setKey("test").setValue("\"test2\"").build()).setVersion(13).build());

        // waiting for adding
        Thread.sleep(100);
        new Verifications() {{
            deviceSideAvatar.setAvatarConsistence(false);
            times = 1;
        }};

        Assert.assertEquals(3, propertyValues.size());
        Assert.assertEquals("null", propertyValues.get(0).toString());
        Assert.assertEquals("\"test1\"", propertyValues.get(1).toString());
        Assert.assertEquals("\"test2\"", propertyValues.get(2).toString());
    }

    @Test
    public void testDownstreamConsumeTooSlow() throws InterruptedException {
        AvatarId avatarId = TestKit.genRandomAvatarId();
        LocalAvatarHolder localAvatarHolder = new LocalAvatarHolder(avatarId, avatarController, deviceSideAvatar, 1);
        PublishProcessor<GeneratedMessageV3> messagePipeline = PublishProcessor.create();
        new Expectations() {{
            avatarController.observe(deviceSideAvatar);
            result = messagePipeline;
        }};

        PropertyKey key = new PropertyKey("test");
        BehaviorProcessor<PropertyValue> valuePipeline = localAvatarHolder.observeDesired(key);
        List<PropertyValue> propertyValues = new ArrayList<>();
        valuePipeline.observeOn(Schedulers.computation()).subscribe(new DisposableSubscriber<PropertyValue>() {
            @Override
            public void onNext(PropertyValue value) {
                try {
                    Thread.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                propertyValues.add(value);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        messagePipeline.onNext(genReply(avatarId, new HashMap<>(), 1));

        int deltaCount = 300;
        for (int i = 2; i <= deltaCount; i ++) {
            Thread.sleep(1);
            messagePipeline.onNext(Delta.newBuilder().addOperations(
                    Operation.newBuilder().setKey("test").setValue("\"test" + i + "\"").build()).setVersion(i).build());
        }

        Thread.sleep(deltaCount * 20);
        Assert.assertTrue(propertyValues.size() < deltaCount);
        Assert.assertEquals("\"test" + deltaCount + "\"", propertyValues.get(propertyValues.size() - 1).toString());
    }

    private GetAvatarReply genReply(AvatarId avatarId, Map<PropertyKey, PropertyValue> desiredProperties, int version) {
        Avatar avatar = AvatarHelper.buildDefaultAvatar(avatarId.getId());
        ObjectNode desired = (ObjectNode) avatar.getDesired();
        desiredProperties.forEach(((key, value) -> desired.set(key.toString(), value.toJsonNode())));
        desired.put(Constants.AVATAR_VERSION_FIELD_NAME, version);
        try {
            return GetAvatarReply.newBuilder().setAvatar(JsonHelper.toJson(avatar)).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
