package com.baidu.iot.file.delivery.task;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import com.baidu.iot.file.delivery.exception.IoTCoreTimeoutException;
import com.baidu.iot.file.delivery.message.Download;
import com.baidu.iot.file.delivery.message.DownloadDataMessage;
import com.baidu.iot.file.delivery.message.FileMetaData;
import com.baidu.iot.file.delivery.result.DownloadResult;
import com.baidu.iot.file.delivery.utils.TaskMessage;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadWriteTask extends AbstractWriteTask {
    private CompletableFuture<DownloadResult> future;
    public DownloadWriteTask(BehaviorSubject<TaskMessage> taskSignal,
                             String subscribedTopicFilter,
                             ScheduledExecutorService scheduler,
                             long timeoutDelay,
                             CompletableFuture<DownloadResult> future) {
        super(UUID.randomUUID().toString(), taskSignal, subscribedTopicFilter, scheduler, timeoutDelay);
        this.future = future;
    }

    public void setFileMetaDataAndWriteBuffer(FileMetaData fileMetaData, String fileName) throws FileNotFoundException {
        this.fileMetaData = fileMetaData;
        this.bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileName),
                fileMetaData.getChunkSize());
    }

    public FileMetaData getFileMetaData() {
        return this.fileMetaData;
    }

    @Override
    public void doProcess(byte[] data) throws IOException {
        timeout = false;
        scheduledFuture.cancel(true);
        if (data == null || data.length == 0) {
            close(DownloadResult.OK);
        }
        Download download = DownloadDataMessage.parseFrom(data).getDownload();
        if (download.getSeq() == lastSeq + 1) {
            lastSeq++;
            byte[] chunk = download.getChunkData().toByteArray();
            fileSizeCount += chunk.length;
            bufferedOutputStream.write(chunk);
            if (fileSizeCount >= fileMetaData.getFileSize()) {
                close(DownloadResult.OK);
            }else {
                timeout = true;
                countDown();
            }
        }else if (download.getSeq() != lastSeq) {
            log.warn("receive non-consecutive chunk");
            close(DownloadResult.FAILED);
        }

    }

    private void close(DownloadResult result) {
        future.complete(result);
        close();
    }

    @Override
    protected void close() {
        if (timeout == true && !future.isDone()) {
            future.completeExceptionally(new IoTCoreTimeoutException("wait too much idle time for next chunk"));
        }
        super.close();
    }
}
