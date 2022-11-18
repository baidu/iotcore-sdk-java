// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.file.delivery;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.baidu.iot.file.delivery.result.DownloadResult;
import com.baidu.iot.file.delivery.result.UploadResult;

/**
 * This is the interface that defines the basic behaviours one peer should have.
 * One can download files from other peers and can also upload its own files to remote peers.
 *
 * @author  gujiawei01
 */
public interface IPeer {

    /**
     * Download interested file from others
     *
     * @param fileName the fileName that the peer interests in
     * @param remotePeer the remotePeer id who has the target file
     * @param dir file directory
     * @return a CompletableFuture that indicates the download result
     */
    CompletableFuture<DownloadResult> download(String fileName, String remotePeer, String dir);

    /**
     * Upload file to others who are interested in the file
     *
     * @param file the file object that the peer wants to upload to others
     * @param remotePeer the target remote peer id
     * @return a CompletableFuture that indicates the upload result
     */
    CompletableFuture<UploadResult> upload(File file, String remotePeer);

    /**
     * Close the peer instance thoroughly.
     *
     * @throws MqttException if any other problem was encountered during the interaction with IoTCore service
     */
    void close() throws MqttException;
}
