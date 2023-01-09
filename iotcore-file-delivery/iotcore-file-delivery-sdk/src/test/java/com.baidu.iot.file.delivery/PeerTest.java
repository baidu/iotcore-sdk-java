package com.baidu.iot.file.delivery;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Test;

import com.baidu.iot.file.delivery.result.DownloadResult;
import com.baidu.iot.file.delivery.result.UploadResult;

import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;

public class PeerTest {
    @Tested
    private Peer peer;
    @Mocked
    private IoTCoreClient client;
    @Injectable
    private String iotCoreId = "dummy";
    @Injectable
    private String userName = "username";
    @Injectable
    private String password = "password";
    @Injectable
    private String peerId = "peerId";
    @Injectable
    private String fileHolderDir = "fileHolderDir";
    @Injectable
    private Integer chunkSize = 10;
    @Injectable
    private Long messageSendInterval = 1L;
    @Injectable
    private Long waitForCompletion = 1L;
    @Injectable
    private Integer maxRetryTimes = 1;
    @Injectable
    private ScheduledExecutorService scheduler = null;

    private static final String EMPTY_FILE_NAME = "";
    private static final String EMPTY_DIR = "/var";
    private static final String REMOTE_PEER_ID = "";

    @Test
    public void testUploadWithFileNotExist() {
        CompletableFuture future = peer.upload(new File(EMPTY_FILE_NAME), REMOTE_PEER_ID);
        assertEquals(future.join(), UploadResult.FAILED);
    }

    @Test
    public void testDownloadWithNoPermission() {
        CompletableFuture future = peer.download(EMPTY_FILE_NAME, REMOTE_PEER_ID, EMPTY_DIR);
        assertEquals(future.join(), DownloadResult.NO_PERMISSION);
    }
}
