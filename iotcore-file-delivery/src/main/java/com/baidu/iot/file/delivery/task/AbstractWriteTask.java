package com.baidu.iot.file.delivery.task;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.baidu.iot.file.delivery.message.FileMetaData;
import com.baidu.iot.file.delivery.utils.TaskMessage;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractWriteTask {
    protected FileMetaData fileMetaData;
    protected BehaviorSubject<TaskMessage> taskSignal;
    protected String subscribedTopicFilter;
    protected BufferedOutputStream bufferedOutputStream;
    protected long fileSizeCount;
    protected long lastSeq;
    protected ScheduledExecutorService scheduler;
    protected boolean timeout;
    protected long timeoutDelay;
    protected ScheduledFuture scheduledFuture;
    protected String taskId;

    protected AbstractWriteTask (String taskId,
                                 BehaviorSubject<TaskMessage> taskSignal,
                                 String subscribedTopicFilter,
                                 ScheduledExecutorService scheduler,
                                 long timeoutDelay) {
        this.taskId = taskId;
        this.taskSignal = taskSignal;
        this.subscribedTopicFilter = subscribedTopicFilter;
        this.fileSizeCount = 0;
        this.lastSeq = -1;
        this.timeout = true;;
        this.scheduler = scheduler;
        this.timeoutDelay = timeoutDelay;
        countDown();
    }

    public abstract void doProcess(byte[] data) throws IOException;
    protected void close() {
        try {
            scheduledFuture.cancel(true);
            bufferedOutputStream.close();
        }catch (IOException exception) {
            log.error("failed to close output stream: ", exception);
        }finally {
            taskSignal.onNext(new TaskMessage(subscribedTopicFilter, taskId,
                    TaskMessage.MessageType.UNSUB, null, 0L));
        }
    };

    protected void countDown() {
        scheduledFuture = scheduler.schedule(() -> {
            if (timeout) {
                log.debug("close write task: {} due to too much idle time", taskId);
                close();
            }
        }, timeoutDelay, TimeUnit.MILLISECONDS);
    }
}
