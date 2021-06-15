# 背景及特性
百度天工IotCore服务对用户提供功能运行时的诊断日志供用户记录及诊断服务状态，日志出口可选标准mqtt通道，用户可以通过标准mqtt协议按需订阅日志内容。

根据目前日志服务的设计及用户实际使用情况看，直接使用日志服务存在一定的门槛，因此提供SDK解决以下问题：

* 日志格式定义的proto文件通过文档传递及更新较为困难。
* 日志通过mqtt通道获取，用户需要自行维护mqtt client的连接生命周期，各用户接入日志功能时的这部分工作属于重复劳动。
* 具体的日志订阅包含特定的日志topic格式、iotcore提供的共享订阅等对用户有一定接入门槛的内容。
* 封装一些使用限制，防止用户滥用引起服务资源消耗过大。

#maven依赖
```$xslt
<dependency>
  <groupId>com.baidu.iot</groupId>
  <artifactId>iotcore-log-sdk</artifactId>
  <version>1.0.3</version>
</dependency>
```
#使用说明
##配置创建IotCoreLogger
```$xslt
String iotCoreId = "yourIoTCoreId";                 // 天工平台创建的iotcore
String username = "yourIoTCoreId/yourDeviceKey";    // 天工平台创建的设备用户名或签名
char[] password = "yourDeviceSecret".toCharArray(); // 天工平台创建的设备密码或签名密钥

// 获取mqtt连接信息配置，可选tcp、tls等多种配置方式，详见MqttConfigFactory实现
MqttConfig mqttConfig = MqttConfigFactory.genPlainMqttConfig(iotCoreId, username, password);

// 创建IotCoreLogger配置信息
Config config = Config.builder()
        .iotCoreId(iotCoreId)
        .mqttConfig(mqttConfig)
        .logLevel(LogLevel.INFO)
        .includeLowerLevel(true)
        .clientPoolSize(3)
        .build();
// 创建IotCoreLogger实例，用于接收日志
IotCoreLoggerRegister register = new IotCoreLoggerRegister();
IotCoreLogger logger = register.registerLogger(config).blockingGet();
```
###Config配置说明
* iotCoreId: 天工平台创建的iotcore
* groupKey: 日志接收者所属的group，同一group中的IotCoreLogger共享所有的日志信息，默认为每次不同的随机值。可参考kafka consumer group进行理解。
* logLevel: 接收日志的级别，可选ERROR、WARN、INFO、DEBUG，级别由低至高。
* includeLowerLevel: 所选日志级别是否包含低级，如选择接收INFO级别日志，默认包含WARN及ERROR级别日志。
* deviceKeys: 执行设备名称列表，只接收特定设备的运行日志。
* clientPoolSize: 内部创建的mqtt client数量，最大100。可根据所接收的日志规模及速率适量选取，每个client最低支持100 qps的日志。
* mqttConfig: 内部mqtt client使用相关配置。
> mqtt连接身份需要具有日志主题的订阅权限，主体格式参考LogLevel，权限配置见[官网地址](https://cloud.baidu.com/product/iot.html)

##接收日志
> 日志接收提供rx风格的订阅接口
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
##日志解析
sdk提供的日志LogEntry为proto中定义的原始日志格式，如需进一步的解析details中key值的含义，可参考iotcore-logger-standalone中的解析方式。

##快速开始使用
为了进一步降低iotcore-log-sdk的理解及使用难度，提供了iotcore-logger-standalone模块，可以直接下载运行使用。运行参数如下：
```$xslt
Usage: <main class> [options]
  Options:
    --clientCount
      Total test client count
      Default: 3
    --deviceKeys
      Logs only from the specific devices would arrive
      Default: [Ljava.lang.String;@6a01e23
    --help
      Show help
    --includeLowerLevel
      Whether the log level should include the lower levels, such as INFO 
      leven would include WARN and ERROR
      Default: true
  * --iotCoreId
      Receive logs from this iotCore
    --level
      Level of the received logs: ERROR, WARN, INFO, DEBUG
      Default: INFO
  * --password
      Mqtt client password
    --uri
      Mqtt broker uri
  * --username
      Mqtt client username
```

快速使用示例：
```$xslt
nohup java -jar iotcore-logger-standalone.jar --iotCoreId axxxxxx --username axxxxxx/test --password hubKQZRMYaQXDgDe > logsdk.log 2>&1 &
```


