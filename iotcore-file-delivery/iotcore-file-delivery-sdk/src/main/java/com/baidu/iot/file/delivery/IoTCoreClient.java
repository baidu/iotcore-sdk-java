// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.TimerPingSender;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import com.baidu.iot.file.delivery.exception.IoTCoreTimeoutException;
import com.baidu.iot.file.delivery.exception.PeerThrottleException;
import com.baidu.iot.file.delivery.message.DownloadDataMessage;
import com.baidu.iot.file.delivery.message.DownloadHeader;
import com.baidu.iot.file.delivery.message.DownloadRequest;
import com.baidu.iot.file.delivery.message.FileMetaData;
import com.baidu.iot.file.delivery.message.UploadRequest;
import com.baidu.iot.file.delivery.result.DownloadResult;
import com.baidu.iot.file.delivery.result.UploadResult;
import com.baidu.iot.file.delivery.task.DownloadReadTask;
import com.baidu.iot.file.delivery.task.DownloadResponseTask;
import com.baidu.iot.file.delivery.task.DownloadWriteTask;
import com.baidu.iot.file.delivery.task.UploadReadTask;
import com.baidu.iot.file.delivery.task.UploadResponseTask;
import com.baidu.iot.file.delivery.task.UploadWriteTask;
import com.baidu.iot.file.delivery.utils.DownloadTopicHelper;
import com.baidu.iot.file.delivery.utils.IoTCoreFeedback;
import com.baidu.iot.file.delivery.utils.TaskMessage;
import com.baidu.iot.file.delivery.utils.UploadTopicHelper;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

/**
 * This is the paho SDK based class for communication with remote IoTCore service,which hides some
 * details about MQTT protocol. It implements time-out and retry with IoTCore.
 */
@Slf4j
public class IoTCoreClient {

    enum TaskType {
        DOWNLOAD,
        UPLOAD
    }

    private String clientId;
    private MqttAsyncClient client;
    private String filesHolderDir;
    private int readChunkSize;
    private long messageSendInterval;
    private long waitForCompletionTime;
    private int maxRetryTimes;
    private Set<String> inflightTaskSet;
    private AtomicInteger inflightTaskCnt;
    private int maxInflightTasks;
    private final ScheduledExecutorService scheduler;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final BehaviorSubject<TaskMessage> taskSignal = BehaviorSubject.create();
    private final BehaviorSubject<IoTCoreFeedback> feedbackSignal = BehaviorSubject.create();

    public IoTCoreClient(String endpoint,
                         String username,
                         String password,
                         String clientId,
                         String filesHolderDir,
                         int readChunkSize,
                         long messageSendInterval,
                         long waitForCompletionTime,
                         int maxRetryTimes,
                         ScheduledExecutorService scheduler,
                         int maxInflightTasks) throws MqttException {
        this.clientId = clientId;
        this.filesHolderDir = filesHolderDir;
        this.readChunkSize = readChunkSize;
        this.messageSendInterval = messageSendInterval;
        this.waitForCompletionTime = waitForCompletionTime;
        this.maxRetryTimes = maxRetryTimes;
        this.inflightTaskSet = new HashSet<>();
        this.inflightTaskCnt = new AtomicInteger();
        this.scheduler = scheduler;
        this.maxInflightTasks = maxInflightTasks;
        client = new MqttAsyncClient(endpoint,
                clientId,
                new MqttDefaultFilePersistence(),
                new TimerPingSender(),
                scheduler);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setMaxInflight(100);
        client.connect(options).waitForCompletion(waitForCompletionTime);
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.error("lost connection: ", cause);
                try {
                    close();
                } catch (MqttException e) {
                    log.error("failed to close client: {}", clientId);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
        log.trace("connect to: {} ok", endpoint);
        client.setManualAcks(false);
        String downloadReqTopicFilter = DownloadTopicHelper.getDownloadReqTopicFilter(clientId);
        String uploadListenerTopicFilter = UploadTopicHelper.getUploadListenerTopicFilter(clientId);
        client.subscribe(downloadReqTopicFilter, 1, (topic, message) -> {
            DownloadRequest request = DownloadRequest.parseFrom(message.getPayload());
            String taskId = generateTaskId(request.getClientId(), request.getSessionId(), TaskType.DOWNLOAD);
            if (!inflightTaskSet.contains(taskId)) {
                inflightTaskSet.add(taskId);
                DownloadResponseTask responseTask = new DownloadResponseTask(request.getClientId(),
                        request.getSessionId(), taskId, filesHolderDir,
                        request.getFileName(), readChunkSize, taskSignal, feedbackSignal);
                responseTask.doProcess();
            }
        }).waitForCompletion(waitForCompletionTime);
        client.subscribe(uploadListenerTopicFilter, 1, (topic, message) -> {
            UploadRequest uploadRequest = UploadRequest.parseFrom(message.getPayload());
            long sessionId = uploadRequest.getSessionId();
            String remotePeer = uploadRequest.getClientId();
            String taskId = generateTaskId(remotePeer, sessionId, TaskType.UPLOAD);
            FileMetaData fileMetaData = uploadRequest.getFileMetaData();
            if (!inflightTaskSet.contains(taskId)) {
                new UploadResponseTask(uploadRequest.getFileName(), remotePeer, sessionId,
                        taskId, fileMetaData, taskSignal, feedbackSignal).doProcess();
                inflightTaskSet.add(taskId);
            }
        }).waitForCompletion(waitForCompletionTime);
        log.trace("subscribe topicFilter: {} and {}", downloadReqTopicFilter, uploadListenerTopicFilter);
        disposables.add(taskSignal.subscribe(taskMessage -> {
            TaskMessage.MessageType messageType = taskMessage.getMessageType();
            switch (messageType) {
                case PUB_DATA:
                    MqttMessage message = new MqttMessage();
                    message.setPayload(taskMessage.getPayload());
                    message.setQos(1);
                    try {
                        client.publish(taskMessage.getTopic(), message).waitForCompletion(waitForCompletionTime);
                        feedbackSignal.onNext(new IoTCoreFeedback(IoTCoreFeedback.Feedback.PUB_OK,
                                taskMessage.getTaskId(), taskMessage.getSeq()));
                    }catch (MqttException e) {
                        log.error("pub taskId: {} message failed", taskMessage.getTopic());
                        feedbackSignal.onNext(new IoTCoreFeedback(IoTCoreFeedback.Feedback.PUB_FAILED,
                                taskMessage.getTaskId(), taskMessage.getSeq()));
                    }
                    break;
                case UNSUB:
                    try {
                        client.unsubscribe(taskMessage.getTopic()).waitForCompletion(waitForCompletionTime);
                    }catch (MqttException exception) {
                        log.error("failed to unsub topicFilter: {}", taskMessage.getTopic(), exception);
                    }
                    break;
                case PUB_UPLOAD_ACK:
                    feedbackSignal.onNext(new IoTCoreFeedback(IoTCoreFeedback.Feedback.PUB_OK,
                            taskMessage.getTaskId(), 0L));
                    break;
                case PUB_DOWNLOAD_HEADER:
                    try {
                        MqttMessage downloadHeader = new MqttMessage();
                        downloadHeader.setPayload(taskMessage.getPayload());
                        downloadHeader.setQos(1);
                        client.publish(taskMessage.getTopic(), downloadHeader).waitForCompletion(waitForCompletionTime);
                        feedbackSignal.onNext(new IoTCoreFeedback(IoTCoreFeedback.Feedback.PUB_HEADER_OK,
                                taskMessage.getTaskId(), 0L));
                    }catch (MqttException exception) {
                        log.error("failed to pub download header: ", exception);
                        feedbackSignal.onNext(new IoTCoreFeedback(IoTCoreFeedback.Feedback.PUB_FAILED,
                                taskMessage.getTaskId(), 0L));
                    }
                    break;
                case TRIGGER_UPLOAD_RECEIVE:
                    startToReceiveDataFromUploader(taskMessage.getTopic(),
                            FileMetaData.parseFrom(taskMessage.getPayload()),
                            taskMessage.getTaskId(), taskMessage.getFileName());
                    break;
                case TRIGGER_DOWNLOAD_SEND:
                    startToSendDataToDownloader(taskMessage.getTopic(),
                            taskMessage.getTaskId(), taskMessage.getFileName());
                    break;
                case FINISH_TRANSFER:
                    inflightTaskSet.remove(taskMessage.getTaskId());
                    break;
                default:
                    log.warn("Unknown messageType: {}", messageType);
                    break;
            }
        }));
    }

    private void startToReceiveDataFromUploader(String receiveMgsTopicFilter,
                                                FileMetaData fileMetaData,
                                                String taskId,
                                                String fileName) throws MqttException, FileNotFoundException {
        final UploadWriteTask[] task = new UploadWriteTask[1];
        task[0] = new UploadWriteTask(fileMetaData, filesHolderDir + "/" + fileName,
                taskId, taskSignal, receiveMgsTopicFilter,
                2 * maxRetryTimes * waitForCompletionTime, scheduler);
        client.subscribe(receiveMgsTopicFilter, 1, ((topic, message) -> {
            log.trace("receive an msg from uploader, topic: {}", receiveMgsTopicFilter);
            if (receiveMgsTopicFilter.equals(topic)) {
                task[0].doProcess(message.getPayload());
            }
        })).waitForCompletion(waitForCompletionTime);
    }

    private void startToSendDataToDownloader(String sendTopic,
                                             String taskId,
                                             String fileName) throws FileNotFoundException {
        DownloadReadTask readTask = new DownloadReadTask(fileName, filesHolderDir, sendTopic,
                taskId, readChunkSize, taskSignal,
                feedbackSignal, scheduler, messageSendInterval);
        readTask.doProcess();
    }

    public CompletableFuture<DownloadResult> downloadFile(String fileName, String remotePeer, String dir) {
        CompletableFuture<DownloadResult> future = new CompletableFuture<>();
        future.whenComplete((v , e) -> {
            if (e != null) {
                if (! (e instanceof PeerThrottleException)) {
                    inflightTaskCnt.decrementAndGet();
                }
            }else {
                inflightTaskCnt.decrementAndGet();
            }
        });
        if (checkThrottle()) {
            future.completeExceptionally(new PeerThrottleException("throttle, too many inflight tasks"));
            inflightTaskCnt.decrementAndGet();
            return future;
        }
        long sessionId = System.nanoTime();
        String receiveTopicFilter = generateDownloadReceiveTopicFilter(sessionId);
        try {
            startToDownloadRequestRemotePeer(fileName, dir, remotePeer, receiveTopicFilter, sessionId, future);
        }catch (Exception e) {
            log.error("failed to communicate with IoTCore: , stop receive message", e);
            try {
                client.unsubscribe(receiveTopicFilter);
            }catch (MqttException mqttException) {
                log.error("failed to unsubscribe topicFilter: {}", receiveTopicFilter, mqttException);
            }
            future.completeExceptionally(e);
        }
        return future;
    }

    private void startToDownloadRequestRemotePeer(String fileName,
                                                  String dir,
                                                  String remotePeer,
                                                  String receiveTopicFilter,
                                                  long sessionId,
                                                  CompletableFuture<DownloadResult> future) throws
            IoTCoreTimeoutException, MqttException {
        final DownloadWriteTask[] task = new DownloadWriteTask[1];
        task[0] = new DownloadWriteTask(
                taskSignal,
                receiveTopicFilter,
                scheduler,
                2 * maxRetryTimes * waitForCompletionTime,
                future);
        client.subscribe(receiveTopicFilter, 1, ((topic, message) -> {
            if (task[0].getFileMetaData() == null) {
                DownloadHeader header = DownloadDataMessage.parseFrom(message.getPayload()).getHeader();
                if (header.getReply() == DownloadHeader.Reply.REJECTED) {
                    task[0].close(DownloadResult.FAILED);
                }else {
                    task[0].setFileMetaDataAndWriteBuffer(header.getFileMetaData(),
                            dir + "/" + fileName, header.getFileMetaData().getFileSize());
                }
            }else {
                task[0].doProcess(message.getPayload());
            }
        })).waitForCompletion(waitForCompletionTime);
        DownloadRequest request = DownloadRequest.newBuilder()
                .setClientId(clientId)
                .setFileName(fileName)
                .setSessionId(sessionId)
                .build();
        MqttMessage message = new MqttMessage(request.toByteArray());
        message.setQos(1);
        int resendTimes = 0;
        for (; resendTimes < maxRetryTimes; resendTimes++) {
            try {
                client.publish(DownloadTopicHelper.getDownloadReqTopicFilter(remotePeer), message)
                        .waitForCompletion(waitForCompletionTime);
                break;
            }catch (MqttException mqttException) {
                log.error("failed to pub msg, clientId: {}, msg: {}", clientId, message, mqttException);
            }
        }
        if (resendTimes == maxRetryTimes) {
            throw new IoTCoreTimeoutException("publish exceed maxRetryTimes");
        }
    }

    private String generateDownloadReceiveTopicFilter(long sessionId) {
        return DownloadTopicHelper.getDownloadMsgRecvTopicFilter(clientId, sessionId);
    }

    public CompletableFuture<UploadResult> uploadFile(File file, String remotePeer) {
        CompletableFuture<UploadResult> future = new CompletableFuture<>();
        future.whenComplete((v, e) -> {
            if (e != null) {
                if (! (e instanceof PeerThrottleException)) {
                    inflightTaskCnt.decrementAndGet();
                }
            }else {
                inflightTaskCnt.decrementAndGet();
            }
        });
        if (checkThrottle()) {
            future.completeExceptionally(
                    new PeerThrottleException("throttle, too many inflight tasks, try again later"));
            inflightTaskCnt.decrementAndGet();
            return future;
        }
        try {
            long sessionId = System.nanoTime();
            startToUploadRequestRemotePeer(file, remotePeer, sessionId, future);
        }catch (Exception exception) {
            log.error("failed to start requesting remote peer: ", exception);
            future.completeExceptionally(exception);
        }
        return future;
    }

    private void startToUploadRequestRemotePeer(File file,
                                                String remotePeer,
                                                long sessionId,
                                                CompletableFuture<UploadResult> future)
            throws FileNotFoundException {
        String uploadRequestTopic = UploadTopicHelper.getUploadListenerTopicFilter(remotePeer);
        UploadReadTask readTask = new UploadReadTask(file.getAbsolutePath(),
                getUploadReceiveTopicFilter(clientId, sessionId), readChunkSize,
                future, taskSignal, feedbackSignal,
                2 * maxRetryTimes * waitForCompletionTime, messageSendInterval, scheduler);
        UploadRequest uploadRequest = UploadRequest.newBuilder()
                .setClientId(clientId)
                .setFileName(file.getName())
                .setSessionId(sessionId)
                .setFileMetaData(FileMetaData.newBuilder()
                        .setFileSize(file.length())
                        .setChunkSize(readChunkSize)
                        .build())
                .build();
        MqttMessage message = new MqttMessage(uploadRequest.toByteArray());
        message.setQos(1);
        int resendTimes = 0;
        for (; resendTimes < maxRetryTimes; resendTimes++) {
            try {
                client.publish(uploadRequestTopic, message).waitForCompletion(waitForCompletionTime);
                break;
            }catch (MqttException mqttException) {
                log.error("failed to pub upload request: {}, resendTimes: {}", uploadRequest, resendTimes);
            }
        }
        if (resendTimes == maxRetryTimes) {
            throw new IoTCoreTimeoutException("publish exceed maxResendTimes");
        }else {
            scheduler.schedule(() -> readTask.doProcess(), messageSendInterval, TimeUnit.MILLISECONDS);
        }
    }

    private String getUploadReceiveTopicFilter(String remotePeer, long sessionId) {
        return UploadTopicHelper.getUploadMsgRecvTopicFilter(remotePeer, sessionId);
    }

    private String generateTaskId(String remotePeer, long sessionId, TaskType taskType) {
        return taskType.name() + "/" + remotePeer + "/" + sessionId;
    }

    private boolean checkThrottle() {
        return inflightTaskCnt.incrementAndGet() > maxInflightTasks;
    }

    public void close() throws MqttException {
        taskSignal.onComplete();
        disposables.dispose();
        client.disconnectForcibly();
        client.close();
        scheduler.shutdown();
        log.info("IoTCore client: {} closed", clientId);
    }

}
