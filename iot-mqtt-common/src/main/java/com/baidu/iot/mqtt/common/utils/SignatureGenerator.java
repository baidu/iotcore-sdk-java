/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.iot.mqtt.common.utils;

import lombok.Getter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SignatureGenerator {

    public enum AlgorithmType {
        SHA256("SHA-256"),
        MD5("MD5");

        @Getter
        private final String algorithmName;

        AlgorithmType(String algorithmName) {
            this.algorithmName = algorithmName;
        }
    }

    private static final char[] hexDigits = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static String genUsername(String iotCoreId, String deviceKey, long timestamp, AlgorithmType algorithmType) {
        return String.format("thingidp@%s|%s|%d|%s", iotCoreId, deviceKey, timestamp, algorithmType.name());
    }

    public static String genSignature(String deviceKey, long timestamp, AlgorithmType algorithmType,
                                      String deviceSecret) throws NoSuchAlgorithmException {
        String key = String.format("%s&%d&%s%s", deviceKey, timestamp, algorithmType.name(), deviceSecret);
        byte[] btInput = key.getBytes();
        MessageDigest mdInst = MessageDigest.getInstance(algorithmType.getAlgorithmName());
        mdInst.update(btInput);
        byte[] md = mdInst.digest();
        int j = md.length;
        char[] str = new char[j * 2];
        int k = 0;
        for (byte byte0 : md) {
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str).toLowerCase();
    }

}
