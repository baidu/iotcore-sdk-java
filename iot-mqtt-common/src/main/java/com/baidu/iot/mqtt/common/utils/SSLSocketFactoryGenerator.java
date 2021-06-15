/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.iot.mqtt.common.utils;

import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLSocketFactoryGenerator {

    static {
        java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public static SSLSocketFactory genSSLSocketFactory(File caCertFile,
                                                       File deviceCertFile,
                                                       File devicePrivateKeyFile) throws Exception {
        X509TrustManager trustManager = getTrustManager(caCertFile);

        KeyManager[] km = getKeyManager(deviceCertFile, devicePrivateKeyFile);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(km, new TrustManager[] {trustManager}, null);
        return context.getSocketFactory();
    }

    private static X509TrustManager getTrustManager(File trustCertFile) throws Exception {
        InputStream certInputStream = new FileInputStream(trustCertFile);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Certificate cert = certFactory.generateCertificate(certInputStream);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        keyStore.setCertificateEntry("ca", cert);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return  (X509TrustManager) trustManagers[0];
    }

    private static KeyManager[] getKeyManager(File certFile, File certPrivateKeyFile)
            throws Exception {
        try (InputStream in = new ByteArrayInputStream(fileToBytes(certFile))) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate x509Certificate = (X509Certificate) certFactory.generateCertificate(in);
            PrivateKey privateKey = getPrivateKey(certPrivateKeyFile);
            KeyStore keyStore1 = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore1.load(null);
            keyStore1.setCertificateEntry("cert-alias", x509Certificate);
            keyStore1.setKeyEntry("key-alias", privateKey, "notUse".toCharArray(), new Certificate[]{x509Certificate});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore1, "notUse".toCharArray());
            KeyManager[] km = kmf.getKeyManagers();
            return km;
        }
    }

    private static PrivateKey getPrivateKey(File certPrivateKeyFile) throws Exception {
        FileInputStream fis = new FileInputStream(certPrivateKeyFile);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int) certPrivateKeyFile.length()];
        dis.readFully(keyBytes);
        dis.close();
        String temp = new String(keyBytes);
        String privKeyPEM = temp.replace("-----BEGIN RSA PRIVATE KEY-----", "");
        privKeyPEM = privKeyPEM.replace("-----END RSA PRIVATE KEY-----", "");
        BASE64Decoder b64 = new BASE64Decoder();
        byte[] decoded = b64.decodeBuffer(privKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static byte[] fileToBytes(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            byte[] bytes = new byte[input.available()];
            input.read(bytes);
            return bytes;
        }
    }

}
