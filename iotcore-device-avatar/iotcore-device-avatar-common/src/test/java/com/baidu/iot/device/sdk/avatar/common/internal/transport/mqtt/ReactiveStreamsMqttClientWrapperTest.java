// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.transport.mqtt;

import com.baidu.iot.device.sdk.avatar.common.EntityId;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.transport.MqttTransportConfig;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarReply;
import com.baidu.iot.thing.avatar.operation.model.GetAvatarRequest;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Author zhangxiao18
 * Date 2020/11/18
 */
public class ReactiveStreamsMqttClientWrapperTest {

    private MqttTransportConfig transportConfig = new MqttTransportConfig(
            new EntityId("iotCoreId", "deviceName"),
            URI.create(String.format("tcp://%s.iot.gz.baidubce.com:1883", "iotCoreId")),
            null,
            "username",
            "password".toCharArray(),
            1000,
            1000,
            1000,
            1000);

    private int maxInflightMessageNum = 100;

    @Test
    public void testConnectSuccess() {
        AtomicReference<IMqttActionListener> reference = new AtomicReference<>();
        AtomicBoolean success = new AtomicBoolean(false);

        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {
            @Mock
            IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback)
                    throws MqttException, MqttSecurityException {
                reference.set(callback);
                return new MockedToken();
            }
        };

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());

        wrapper.connect()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        success.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        success.set(false);
                    }
                });

        reference.get().onSuccess(new MockedToken());
        Assert.assertTrue(success.get());
    }

    @Test
    public void testConnectFailure() throws MqttException {
        AtomicReference<IMqttActionListener> reference = new AtomicReference<>();
        AtomicBoolean failure = new AtomicBoolean(false);

        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {
            @Mock
            IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback)
                    throws MqttException, MqttSecurityException {
                reference.set(callback);
                return new MockedToken();
            }
        };

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());

        wrapper.connect()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                    }
                });

        reference.get().onFailure(new MockedToken(), new RuntimeException());
        Assert.assertTrue(failure.get());
    }

    @Test
    public void testSubscribeSuccess() throws Exception {
        AtomicReference<IMqttActionListener> actionListenerReference = new AtomicReference<>();
        AtomicReference<IMqttMessageListener> mqttMessageListenerReference = new AtomicReference<>();

        AtomicBoolean success = new AtomicBoolean(false);

        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {
            @Mock
            public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback,
                                        IMqttMessageListener messageListener) throws MqttException {
                actionListenerReference.set(callback);
                mqttMessageListenerReference.set(messageListener);
                return new MockedToken();
            }
        };

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());

        wrapper.subscribe(new Topic("test"))
                .subscribe(new DisposableSingleObserver<Observable<UserMessage>>() {
                    @Override
                    public void onSuccess(@NonNull Observable<UserMessage> userMessageObservable) {
                        userMessageObservable.subscribe(new Observer<UserMessage>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onNext(@NonNull UserMessage userMessage) {
                                success.set(true);
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

        actionListenerReference.get().onSuccess(new MockedToken());
        mqttMessageListenerReference.get().messageArrived("$iot/test/shadow/get/reply",
                new MqttMessage(GetAvatarReply.newBuilder().setReqId("123").build().toByteArray()));

        Assert.assertTrue(success.get());
    }

    @Test
    public void testSubscribeFailure() throws Exception {
        AtomicReference<IMqttActionListener> actionListenerReference = new AtomicReference<>();
        AtomicReference<IMqttMessageListener> mqttMessageListenerReference = new AtomicReference<>();

        AtomicBoolean failure = new AtomicBoolean(false);

        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {
            @Mock
            public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback,
                                        IMqttMessageListener messageListener) throws MqttException {
                actionListenerReference.set(callback);
                mqttMessageListenerReference.set(messageListener);
                return new MockedToken();
            }
        };

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());

        wrapper.subscribe(new Topic("test"))
                .subscribe(new DisposableSingleObserver<Observable<UserMessage>>() {
                    @Override
                    public void onSuccess(@NonNull Observable<UserMessage> userMessageObservable) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                    }
                });

        actionListenerReference.get().onFailure(new MockedToken(), new RuntimeException());
        Assert.assertTrue(failure.get());
    }

    @Test
    public void testSubscribeTimeout() throws Exception {
        AtomicBoolean failure = new AtomicBoolean(false);

        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {
            @Mock
            public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback,
                                        IMqttMessageListener messageListener) throws MqttException {
                return new MockedToken();
            }

            @Mock
            public boolean isConnected() {
                return true;
            }
        };

        MqttTransportConfig transportConfig = new MqttTransportConfig(
                new EntityId("iotCoreId", "deviceName"), URI.create(String.format("tcp://%s.iot.gz.baidubce.com:1883", "iotCoreId")),
                null, "username", "password".toCharArray(), 1000, 100, 1000, 1000);

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());
        wrapper.subscribe(new Topic("test"))
                .subscribe(new DisposableSingleObserver<Observable<UserMessage>>() {
                    @Override
                    public void onSuccess(@NonNull Observable<UserMessage> userMessageObservable) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                        Assert.assertTrue(e instanceof TimeoutException);
                    }
                });

        Thread.sleep(2 * 1000);
        Assert.assertTrue(failure.get());
    }

    @Test
    public void testPublishSuccess() {
        AtomicReference<IMqttActionListener> reference = new AtomicReference<>();
        AtomicBoolean success = new AtomicBoolean(false);

        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {
            @Mock
            public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext, IMqttActionListener callback)
                    throws MqttException, MqttPersistenceException {
                reference.set(callback);
                return null;
            }
        };

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());
        wrapper.publish(new Topic("test"), UserMessage.genGetMessage(GetAvatarRequest.newBuilder().build()))
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        success.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
        reference.get().onSuccess(new MockedToken());
        Assert.assertTrue(success.get());
    }

    @Test
    public void testPublishError() {
        AtomicReference<IMqttActionListener> reference = new AtomicReference<>();
        AtomicBoolean failure = new AtomicBoolean(false);

        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {
            @Mock
            public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext, IMqttActionListener callback)
                    throws MqttException, MqttPersistenceException {
                reference.set(callback);
                return null;
            }
        };

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());
        wrapper.publish(new Topic("test"), UserMessage.genGetMessage(GetAvatarRequest.newBuilder().build()))
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                    }
                });
        reference.get().onFailure(new MockedToken(), new RuntimeException());
        Assert.assertTrue(failure.get());
    }

    @Test
    public void testDisconnectSuccess() {
        AtomicReference<IMqttActionListener> reference = new AtomicReference<>();
        AtomicBoolean success = new AtomicBoolean(false);

        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {
            @Mock
            public IMqttToken disconnect(long quiesceTimeout, Object userContext, IMqttActionListener callback) throws MqttException {
                reference.set(callback);
                return null;
            }
        };

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());
        wrapper.disconnect()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        success.set(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
        reference.get().onSuccess(new MockedToken());
        Assert.assertTrue(success.get());
    }

    @Test
    public void testDisconnectFailure() {
        AtomicReference<IMqttActionListener> reference = new AtomicReference<>();
        AtomicBoolean failure = new AtomicBoolean(false);

        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {
            @Mock
            public IMqttToken disconnect(long quiesceTimeout, Object userContext, IMqttActionListener callback) throws MqttException {
                reference.set(callback);
                return null;
            }
        };

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());
        wrapper.disconnect()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        failure.set(true);
                    }
                });
        reference.get().onFailure(new MockedToken(), new RuntimeException());
        Assert.assertTrue(failure.get());
    }

    @Test
    public void testDisconnectForcibly(@Mocked IMqttAsyncClient mqttAsyncClient) throws MqttException {
        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, mqttAsyncClient);

        wrapper.disconnectForcibly();

        new Verifications() {{
           mqttAsyncClient.disconnectForcibly();
           times = 1;
        }};
    }

    @Test
    public void testSetCallback(@Mocked IMqttAsyncClient mqttAsyncClient) {
        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, mqttAsyncClient);

        wrapper.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        new Verifications() {{
            mqttAsyncClient.setCallback((MqttCallback) any);
            times = 1;
        }};
    }

    @Test
    public void testClose() throws Exception {
        new MockUp<MockedMqttAsyncClient>(MockedMqttAsyncClient.class) {

        };

        ReactiveStreamsMqttClientWrapper wrapper = new ReactiveStreamsMqttClientWrapper(
                transportConfig, maxInflightMessageNum, new MockedMqttAsyncClient());

        wrapper.close();
    }

    private static class MockedMqttAsyncClient implements IMqttAsyncClient {

        @Override
        public IMqttToken connect() throws MqttException, MqttSecurityException {
            return null;
        }

        @Override
        public IMqttToken connect(MqttConnectOptions options) throws MqttException, MqttSecurityException {
            return null;
        }

        @Override
        public IMqttToken connect(Object userContext, IMqttActionListener callback) throws MqttException, MqttSecurityException {
            return null;
        }

        @Override
        public IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback) throws MqttException, MqttSecurityException {
            return null;
        }

        @Override
        public IMqttToken disconnect() throws MqttException {
            return null;
        }

        @Override
        public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken disconnect(Object userContext, IMqttActionListener callback) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken disconnect(long quiesceTimeout, Object userContext, IMqttActionListener callback) throws MqttException {
            return null;
        }

        @Override
        public void disconnectForcibly() throws MqttException {

        }

        @Override
        public void disconnectForcibly(long disconnectTimeout) throws MqttException {

        }

        @Override
        public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {

        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public String getClientId() {
            return null;
        }

        @Override
        public String getServerURI() {
            return null;
        }

        @Override
        public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained) throws MqttException, MqttPersistenceException {
            return null;
        }

        @Override
        public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained, Object userContext, IMqttActionListener callback) throws MqttException, MqttPersistenceException {
            return null;
        }

        @Override
        public IMqttDeliveryToken publish(String topic, MqttMessage message) throws MqttException, MqttPersistenceException {
            return null;
        }

        @Override
        public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext, IMqttActionListener callback) throws MqttException, MqttPersistenceException {
            return null;
        }

        @Override
        public IMqttToken subscribe(String topicFilter, int qos) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken subscribe(String[] topicFilters, int[] qos) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback, IMqttMessageListener messageListener) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken subscribe(String topicFilter, int qos, IMqttMessageListener messageListener) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken subscribe(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback, IMqttMessageListener[] messageListeners) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken unsubscribe(String topicFilter) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken unsubscribe(String[] topicFilters) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken unsubscribe(String topicFilter, Object userContext, IMqttActionListener callback) throws MqttException {
            return null;
        }

        @Override
        public IMqttToken unsubscribe(String[] topicFilters, Object userContext, IMqttActionListener callback) throws MqttException {
            return null;
        }

        @Override
        public boolean removeMessage(IMqttDeliveryToken token) throws MqttException {
            return false;
        }

        @Override
        public void setCallback(MqttCallback callback) {

        }

        @Override
        public IMqttDeliveryToken[] getPendingDeliveryTokens() {
            return new IMqttDeliveryToken[0];
        }

        @Override
        public void setManualAcks(boolean manualAcks) {

        }

        @Override
        public void reconnect() throws MqttException {

        }

        @Override
        public void messageArrivedComplete(int messageId, int qos) throws MqttException {

        }

        @Override
        public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {

        }

        @Override
        public int getBufferedMessageCount() {
            return 0;
        }

        @Override
        public MqttMessage getBufferedMessage(int bufferIndex) {
            return null;
        }

        @Override
        public void deleteBufferedMessage(int bufferIndex) {

        }

        @Override
        public int getInFlightMessageCount() {
            return 0;
        }

        @Override
        public void close() throws MqttException {

        }
    }

    private static class MockedToken implements IMqttToken {

        @Override
        public void waitForCompletion() throws MqttException {

        }

        @Override
        public void waitForCompletion(long timeout) throws MqttException {

        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public MqttException getException() {
            return null;
        }

        @Override
        public void setActionCallback(IMqttActionListener listener) {

        }

        @Override
        public IMqttActionListener getActionCallback() {
            return null;
        }

        @Override
        public IMqttAsyncClient getClient() {
            return null;
        }

        @Override
        public String[] getTopics() {
            return new String[0];
        }

        @Override
        public void setUserContext(Object userContext) {

        }

        @Override
        public Object getUserContext() {
            return null;
        }

        @Override
        public int getMessageId() {
            return 0;
        }

        @Override
        public int[] getGrantedQos() {
            return new int[] {1};
        }

        @Override
        public boolean getSessionPresent() {
            return false;
        }

        @Override
        public MqttWireMessage getResponse() {
            return null;
        }
    }

}
