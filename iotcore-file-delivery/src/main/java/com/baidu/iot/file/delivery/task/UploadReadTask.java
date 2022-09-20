package com.baidu.iot.file.delivery.task;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.baidu.iot.file.delivery.exception.IoTCoreTimeoutException;
import com.baidu.iot.file.delivery.message.UploadDataMessage;
import com.baidu.iot.file.delivery.result.UploadResult;
import com.baidu.iot.file.delivery.utils.IoTCoreFeedback;
import com.baidu.iot.file.delivery.utils.TaskMessage;
import com.google.protobuf.ByteString;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UploadReadTask extends AbstractReadTask{

    private CompletableFuture<UploadResult> future;
    private boolean firstChunkArrived = false;
    private ScheduledFuture timeoutFuture;

    public UploadReadTask(String fileName,
                          String dir,
                          String sendTopic,
                          int chunkSize,
                          CompletableFuture<UploadResult> future,
                          BehaviorSubject<TaskMessage> taskSignal,
                          BehaviorSubject<IoTCoreFeedback> feedbackSignal,
                          long timeoutDelay,
                          long messageSendInterval,
                          ScheduledExecutorService scheduler) throws FileNotFoundException {
        super(fileName, dir, chunkSize, taskSignal, feedbackSignal);
        this.sendTopic = sendTopic;
        this.future = future;
        this.timeoutFuture = scheduler.schedule(() -> {
            if (!firstChunkArrived) {
                future.completeExceptionally(new IoTCoreTimeoutException("close task due to late ack"));
                close();
            }
        }, timeoutDelay, TimeUnit.MILLISECONDS);
        disposables.add(feedbackSignal.subscribe(feedback -> {
            if (feedback.getTaskId().equals(taskId)) {
                IoTCoreFeedback.Feedback pubFeedback = feedback.getFeedback();
                if (pubFeedback == IoTCoreFeedback.Feedback.PUB_OK) {
                    unConfirmedTaskMsg = null;
                    scheduledFuture = scheduler.schedule(() -> readAndSend(), messageSendInterval, TimeUnit.MILLISECONDS);
                }else {
                    if (++unConfirmedTaskMsg.resendTimes > 3) {
                        close();
                    }else {
                        taskSignal.onNext(unConfirmedTaskMsg.taskMessage);
                    }
                }
            }
        }));
    }

    @Override
    public void doProcess() {
        firstChunkArrived = true;
        readAndSend();
    }

    private void readAndSend() {
        byte[] chunk = null;
        try {
            chunk = reader.read();
        } catch (IOException e) {
            log.error("read file error: ", e);
            future.completeExceptionally(e);
            close();
        }
        if (chunk == null) {
            endOfFile = true;;
        }else {
            TaskMessage taskMessage = new TaskMessage(sendTopic,
                    taskId,
                    TaskMessage.MessageType.PUB_DATA,
                    UploadDataMessage.newBuilder()
                            .setSeq(seq)
                            .setChunk(ByteString.copyFrom(chunk))
                            .build().toByteArray(),
                    seq);
            unConfirmedTaskMsg = new InFlightTaskMessage(taskMessage, 0);
            taskSignal.onNext(taskMessage);
            seq++;
        }
        if (endOfFile == true) {
            log.trace("close task");
            close();
        }
    }

    @Override
    public void close() {
        super.close();
        timeoutFuture.cancel(true);
        if (!future.isDone()) {
            future.complete(UploadResult.OK);
        }
    }
}
