/*
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.iot.device.sdk.avatar.common;

import com.baidu.iot.device.sdk.avatar.common.utils.BceIAMSignatureGenerator;
import com.baidu.iot.device.sdk.avatar.common.utils.SSLSocketFactoryGenerator;
import com.baidu.iot.device.sdk.avatar.common.utils.SignatureGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

public class MqttConfigFactory {

    private static final String defaultTcpIotCoreEndpointPattern = "%s.iot.gz.baidubce.com:1883";

    private static final String defaultSslIotCoreEndpointPattern = "%s.iot.gz.baidubce.com:1884";

    public static MqttConfig genPlainMqttConfig(String iotCoreId, String username, char[] password) {
        URI uri = URI.create("tcp://" + String.format(defaultTcpIotCoreEndpointPattern, iotCoreId));
        return genPlainMqttConfig(uri, iotCoreId, username, password);
    }

    public static MqttConfig genPlainMqttConfig(URI uri, String iotCoreId, String username, char[] password) {
        return new MqttConfig(uri, null, username, password);
    }

    /**
     * Generate mqtt config using signature with md5 algorithm.
     *
     * @param iotCoreId
     * @param deviceKey
     * @param deviceSecret
     * @return
     */
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
        URI uri = URI.create("tcp://" + String.format(defaultTcpIotCoreEndpointPattern, iotCoreId));
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
        URI uri = URI.create("ssl://" + String.format(defaultSslIotCoreEndpointPattern, iotCoreId));
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
        URI uri = URI.create("tcp://" + String.format(defaultTcpIotCoreEndpointPattern, iotCoreId));
        return genBceIamSignatureMqttConfig(uri, iotCoreId, appKey, secretKey);
    }

    public static MqttConfig genBceIamSignatureMqttConfig(URI uri, String iotCoreId, String appKey, String secretKey) {
        Calendar cal = Calendar.getInstance() ;
        long BJTimestamp = cal.getTime().getTime();
        long UTCTimestamp = getUTCTimestamp(cal);
        try {
            return new MqttConfig(uri, null,
                    BceIAMSignatureGenerator.genUsername(iotCoreId, appKey, BJTimestamp),
                    BceIAMSignatureGenerator.genSignature(appKey, secretKey, UTCTimestamp).toCharArray());
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
