package com.baidu.iot.file.delivery;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.TimerPingSender;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import com.baidu.iot.file.delivery.exception.IoTCoreTimeoutException;
import com.baidu.iot.file.delivery.message.DownloadDataMessage;
import com.baidu.iot.file.delivery.message.DownloadHeader;
import com.baidu.iot.file.delivery.message.DownloadRequest;
import com.baidu.iot.file.delivery.message.FileMetaData;
import com.baidu.iot.file.delivery.message.UploadAck;
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
                         ScheduledExecutorService scheduler) throws MqttException {
        this.clientId = clientId;
        this.filesHolderDir = filesHolderDir;
        this.readChunkSize = readChunkSize;
        this.messageSendInterval = messageSendInterval;
        this.waitForCompletionTime = waitForCompletionTime;
        this.maxRetryTimes = maxRetryTimes;
        this.inflightTaskSet = new HashSet<>();
        this.scheduler = scheduler;
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
        client.connect(options).waitForCompletion(5000);
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
        } ).waitForCompletion(waitForCompletionTime);
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
                    try {
                        MqttMessage uploadAckMsg = new MqttMessage();
                        uploadAckMsg.setPayload(taskMessage.getPayload());
                        uploadAckMsg.setQos(1);
                        client.publish(taskMessage.getTopic(), uploadAckMsg).waitForCompletion(waitForCompletionTime);
                        feedbackSignal.onNext(new IoTCoreFeedback(IoTCoreFeedback.Feedback.PUB_OK,
                                taskMessage.getTaskId(), 0L));
                    }catch (MqttException exception) {
                        log.error("failed to pub upload ack: ", exception);
                        feedbackSignal.onNext(new IoTCoreFeedback(IoTCoreFeedback.Feedback.PUB_FAILED,
                                taskMessage.getTaskId(), 0L));
                    }
                    break;
                case PUB_DOWNLOAD_HEADER:
                    try {
                        MqttMessage downloadHeader = new MqttMessage();
                        downloadHeader.setPayload(taskMessage.getPayload());
                        downloadHeader.setQos(1);
                        client.publish(taskMessage.getTopic(), downloadHeader).waitForCompletion(waitForCompletionTime);
                        feedbackSignal.onNext(new IoTCoreFeedback(IoTCoreFeedback.Feedback.PUB_OK,
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
                    startToSendDataToDownloader(taskMessage.getTopic(), taskMessage.getTaskId(), taskMessage.getFileName());
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
            if (receiveMgsTopicFilter.equals(topic)) {
                task[0].doProcess(message.getPayload());
            }
        }));
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
        long sessionId = System.nanoTime();
        String receiveTopicFilter = generateDownloadReceiveTopicFilter(sessionId);
        CompletableFuture<DownloadResult> future = new CompletableFuture<>();
        try {
            startToDownloadRequestRemotePeer(fileName, remotePeer, receiveTopicFilter, sessionId, future);
        }catch (Exception e) {
            log.error("failed to communicate with IoTCore: , stop receive message", e);
            client.unsubscribe(receiveTopicFilter);
            future.completeExceptionally(e);
        }finally {
            return future;
        }
    }

    private void startToDownloadRequestRemotePeer(String fileName,
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
                    future.complete(DownloadResult.FAILED);
                    client.unsubscribe(receiveTopicFilter);
                    return;
                }else {
                    task[0].setFileMetaDataAndWriteBuffer(header.getFileMetaData(),
                            filesHolderDir + "/" + fileName);
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
        try {
            String receiveAckTopicFilter = UploadTopicHelper.getUploadAckTopicFilter(clientId);
            startToUploadRequestRemotePeer(file, remotePeer, receiveAckTopicFilter, System.nanoTime(), future);
        }catch (Exception exception) {
            log.error("failed to start requesting remote peer: ", exception);
            future.completeExceptionally(exception);
        }finally {
            return future;
        }
    }

    private void startToUploadRequestRemotePeer(File file,
                                                String remotePeer,
                                                String receiveAckTopicFilter,
                                                long sessionId,
                                                CompletableFuture<UploadResult> future)
            throws FileNotFoundException, MqttException {
        String uploadRequestTopic = UploadTopicHelper.getUploadListenerTopicFilter(remotePeer);
        UploadReadTask readTask = new UploadReadTask(file.getName(), filesHolderDir,
                getUploadReceiveTopicFilter(clientId, sessionId), readChunkSize,
                future, taskSignal, feedbackSignal,
                2 * maxRetryTimes * waitForCompletionTime, messageSendInterval, scheduler);
        client.subscribe(receiveAckTopicFilter, 1, ((topic, message) -> {
            UploadAck uploadAck = UploadAck.parseFrom(message.getPayload());
            if (uploadAck.getReply() == UploadAck.Reply.REJECTED) {
                log.warn("uploader: {} request is rejected by remotePeer, receiverAckTopicFilter: {}",
                        clientId, receiveAckTopicFilter);
                client.unsubscribe(receiveAckTopicFilter);
                future.complete(UploadResult.FAILED);
                return;
            }
            readTask.doProcess();
        })).waitForCompletion(waitForCompletionTime);
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
        }
    }

    private String getUploadReceiveTopicFilter(String remotePeer, long sessionId) {
        return UploadTopicHelper.getUploadMsgRecvTopicFilter(remotePeer, sessionId);
    }

    private String generateTaskId(String remotePeer, long sessionId, TaskType taskType) {
        return taskType.name() + "/" + remotePeer + "/" + sessionId;
    }

    public void close() throws MqttException {
        log.info("closing IoTCore client");
        taskSignal.onComplete();
        disposables.dispose();
        client.disconnectForcibly();
        scheduler.shutdown();
        client.close();
    }

}
