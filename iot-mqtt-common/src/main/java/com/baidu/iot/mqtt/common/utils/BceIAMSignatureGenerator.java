/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.iot.mqtt.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
