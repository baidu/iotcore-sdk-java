# IoTCore Logger SDK for Java

## 帮助文档
* 详细使用文档参见wiki [iotcore-log-sdk使用指南](https://github.com/baidu/iotcore-sdk-java/wiki/%E6%97%A5%E5%BF%97%E6%9C%8D%E5%8A%A1SDK)
* 支持的java版本：1.8
    
## 快速开始

### 添加maven依赖
```$xslt
<dependency>
  <groupId>com.baidu.iot</groupId>
  <artifactId>iotcore-log-sdk</artifactId>
  <version>1.0.3</version>
</dependency>
```
### 初始化IotCoreLogger
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

### 接收日志
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


