package com.baidu.iot.file.delivery;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.baidu.iot.file.delivery.result.DownloadResult;
import com.baidu.iot.file.delivery.result.UploadResult;

public interface IPeer {

    CompletableFuture<DownloadResult> download(String fileName, String remotePeer, String dir) throws MqttException, FileNotFoundException;

    CompletableFuture<UploadResult> upload(File file, String remotePeer);

    void close() throws MqttException;
}
