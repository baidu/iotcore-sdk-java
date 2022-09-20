package com.baidu.iot.file.delivery.task;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import com.baidu.iot.file.delivery.utils.IoTCoreFeedback;
import com.baidu.iot.file.delivery.utils.TaskMessage;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractReadTask {
    protected String absoluteFileName;
    protected String sendTopic;
    protected long seq;
    protected int chunkSize;
    protected boolean endOfFile;
    protected ScheduledFuture scheduledFuture;
    protected FileReader reader;
    protected BehaviorSubject<TaskMessage> taskSignal;
    protected BehaviorSubject<IoTCoreFeedback> feedbackSignal;
    protected CompositeDisposable disposables;
    protected String taskId;
    protected InFlightTaskMessage unConfirmedTaskMsg;

    @AllArgsConstructor
    protected class InFlightTaskMessage {
        TaskMessage taskMessage;
        int resendTimes;
    }

    class FileReader {
        private FileInputStream fileIn;
        private ByteBuffer byteBuf;
        private byte[] array;

        FileReader() throws FileNotFoundException {
            this.fileIn = new FileInputStream(absoluteFileName);
            this.byteBuf = ByteBuffer.allocate(chunkSize);;
        }

        byte[] read() throws IOException {
            FileChannel fileChannel = fileIn.getChannel();
            int bytes = fileChannel.read(byteBuf);// Read ByteBuffer in
            if (bytes != -1) {
                array = new byte[bytes];// Byte array length is read length
                byteBuf.flip();
                byteBuf.get(array);// from ByteBuffer Get byte array from
                byteBuf.clear();
                return array;
            }
            return null;
        }

        void close() throws IOException {
            log.trace("close reader");
            fileIn.close();
            array = null;
        }
    }

    protected AbstractReadTask(String fileName,
                               String dir,
                               int chunkSize,
                               BehaviorSubject<TaskMessage> taskSignal,
                               BehaviorSubject<IoTCoreFeedback> feedbackSignal) throws FileNotFoundException {
        this.seq = 0L;
        this.absoluteFileName = dir + "/" + fileName;
        this.chunkSize = chunkSize;
        this.reader = new FileReader();
        this.taskSignal = taskSignal;
        this.feedbackSignal = feedbackSignal;
        this.endOfFile = false;
        this.disposables = new CompositeDisposable();
        this.taskId = UUID.randomUUID().toString();
        this.unConfirmedTaskMsg = null;
    }

    public abstract void doProcess();

    public void close() {
        disposables.dispose();
        scheduledFuture.cancel(true);
        try {
            reader.close();
        }catch (IOException exception) {
            log.error("close read task error: ", exception);
        }
    }
}
