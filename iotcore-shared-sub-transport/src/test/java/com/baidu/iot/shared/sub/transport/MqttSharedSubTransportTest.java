/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.iot.shared.sub.transport;

import io.reactivex.rxjava3.schedulers.TestScheduler;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Verifications;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.baidu.iot.mqtt.common.MqttConfigFactory;
import com.baidu.iot.shared.sub.transport.disruptor.MessageDisruptorImpl;
import com.baidu.iot.shared.sub.transport.enums.Qos;
import com.baidu.iot.shared.sub.transport.enums.TransportState;
import com.baidu.iot.shared.sub.transport.model.TransportId;

/**
 * Created by mafei01 in 6/7/21 4:08 PM
 */
public class MqttSharedSubTransportTest {

    @Injectable
    private IMqttAsyncClient client1;
    @Injectable
    private IMqttAsyncClient client2;
    @Mocked
    private IMqttDeliveryToken token;

    private MqttTransportConfig config;
    private Set<IMqttAsyncClient> clients;

    private String[] topicFilters = new String[] {"topic-1", "topic-2"};
    private String iotCoreId = "testCore";
    private TransportId transportId = new TransportId(iotCoreId, null, null);
    private String username = "user";
    private char[] password = "pwd".toCharArray();
    private int poolSize = 2;

    private List<IMqttActionListener> listeners_conn_1;
    private List<IMqttActionListener> listeners_conn_2;
    private List<IMqttActionListener> listeners_sub_1;
    private List<IMqttActionListener> listeners_sub_2;

    private AtomicBoolean success;
    private AtomicReference error;

    @Before
    public void setup() throws MqttException {
        clients = new HashSet<IMqttAsyncClient>() {{
            add(client1);
            add(client2);
        }};
        config = MqttTransportConfig.builder()
                .transportId(transportId)
                .clientPoolSize(poolSize)
                .topicFilters(topicFilters)
                .mqttConfig(MqttConfigFactory.genPlainMqttConfig(iotCoreId, username, password))
                .build();
        listeners_conn_1 = new ArrayList<>();
        listeners_conn_2 = new ArrayList<>();
        listeners_sub_1 = new ArrayList<>();
        listeners_sub_2 = new ArrayList<>();
        success = new AtomicBoolean(false);
        error = new AtomicReference();
    }

    @Test
    public void testStartSuccessfully() throws InterruptedException, MqttException {
        mockMqttConnectAndSub();
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, null);
        // conn and sub successfully
        iSharedSubTransport.start().subscribe(() -> success.set(true));
        Thread.sleep(30);
        listeners_conn_1.get(0).onSuccess(token);
        listeners_conn_2.get(0).onSuccess(token);
        Thread.sleep(30);
        new Verifications() {{
            client1.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = poolSize;
            client2.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = poolSize;
        }};
        new Expectations() {{
            token.getGrantedQos();
            result = new int[] {1};
        }};
        for (int i = 0; i < topicFilters.length; i++) {
            listeners_sub_1.get(i).onSuccess(token);
            listeners_sub_2.get(i).onSuccess(token);
        }
        Thread.sleep(30);
        Assert.assertTrue(success.get());
    }

    @Test
    public void testStartTwice() {
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, null);
        // conn and sub successfully
        iSharedSubTransport.start();
        iSharedSubTransport.start().subscribe(() -> {}, e -> error.set(e));
        Assert.assertNotNull(error.get());
    }

    @Test
    public void testTransportStartWithOneClientConnFailed() throws InterruptedException, MqttException {
        new Expectations() {{
            client1.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_1));
            client2.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_2));
            client1.subscribe(anyString, anyInt, any, withCapture(listeners_sub_1));
        }};
        TestScheduler testScheduler = new TestScheduler();
        testScheduler.advanceTimeTo(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, testScheduler);
        iSharedSubTransport.start().subscribe(() -> success.set(true), error::set);
        Thread.sleep(30);
        testScheduler.triggerActions();
        // client1 conn and sub success
        listeners_conn_1.get(0).onSuccess(token);
        new Expectations() {{
            token.getGrantedQos();
            result = new int[] {1};
        }};
        Thread.sleep(30);
        testScheduler.triggerActions();
        for (int i = 0; i < topicFilters.length; i++) {
            listeners_sub_1.get(i).onSuccess(token);
        }
        // client2 conn failed after all retries
        for (int i = 0; i < MqttSharedSubTransport.retryTimes; i++) {
            listeners_conn_2.get(i).onFailure(token, new RuntimeException());
            testScheduler.advanceTimeBy(MqttSharedSubTransport.retryIntervalInMs + 1000, TimeUnit.MILLISECONDS);
        }
        Thread.sleep(30);
        Assert.assertTrue(success.get());
        Assert.assertNull(error.get());
    }

    @Test
    public void testTransportStartWithAllClientConnFailed() throws InterruptedException, MqttException {
        new Expectations() {{
            client1.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_1));
            client2.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_2));
        }};
        TestScheduler testScheduler = new TestScheduler();
        testScheduler.advanceTimeTo(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, testScheduler);
        iSharedSubTransport.start().subscribe(() -> success.set(true), error::set);
        Thread.sleep(30);
        testScheduler.triggerActions();
        // all client conn failed after all retries
        for (int i = 0; i < MqttSharedSubTransport.retryTimes; i++) {
            listeners_conn_1.get(i).onFailure(token, new RuntimeException());
            listeners_conn_2.get(i).onFailure(token, new RuntimeException());
            testScheduler.advanceTimeBy(MqttSharedSubTransport.retryIntervalInMs + 1000, TimeUnit.MILLISECONDS);
        }
        Thread.sleep(30);
        Assert.assertFalse(success.get());
        Assert.assertNotNull(error.get());
    }

    @Test
    public void testTransportStartWithOneClientSubFailed() throws InterruptedException, MqttException {
        mockMqttConnectAndSub();
        TestScheduler testScheduler = new TestScheduler();
        testScheduler.advanceTimeTo(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, testScheduler);
        iSharedSubTransport.start().subscribe(() -> success.set(true), error::set);
        Thread.sleep(30);
        testScheduler.triggerActions();
        listeners_conn_1.get(0).onSuccess(token);
        listeners_conn_2.get(0).onSuccess(token);
        Thread.sleep(30);
        testScheduler.triggerActions();
        // client1 conn and sub success, client2 connect success
        new Verifications() {{
            client1.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length;
            client2.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length;
        }};
        new Expectations() {{
            token.getGrantedQos();
            result = new int[] {1};
        }};
        for (int i = 0; i < topicFilters.length; i++) {
            listeners_sub_1.get(i).onSuccess(token);
            testScheduler.triggerActions();
        }
        Thread.sleep(30);
        // client2 sub failed after all retries
        for (int i = 0; i < MqttSharedSubTransport.retryTimes; i++) {
            listeners_sub_2.get(i * 2).onFailure(token, new RuntimeException());
            testScheduler.advanceTimeBy(MqttSharedSubTransport.retryIntervalInMs + 1000, TimeUnit.MILLISECONDS);
        }
        new Verifications() {{
            client2.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length * (MqttSharedSubTransport.retryTimes - 1);
        }};
        Thread.sleep(30);
        Assert.assertTrue(success.get());
        Assert.assertNull(error.get());
    }

    @Test
    public void testTransportStartWithAllClientSubFailed() throws InterruptedException, MqttException {
        mockMqttConnectAndSub();
        TestScheduler testScheduler = new TestScheduler();
        testScheduler.advanceTimeTo(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, testScheduler);

        iSharedSubTransport.start().subscribe(() -> success.set(true), error::set);
        Thread.sleep(30);
        testScheduler.triggerActions();
        // all client connect success
        listeners_conn_1.get(0).onSuccess(token);
        listeners_conn_2.get(0).onSuccess(token);
        Thread.sleep(30);
        testScheduler.triggerActions();
        // all client sub failed after all retries
        for (int i = 0; i < MqttSharedSubTransport.retryTimes; i++) {
            listeners_sub_1.get(i * 2).onFailure(token, new RuntimeException());
            listeners_sub_2.get(i * 2).onFailure(token, new RuntimeException());
            testScheduler.advanceTimeBy(MqttSharedSubTransport.retryIntervalInMs + 1000, TimeUnit.MILLISECONDS);
        }
        new Verifications() {{
            client1.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length * MqttSharedSubTransport.retryTimes;
            client2.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length * MqttSharedSubTransport.retryTimes;
        }};
        Thread.sleep(30);
        Assert.assertFalse(success.get());
        Assert.assertNotNull(error.get());
    }

    @Test
    public void testTransportRecovery() throws InterruptedException, MqttException {
        clients.clear();
        clients.add(client1);
        List<MqttCallbackExtended> cbs = new ArrayList<>();
        new Expectations() {{
            client1.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_1));
            client1.subscribe(anyString, anyInt, any, withCapture(listeners_sub_1));
            client1.setCallback(withCapture(cbs));
        }};
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, null);
        // conn and sub successfully
        AtomicBoolean success = new AtomicBoolean(false);
        iSharedSubTransport.start().subscribe(() -> success.set(true));
        Thread.sleep(30);
        listeners_conn_1.get(0).onSuccess(token);
        Thread.sleep(30);
        new Verifications() {{
            client1.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length;
        }};
        new Expectations() {{
            token.getGrantedQos();
            result = new int[] {1};
        }};
        for (int i = 0; i < topicFilters.length; i++) {
            listeners_sub_1.get(i).onSuccess(token);
        }
        Thread.sleep(30);
        Assert.assertTrue(success.get());
        // connect lost and reSub
        cbs.get(0).connectionLost(new RuntimeException());
        cbs.get(0).connectComplete(true, "");
        Thread.sleep(30);
        new Verifications() {{
            client1.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length;
        }};
    }

    @Test
    public void testTransportMessageArrived(@Mocked MessageDisruptorImpl disruptor) throws Exception {
        clients.clear();
        clients.add(client1);
        List<MqttCallbackExtended> cbs = new ArrayList<>();
        new Expectations() {{
            client1.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_1));
            client1.subscribe(anyString, anyInt, any, withCapture(listeners_sub_1));
            client1.setCallback(withCapture(cbs));
        }};
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, null);
        // conn and sub successfully
        AtomicBoolean success = new AtomicBoolean(false);
        iSharedSubTransport.start().subscribe(() -> success.set(true));
        Thread.sleep(30);
        listeners_conn_1.get(0).onSuccess(token);
        Thread.sleep(30);
        new Verifications() {{
            client1.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length;
        }};
        new Expectations() {{
            token.getGrantedQos();
            result = new int[] {1};
        }};
        for (int i = 0; i < topicFilters.length; i++) {
            listeners_sub_1.get(i).onSuccess(token);
        }
        Thread.sleep(30);
        Assert.assertTrue(success.get());
        // connect lost and reSub
        MqttMessage message = new MqttMessage(new byte[]{1});
        message.setQos(1);
        cbs.get(0).messageArrived("topic", message);
        Thread.sleep(30);
        new Verifications() {{
            disruptor.publish(transportId.getId(), "topic", Qos.AT_LEAST_ONCE, message.getPayload());
        }};
    }

    @Test
    public void testTransportClose(@Mocked MessageDisruptorImpl disruptor) throws Exception {
        clients.clear();
        clients.add(client1);
        List<MqttCallbackExtended> cbs = new ArrayList<>();
        new Expectations() {{
            client1.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_1));
            client1.subscribe(anyString, anyInt, any, withCapture(listeners_sub_1));
            client1.setCallback(withCapture(cbs));
        }};
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, null);
        AtomicReference<TransportState> state = new AtomicReference<>();
        // conn and sub successfully
        iSharedSubTransport.start().subscribe(() -> success.set(true));
        iSharedSubTransport.transportState().subscribe(state::set);
        Thread.sleep(30);
        listeners_conn_1.get(0).onSuccess(token);
        Thread.sleep(30);
        new Verifications() {{
            client1.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length;
        }};
        new Expectations() {{
            token.getGrantedQos();
            result = new int[] {1};
        }};
        for (int i = 0; i < topicFilters.length; i++) {
            listeners_sub_1.get(i).onSuccess(token);
        }
        Thread.sleep(30);
        Assert.assertTrue(success.get());
        // close transport
        List<IMqttActionListener> listeners_disconn_1 = new ArrayList<>();
        new Expectations() {{
           client1.disconnect(anyLong, any, withCapture(listeners_disconn_1));
        }};
        success = new AtomicBoolean(false);
        iSharedSubTransport.close().subscribe(() -> success.set(true));
        Thread.sleep(30);
        listeners_disconn_1.get(0).onSuccess(token);
        Thread.sleep(30);
        Assert.assertEquals(1, listeners_disconn_1.size());
        Assert.assertTrue(success.get());
        Assert.assertEquals(TransportState.SHUTDOWN, state.get());
        new Verifications() {{
            disruptor.unregister(transportId.getId());
        }};
    }

    @Test
    public void testTransportCloseWithMqttCloseFailed(@Mocked MessageDisruptorImpl disruptor) throws Exception {
        clients.clear();
        clients.add(client1);
        List<MqttCallbackExtended> cbs = new ArrayList<>();
        new Expectations() {{
            client1.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_1));
            client1.subscribe(anyString, anyInt, any, withCapture(listeners_sub_1));
            client1.setCallback(withCapture(cbs));
        }};
        ISharedSubTransport iSharedSubTransport = MqttSharedSubTransport.create(config, clients, null);
        // conn and sub successfully
        iSharedSubTransport.start().subscribe(() -> success.set(true));
        Thread.sleep(30);
        listeners_conn_1.get(0).onSuccess(token);
        Thread.sleep(30);
        new Verifications() {{
            client1.subscribe(anyString, anyInt, any, (IMqttActionListener) any);
            times = topicFilters.length;
        }};
        new Expectations() {{
            token.getGrantedQos();
            result = new int[] {1};
        }};
        for (int i = 0; i < topicFilters.length; i++) {
            listeners_sub_1.get(i).onSuccess(token);
        }
        Thread.sleep(30);
        Assert.assertTrue(success.get());
        // close transport
        List<IMqttActionListener> listeners_disconn_1 = new ArrayList<>();
        new Expectations() {{
            client1.disconnect(anyLong, any, withCapture(listeners_disconn_1));
        }};
        success = new AtomicBoolean(false);
        error.set(null);
        iSharedSubTransport.close().subscribe(() -> success.set(true), e -> error.set(e));
        Thread.sleep(30);
        listeners_disconn_1.get(0).onFailure(token, new RuntimeException());
        Thread.sleep(30);
        Assert.assertEquals(1, listeners_disconn_1.size());
        Assert.assertTrue(success.get());
        new Verifications() {{
           client1.disconnectForcibly();
           times = 1;
           disruptor.unregister(transportId.getId());
        }};
    }

    private void mockMqttConnectAndSub() throws MqttException {
        new Expectations() {{
            client1.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_1));
            client2.connect((MqttConnectOptions) any, any, withCapture(listeners_conn_2));
            client1.subscribe(anyString, anyInt, any, withCapture(listeners_sub_1));
            client2.subscribe(anyString, anyInt, any, withCapture(listeners_sub_2));
        }};
    }
}
