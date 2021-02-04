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

package com.baidu.iot.device.sdk.avatar.common.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BceIAMSignatureGenerator {

    public static String genUsername(String iotCoreId, String appKey, long BJTimestamp) {
        return String.format("bceiam@%s|%s|%d|SHA256", iotCoreId, appKey, BJTimestamp);
    }

    public static String genSignature(String accessKey, String secretKey, long UTCTimestamp)
            throws NoSuchAlgorithmException, InvalidKeyException {
        return hmacSHA256("POST\n/connect\n\nhost:iot.gz.baidubce.com",
                genSignKey(accessKey, secretKey, UTCTimestamp));
    }

    private static String genSignKey(String accessKey, String secretKey, long timestamp)
            throws InvalidKeyException, NoSuchAlgorithmException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String date = dateFormat.format(new Date(timestamp));
        return hmacSHA256(String.format("bce-auth-v1/%s/%s/60", accessKey, date), secretKey);
    }

    private static String hmacSHA256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256HMAC.init(secretKey);

        byte[] array = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte item : array) {
            sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
        }
        return sb.toString().toLowerCase();
    }

}
