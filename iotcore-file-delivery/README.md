# IoTCore File Delivery SDK for Java
## Overview
This is an mqtt based file delivery SDK. According to the [IoTCore system restrictions](https://cloud.baidu.com/doc/IoTCore/s/3k7o8yehh), 
the maximum pay load for a single message is 32 KB which can be awkward under some cases such as AI model delivery and image transfer. 
Based on that, the SDK was developed to solve the problem. The core concept in ths SDK is peer which owns the ability of uploading files to and downloading files from remote peers.

## Using SDK
### Quick Start
To import the dependency, the maven goes:
```$xslt
<dependency>
   <groupId>com.baidu.bce-iot</groupId>
   <artifactId>iotcore-file-delivery</artifactId>
   <version>1.0.0</version>
</dependency>
```
In the SDK, the minimum unit is peer which warps an mqtt client. Beginning with, one should first create `Peer` instance with IoTCore credentials and basic information.
```$xslt
IPeer peer = Peer.builder()
                .iotCoreId("your_IoTCoreId")
                .userName("yourUserName")
                .password("yourPassword")
                .fileHolderDir("fileDir")
                .peerId("yourPeerId")
                .build();
```
Noticing, in th example, the 5 parameters are mandatory. Also, all the peers should share the same IoTCoreId for communication and have the permissions to publish and subscribe topics. For simplicity, `#` permission control is recommended.

After instancing the `peer`, one can invoke following methods for file delivery respectively.
1. `CompletableFuture<DownloadResult> download(String fileName, String remotePeer, String dir)` 
2. `CompletableFuture<UploadResult> upload(File file, String remotePeer)`

### Optional Parameters
1. `chunkSize`

    The file is sliced into chunks. Thus, chunkSize has impact on delivery efficiency. Too large or too small value is inappropriate. Default value is 32KB.
2. `messageSendInterval`

    After one chunk of data is delivered successfully, the sender can send next chunk. It specifies the sending rate. Sending data too fast may cause burden for the receiver. Default value is 3s.
3. `waitForCompletion`

    Interaction with IoTCore is an asynchronous model which means the delivery latency may be very large. It is the largest latency the user can tolerate for a complete message delivery with IoTCore.
4. `maxRetryTimes`

   Interaction with IoTCore may fail under some cases such as data loss due to the underlying network issue. It can make the peer to retry several times for a failed message delivery with IoTCore.

## CHANGELOG
Detailed changes for each release are documented in the release [notes](./CHANGELOG.md).