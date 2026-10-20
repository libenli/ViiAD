package com.mrd.ad.business.wallet.pay;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class WechatPayHelper {

    private WechatPayHelper() {
    }

    public static String nonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static int yuanToCent(BigDecimal amount) {
        return amount.multiply(new BigDecimal("100")).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
    }

    public static String sign(Map<String, String> params, String apiKey) {
        TreeMap<String, String> sorted = new TreeMap<String, String>(params);
        StringBuilder raw = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if ("sign".equals(entry.getKey()) || StringUtils.isBlank(entry.getValue())) {
                continue;
            }
            if (raw.length() > 0) {
                raw.append('&');
            }
            raw.append(entry.getKey()).append('=').append(entry.getValue());
        }
        raw.append("&key=").append(apiKey);
        return md5(raw.toString()).toUpperCase();
    }

    public static boolean verify(Map<String, String> params, String apiKey) {
        String sign = params.get("sign");
        return StringUtils.isNotBlank(sign) && sign.equalsIgnoreCase(sign(params, apiKey));
    }

    public static String toXml(Map<String, String> params) {
        StringBuilder xml = new StringBuilder("<xml>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            xml.append('<').append(entry.getKey()).append('>');
            xml.append("<![CDATA[").append(StringUtils.defaultString(entry.getValue())).append("]]>");
            xml.append("</").append(entry.getKey()).append('>');
        }
        xml.append("</xml>");
        return xml.toString();
    }

    public static Map<String, String> parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList children = document.getDocumentElement().getChildNodes();
            Map<String, String> result = new HashMap<String, String>();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    result.put(node.getNodeName(), node.getTextContent());
                }
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("微信支付XML解析失败", ex);
        }
    }

    public static String postXml(String url, String requestXml) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            OutputStream output = connection.getOutputStream();
            output.write(requestXml.getBytes(StandardCharsets.UTF_8));
            output.flush();
            output.close();
            InputStream input = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            int len;
            while ((len = input.read(bytes)) != -1) {
                buffer.write(bytes, 0, len);
            }
            input.close();
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("微信支付请求失败", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    public static String postXmlWithCert(String url, String requestXml, String certPath, String certPassword) {
        HttpsURLConnection connection = null;
        FileInputStream certInput = null;
        try {
            char[] password = StringUtils.defaultString(certPassword).toCharArray();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            certInput = new FileInputStream(certPath);
            keyStore.load(certInput, password);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

            connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            OutputStream output = connection.getOutputStream();
            output.write(requestXml.getBytes(StandardCharsets.UTF_8));
            output.flush();
            output.close();
            InputStream input = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            int len;
            while ((len = input.read(bytes)) != -1) {
                buffer.write(bytes, 0, len);
            }
            input.close();
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("微信退款证书请求失败", ex);
        } finally {
            if (certInput != null) {
                try {
                    certInput.close();
                } catch (Exception ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    private static String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte item : bytes) {
                String part = Integer.toHexString(item & 0xff);
                if (part.length() == 1) {
                    hex.append('0');
                }
                hex.append(part);
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("微信支付签名失败", ex);
        }
    }
}
