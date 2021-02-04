# IoTCore sdk
IoT Core设备管理java sdk，包含：
- 影子设备侧和控制侧sdk：隐藏了mqtt协议以及网络细节，方便用户使用物联网核心套件影子相关功能，支持多种认证方式（包含签名，ssl等），并提供上报信息，下发指令以及监听指令变化等功能。
    
* 详细使用文档参见wiki [SDK使用指南](https://github.com/baidu/iotcore-sdk-java/wiki)
* 支持的java版本：1.8及以上

    
## 快速开始
| 其他场景参考 [示例代码](https://github.com/baidu/iotcore-sdk-java/tree/main/iot-device-sdk-avatar-samples/src/main/java/com/baidu/iot/device/sdk/avatar/samples)
### 添加maven依赖
```$xslt
<dependency>
  <groupId>com.baidu.iot</groupId>
  <artifactId>iot-device-sdk-avatar-deviceside</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```
### 初始化
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

### 上报属性
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
## 测试
参考 [创建设备](https://cloud.baidu.com/doc/IoTCore/s/rk7omsf4h) 创建可以连接到iotCore的设备，再通过示例代码验证

## 维护者
- 张潇: zhangxiao18@baidu.com
