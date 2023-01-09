package com.baidu.iot.file.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

import com.baidu.iot.file.delivery.exception.IoTCoreTimeoutException;
import com.baidu.iot.file.delivery.exception.PeerThrottleException;
import com.baidu.iot.file.delivery.message.Download;
import com.baidu.iot.file.delivery.message.DownloadDataMessage;
import com.baidu.iot.file.delivery.message.DownloadHeader;
import com.baidu.iot.file.delivery.message.FileMetaData;
import com.baidu.iot.file.delivery.message.UploadDataMessage;
import com.baidu.iot.file.delivery.result.DownloadResult;
import com.baidu.iot.file.delivery.result.UploadResult;
import com.baidu.iot.file.delivery.task.DownloadWriteTask;
import com.baidu.iot.file.delivery.task.UploadWriteTask;
import com.baidu.iot.file.delivery.utils.TaskMessage;
import com.google.protobuf.ByteString;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;

public class IoTCoreClientTest {
    private File file = new File("src/test/resources/file.txt");
    private String remotePeer = "remotePeer";
    @Tested
    private IoTCoreClient client;
    @Injectable
    private String clientId = "clientId";
    @Mocked
    private MqttAsyncClient mqttAsyncClient;
    @Injectable
    private String filesHolderDir = "";
    @Injectable
    private int readChunkSize = 10;
    @Injectable
    private long messageSendInterval = 1L;
    @Injectable
    private long waitForCompletionTime = 100L;
    @Injectable
    private int maxRetryTimes = 5;
    @Injectable
    private AtomicInteger inflightTaskCnt = new AtomicInteger();
    @Injectable
    private int maxInflightTasks = 3;
    @Injectable
    private String endpoint = "endpoint";
    @Injectable
    private String username = "username";
    @Injectable
    private String password = "password";
    @Injectable
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Test
    public void testUpload() throws MqttException {
        new Expectations() {{
            mqttAsyncClient.publish(anyString, (MqttMessage) any).waitForCompletion(anyLong);
            result = null;
        }};
        UploadResult result = client.uploadFile(file, remotePeer).join();
        assertEquals(result, UploadResult.OK);
    }

    @Test
    public void testUploadWithThrottle() throws MqttException {
        new Expectations() {{
            mqttAsyncClient.publish(anyString, (MqttMessage) any).waitForCompletion(anyLong);
            result = null;
        }};
        CompletableFuture[] futures = new CompletableFuture[maxInflightTasks + 1];
        for (int index = 0; index < maxInflightTasks + 1; index++) {
            futures[index] = client.uploadFile(file, remotePeer);
        }

        futures[maxInflightTasks].whenComplete((v, e) -> assertTrue(e instanceof PeerThrottleException));
    }

    @Test
    public void testDownload() throws Exception {
        BehaviorSubject<TaskMessage> taskSignal = BehaviorSubject.create();
        CompletableFuture<DownloadResult> future = new CompletableFuture<>();
        String receiveTopicFilter = "receiveTopicFilter";
        final DownloadWriteTask[] task = new DownloadWriteTask[1];
        FileMetaData fileMetaData = FileMetaData.newBuilder()
                .setFileSize(8 * 1024)
                .setChunkSize(1024)
                .build();
        DownloadHeader headerMsg = DownloadHeader.newBuilder()
                .setFileMetaData(fileMetaData)
                .setReply(DownloadHeader.Reply.ACCEPTED)
                .build();
        DownloadDataMessage downloadDataMessage = DownloadDataMessage.newBuilder()
                .setHeader(headerMsg)
                .build();
        task[0] = new DownloadWriteTask(
                taskSignal,
                receiveTopicFilter,
                scheduler,
                2 * maxRetryTimes * waitForCompletionTime,
                future);

        IMqttMessageListener listener = (topic, message) -> {
            if (task[0].getFileMetaData() == null) {
                DownloadHeader header = DownloadDataMessage.parseFrom(message.getPayload()).getHeader();
                if (header.getReply() == DownloadHeader.Reply.REJECTED) {
                    task[0].close(DownloadResult.FAILED);
                }else {
                    task[0].setFileMetaDataAndWriteBuffer(header.getFileMetaData(),
                            file.getAbsolutePath(), header.getFileMetaData().getFileSize());
                }
            }else {
                task[0].doProcess(message.getPayload());
            }
        };

        MqttMessage firstMsg = new MqttMessage();
        firstMsg.setPayload(downloadDataMessage.toByteArray());
        listener.messageArrived(receiveTopicFilter, firstMsg);

        for (int index = 0; index < (fileMetaData.getFileSize() / fileMetaData.getChunkSize()); index++) {
            DownloadDataMessage dataMessage = DownloadDataMessage.newBuilder()
                    .setDownload(Download.newBuilder()
                            .setSeq(index)
                            .setChunkData(ByteString.copyFrom(new byte[fileMetaData.getChunkSize()]))
                            .build())
                    .build();
            MqttMessage msg = new MqttMessage();
            msg.setPayload(dataMessage.toByteArray());
            listener.messageArrived(receiveTopicFilter,msg);
        }

        assertTrue(future.isDone());
        assertEquals(future.join(), DownloadResult.OK);
    }

    @Test
    public void testDownloadWithTimeout() throws InterruptedException {
        BehaviorSubject<TaskMessage> taskSignal = BehaviorSubject.create();
        CompletableFuture<DownloadResult> future = new CompletableFuture<>();
        String receiveTopicFilter = "receiveTopicFilter";
        final DownloadWriteTask[] task = new DownloadWriteTask[1];
        task[0] = new DownloadWriteTask(
                taskSignal,
                receiveTopicFilter,
                scheduler,
                2 * maxRetryTimes * waitForCompletionTime,
                future);
        Thread.sleep(2 * maxRetryTimes * waitForCompletionTime + 100L);
        future.whenComplete((v, e) -> assertTrue(e instanceof IoTCoreTimeoutException));
    }

    @Test
    public void testDownloadWithThrottle() throws MqttException {
        new Expectations() {{
            mqttAsyncClient.subscribe(anyString, 1, (IMqttMessageListener) any).waitForCompletion(anyLong);
            result = null;

            mqttAsyncClient.publish(anyString, (MqttMessage) any).waitForCompletion(anyLong);
            result = null;
        }};
        CompletableFuture[] futures = new CompletableFuture[maxInflightTasks + 1];
        for (int index = 0; index < maxInflightTasks + 1; index++) {
            CompletableFuture future = client.downloadFile(file.getName(), remotePeer, filesHolderDir);
            futures[index] = future;
        }
        futures[maxInflightTasks].whenComplete((v, e) -> assertTrue(e instanceof PeerThrottleException));
    }

    @Test
    public void testUploadResponder() throws Exception {
        BehaviorSubject<TaskMessage> taskSignal = BehaviorSubject.create();
        String taskId = "taskId";
        FileMetaData fileMetaData = FileMetaData.newBuilder()
                .setFileSize(8 * 1024)
                .setChunkSize(1024)
                .build();
        String receiveMgsTopicFilter = "receiveMgsTopicFilter";
        UploadWriteTask[] task = new UploadWriteTask[1];
        task[0] = new UploadWriteTask(fileMetaData, file.getAbsolutePath(),
                taskId, taskSignal, receiveMgsTopicFilter,
                2 * maxRetryTimes * waitForCompletionTime, scheduler);
        IMqttMessageListener listener = (topic, message) -> {
            if (receiveMgsTopicFilter.equals(topic)) {
                task[0].doProcess(message.getPayload());
            }
        };
        for (int index = 0; index < (fileMetaData.getFileSize() / fileMetaData.getChunkSize()); index++) {
            UploadDataMessage dataMessage = UploadDataMessage.newBuilder()
                    .setChunk(ByteString.copyFrom(new byte[fileMetaData.getChunkSize()]))
                    .setSeq(index)
                    .build();
            MqttMessage message = new MqttMessage();
            message.setPayload(dataMessage.toByteArray());
            listener.messageArrived(receiveMgsTopicFilter, message);
        }

        new Verifications() {{
            taskSignal.onNext(new TaskMessage(receiveMgsTopicFilter, taskId,
                    TaskMessage.MessageType.FINISH_TRANSFER, null, 0L));

            taskSignal.onNext(new TaskMessage(receiveMgsTopicFilter, taskId,
                    TaskMessage.MessageType.FINISH_TRANSFER, null, 0L));
        }};
    }

}
