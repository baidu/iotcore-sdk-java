// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.mqtt.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import com.baidu.iot.mqtt.common.utils.BceIAMSignatureGenerator;
import com.baidu.iot.mqtt.common.utils.SSLSocketFactoryGenerator;
import com.baidu.iot.mqtt.common.utils.SignatureGenerator;

public class MqttConfigFactory {

    private static final String DEFAULT_TCP_IOT_CORE_ENDPOINT_PATTERN = "%s.iot.gz.baidubce.com:1883";

    private static final String DEFAULT_SSL_IOT_CORE_ENDPOINT_PATTERN = "%s.iot.gz.baidubce.com:1884";

    public static MqttConfig genPlainMqttConfig(String iotCoreId, String username, char[] password) {
        URI uri = URI.create("tcp://" + String.format(DEFAULT_TCP_IOT_CORE_ENDPOINT_PATTERN, iotCoreId));
        return genPlainMqttConfig(uri, username, password);
    }

    public static MqttConfig genPlainMqttConfig(URI uri, String username, char[] password) {
        return new MqttConfig(uri, null, username, password);
    }

    public static MqttConfig genMD5SignatureMqttConfig(String iotCoreId, String deviceKey, String deviceSecret) {
        return genSignatureMqttConfig(iotCoreId, deviceKey, System.currentTimeMillis() / 1000,
                deviceSecret, SignatureGenerator.AlgorithmType.MD5);
    }

    public static MqttConfig genMD5SignatureMqttConfig(
            URI uri, String iotCoreId, String deviceKey, String deviceSecret) {
        return genSignatureMqttConfig(uri, iotCoreId, deviceKey, System.currentTimeMillis() / 1000,
                deviceSecret, SignatureGenerator.AlgorithmType.MD5);
    }

    public static MqttConfig genSHA256SignatureMqttConfig(String iotCoreId, String deviceKey, String deviceSecret) {
        return genSignatureMqttConfig(iotCoreId, deviceKey, System.currentTimeMillis() / 1000,
                deviceSecret, SignatureGenerator.AlgorithmType.SHA256);
    }

    public static MqttConfig genSHA256SignatureMqttConfig(
            URI uri, String iotCoreId, String deviceKey, String deviceSecret) {
        return genSignatureMqttConfig(uri, iotCoreId, deviceKey, System.currentTimeMillis() / 1000,
                deviceSecret, SignatureGenerator.AlgorithmType.SHA256);
    }

    private static MqttConfig genSignatureMqttConfig(String iotCoreId, String deviceKey,
                                                     long timestamp, String deviceSecret,
                                                     SignatureGenerator.AlgorithmType algorithmType) {
        URI uri = URI.create("tcp://" + String.format(DEFAULT_TCP_IOT_CORE_ENDPOINT_PATTERN, iotCoreId));
        return genSignatureMqttConfig(uri, iotCoreId, deviceKey, timestamp, deviceSecret, algorithmType);
    }

    private static MqttConfig genSignatureMqttConfig(URI uri, String iotCoreId, String deviceKey,
                                                     long timestamp, String deviceSecret,
                                                     SignatureGenerator.AlgorithmType algorithmType) {
        try {
            return new MqttConfig(
                    uri,
                    null,
                    SignatureGenerator.genUsername(iotCoreId, deviceKey, timestamp, algorithmType),
                    SignatureGenerator.genSignature(deviceKey, timestamp, algorithmType, deviceSecret).toCharArray());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Can not generate signature config", e);
        }
    }

    public static MqttConfig genSSlMqttConfig(String iotCoreId, File caCertFile, File deviceCertFile,
                                              File devicePrivateKeyFile) throws FileNotFoundException {
        URI uri = URI.create("ssl://" + String.format(DEFAULT_SSL_IOT_CORE_ENDPOINT_PATTERN, iotCoreId));
        return genSSlMqttConfig(uri, caCertFile, deviceCertFile, devicePrivateKeyFile);
    }

    public static MqttConfig genSSlMqttConfig(URI uri, File caCertFile, File deviceCertFile,
                                              File devicePrivateKeyFile) throws FileNotFoundException {
        if (!caCertFile.exists()) {
            throw new FileNotFoundException("Ca cert file not found.");
        }
        if (!deviceCertFile.exists()) {
            throw new FileNotFoundException("Device cert file not found.");
        }
        if (!devicePrivateKeyFile.exists()) {
            throw new FileNotFoundException("Device private key file not found.");
        }
        try {
            return new MqttConfig(uri, SSLSocketFactoryGenerator.genSSLSocketFactory(
                    caCertFile, deviceCertFile, devicePrivateKeyFile), null, null);
        } catch (Exception e) {
            throw new RuntimeException("Can not generate Ssl config", e);
        }
    }

    public static MqttConfig genBceIamSignatureMqttConfig(String iotCoreId, String appKey, String secretKey) {
        URI uri = URI.create("tcp://" + String.format(DEFAULT_TCP_IOT_CORE_ENDPOINT_PATTERN, iotCoreId));
        return genBceIamSignatureMqttConfig(uri, iotCoreId, appKey, secretKey);
    }

    public static MqttConfig genBceIamSignatureMqttConfig(URI uri, String iotCoreId, String appKey, String secretKey) {
        Calendar cal = Calendar.getInstance();
        long bjTimestamp = cal.getTime().getTime();
        long utcTimestamp = getUTCTimestamp(cal);
        try {
            return new MqttConfig(uri, null,
                    BceIAMSignatureGenerator.genUsername(iotCoreId, appKey, bjTimestamp),
                    BceIAMSignatureGenerator.genSignature(appKey, secretKey, utcTimestamp).toCharArray());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Can not generate bce-iam signature config", e);
        }
    }

    private static long getUTCTimestamp(Calendar cal) {
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        cal.add(Calendar.MILLISECOND, - (zoneOffset + dstOffset));
        return cal.getTime().getTime();
    }
}
