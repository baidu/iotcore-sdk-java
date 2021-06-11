# IoTCore sdk
IoT Core java sdk，包含：
- 影子设备侧和控制侧sdk：隐藏了mqtt协议以及网络细节，方便用户使用物联网核心套件影子相关功能，支持多种认证方式（包含签名，ssl等），并提供上报信息，下发指令以及监听指令变化等功能。
- IoTCore日志服务sdk：可以通过sdk快速的通过原生的mqtt方式使用IoTCore的日志服务，隐藏了mqtt协议的使用及维护细节，内部采用多client共享订阅的方式保证了日志量较大的情况下消息通道的拓展性。

## iot-device-sdk
### 帮助文档
* 详细使用文档参见wiki [iot-device-sdk使用指南](https://github.com/baidu/iotcore-sdk-java/wiki)
* 支持的java版本：1.8及以上

    
### 快速开始
>其他场景参考 [示例代码](https://github.com/baidu/iotcore-sdk-java/tree/main/iot-device-sdk-avatar-samples/src/main/java/com/baidu/iot/device/sdk/avatar/samples)
#### 添加maven依赖
```$xslt
<dependency>
  <groupId>com.baidu.iot</groupId>
  <artifactId>iot-device-sdk-avatar-deviceside</artifactId>
  <version>1.0.3</version>
</dependency>
```
#### 初始化
```$xslt
String iotCoreId = "yourIoTCoreId"; 
String deviceName = "yourDeviceKey";  
String username = "yourIoTCoreId/yourDeviceKey";
char[] password = "yourDeviceSecret".toCharArray();

IoTDeviceFactory factory = new IoTDeviceFactory(IoTDeviceFactory.Config.builder()
        .iotCoreId(iotCoreId)
        .build());

Device device = factory.getDevice(deviceName,
                MqttConfigFactory.genPlainMqttConfig(iotCoreId, username, password))
                .blockingGet();
```

#### 上报属性
```$xslt
Map<PropertyKey, PropertyValue> properties = new HashMap<>();
properties.put(new PropertyKey("test"), new PropertyValue("\"test value\""));
device.updateReported(properties).blockingSubscribe(new DisposableSingleObserver<Status>() {
    @Override
    public void onSuccess(@NonNull Status status) {
        System.out.println("Update reported success, status:" + status);
    }

    @Override
    public void onError(@NonNull Throwable e) {
        System.out.println("Update reported failure");
        e.printStackTrace();
    }
});
```
### 测试
参考 [创建设备](https://cloud.baidu.com/doc/IoTCore/s/rk7omsf4h) 创建可以连接到iotCore的设备，再通过示例代码验证

## iotcore-log-sdk
### 帮助文档
* 详细使用文档参见wiki [iotcore-log-sdk使用指南](to be done)
* 支持的java版本：1.8及以上
    
### 快速开始

#### 添加maven依赖
```$xslt
<dependency>
  <groupId>com.baidu.iot</groupId>
  <artifactId>iotcore-log-sdk</artifactId>
  <version>1.0.3</version>
</dependency>
```
#### 初始化IotCoreLogger
```$xslt
String iotCoreId = "yourIoTCoreId";
String username = "yourIoTCoreId/yourDeviceKey";
char[] password = "yourDeviceSecret".toCharArray();

MqttConfig mqttConfig = MqttConfigFactory.genPlainMqttConfig(iotCoreId, username, password);

Config config = Config.builder()
        .iotCoreId(iotCoreId)
        .mqttConfig(mqttConfig)
        .logLevel(LogLevel.INFO)
        .includeLowerLevel(true)
        .clientPoolSize(3)
        .build();

IotCoreLoggerRegister register = new IotCoreLoggerRegister();
IotCoreLogger logger = register.registerLogger(config).blockingGet();
```

#### 接收日志
```$xslt
logger.receive().subscribeWith(new DisposableObserver<LogEntry>() {
    @Override
    public void onNext(@NonNull LogEntry logEntry) {
        // handle logEntry
        System.out.println(logEntry);
    }

    @Override
    public void onError(@NonNull Throwable e) {
        // handle error
        e.printStackTrace();
        dispose();
    }

    @Override
    public void onComplete() {
        dispose();
    }
});
```


