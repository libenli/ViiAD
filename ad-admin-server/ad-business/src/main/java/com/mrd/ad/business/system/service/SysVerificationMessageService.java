package com.mrd.ad.business.system.service;

import com.mrd.ad.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class SysVerificationMessageService {

    private static final Logger log = LoggerFactory.getLogger(SysVerificationMessageService.class);
    private static final String SMS_ENDPOINT = "https://dysmsapi.aliyuncs.com/";
    private static final String SMS_API_VERSION = "2017-05-25";
    private static final String TYPE_PHONE = "phone";
    private static final String TYPE_EMAIL = "email";

    @Value("${mrd.auth.verification-code.mock-send:true}")
    private boolean mockSend;

    @Value("${mrd.auth.sms.access-key-id:}")
    private String smsAccessKeyId;

    @Value("${mrd.auth.sms.access-key-secret:}")
    private String smsAccessKeySecret;

    @Value("${mrd.auth.sms.sign-name:}")
    private String smsSignName;

    @Value("${mrd.auth.sms.region-id:cn-hangzhou}")
    private String smsRegionId;

    @Value("${mrd.auth.sms.template-code.zh:}")
    private String smsTemplateZh;

    @Value("${mrd.auth.sms.template-code.en:}")
    private String smsTemplateEn;

    @Value("${mrd.auth.sms.template-code.overseas:}")
    private String smsTemplateOverseas;

    @Value("${mrd.auth.email.host:}")
    private String emailHost;

    @Value("${mrd.auth.email.port:465}")
    private int emailPort;

    @Value("${mrd.auth.email.username:}")
    private String emailUsername;

    @Value("${mrd.auth.email.password:}")
    private String emailPassword;

    @Value("${mrd.auth.email.from:}")
    private String emailFrom;

    @Value("${mrd.auth.email.from-name:ViiAD}")
    private String emailFromName;

    @Value("${mrd.auth.email.ssl:true}")
    private boolean emailSsl;

    @Value("${mrd.auth.email.starttls:false}")
    private boolean emailStarttls;

    public void sendLoginCode(String type, String countryCode, String target, String code, String locale) {
        sendCode(type, countryCode, target, code, locale, "login");
    }

    public void sendForgotPasswordCode(String type, String countryCode, String target, String code, String locale) {
        sendCode(type, countryCode, target, code, locale, "forgot-password");
    }

    private void sendCode(String type, String countryCode, String target, String code, String locale, String scene) {
        if (mockSend) {
            log.info("ViiAD verification code mock send. scene={}, type={}, countryCode={}, target={}, locale={}, code={}",
                    scene, type, countryCode, maskTarget(target), locale, code);
            return;
        }
        if (TYPE_EMAIL.equals(type)) {
            sendEmail(target, code, locale, scene);
            return;
        }
        if (TYPE_PHONE.equals(type)) {
            sendSms(countryCode, target, code, locale);
            return;
        }
        throw new BusinessException("Unsupported verification type");
    }

    private void sendEmail(String email, String code, String locale, String scene) {
        checkEmailConfig();
        boolean english = isEnglish(locale);
        String title = english ? "ViiAD Verification Code" : "ViiAD 验证码";
        String intro = "forgot-password".equals(scene)
                ? (english ? "You are resetting your password. Your verification code is:" : "您正在重置密码，您的验证码为：")
                : (english ? "You are signing in to ViiAD. Your verification code is:" : "您正在登录 ViiAD，您的验证码为：");
        String content = buildEmailContent(intro, code, english);
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.host", emailHost.trim());
            properties.put("mail.smtp.port", String.valueOf(emailPort));
            properties.put("mail.smtp.ssl.enable", String.valueOf(emailSsl));
            properties.put("mail.smtp.starttls.enable", String.valueOf(emailStarttls));
            properties.put("mail.smtp.ssl.protocols", "TLSv1 TLSv1.1 TLSv1.2");
            Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUsername.trim(), emailPassword);
                }
            });
            MimeMessage message = new MimeMessage(session);
            String fromAddress = isBlank(emailFrom) ? emailUsername : emailFrom;
            message.setFrom(new InternetAddress(fromAddress.trim(), emailFromName, StandardCharsets.UTF_8.name()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email.trim()));
            message.setSubject(title, StandardCharsets.UTF_8.name());
            message.setContent(content, "text/html;charset=UTF-8");
            Transport.send(message);
        } catch (Exception ex) {
            throw new BusinessException("邮箱验证码发送失败：" + ex.getMessage());
        }
    }

    private void sendSms(String countryCode, String phone, String code, String locale) {
        checkSmsConfig();
        try {
            String normalizedCountryCode = isBlank(countryCode) ? "+86" : countryCode.trim();
            Map<String, String> params = new TreeMap<String, String>();
            params.put("AccessKeyId", smsAccessKeyId.trim());
            params.put("Action", "SendSms");
            params.put("Format", "JSON");
            params.put("PhoneNumbers", buildSmsPhone(normalizedCountryCode, phone));
            params.put("RegionId", smsRegionId);
            params.put("SignName", smsSignName.trim());
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("SignatureVersion", "1.0");
            params.put("TemplateCode", resolveSmsTemplate(normalizedCountryCode, locale));
            params.put("TemplateParam", "{\"code\":\"" + code + "\"}");
            params.put("Timestamp", utcNow());
            params.put("Version", SMS_API_VERSION);
            params.put("Signature", sign(params));
            String responseBody = postForm(params);
            if (!isSmsSuccess(responseBody)) {
                throw new BusinessException("短信验证码发送失败：" + extractSmsMessage(responseBody));
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("短信验证码发送失败：" + ex.getMessage());
        }
    }

    private void checkSmsConfig() {
        if (isBlank(smsAccessKeyId) || isBlank(smsAccessKeySecret) || isBlank(smsSignName)) {
            throw new BusinessException("请先配置短信服务，再操作");
        }
        if (isBlank(smsTemplateZh) && isBlank(smsTemplateEn) && isBlank(smsTemplateOverseas)) {
            throw new BusinessException("请先配置短信验证码模板，再操作");
        }
    }

    private void checkEmailConfig() {
        if (isBlank(emailHost) || isBlank(emailUsername) || isBlank(emailPassword)) {
            throw new BusinessException("请先配置邮箱服务，再操作");
        }
    }

    private String buildSmsPhone(String countryCode, String phone) {
        String phoneNumber = phone == null ? "" : phone.trim();
        if ("+86".equals(countryCode)) {
            return phoneNumber;
        }
        return countryCode.replace("+", "") + phoneNumber;
    }

    private String resolveSmsTemplate(String countryCode, String locale) {
        if (!"+86".equals(countryCode) && !isBlank(smsTemplateOverseas)) {
            return smsTemplateOverseas.trim();
        }
        if (isEnglish(locale) && !isBlank(smsTemplateEn)) {
            return smsTemplateEn.trim();
        }
        if (!isBlank(smsTemplateZh)) {
            return smsTemplateZh.trim();
        }
        return !isBlank(smsTemplateEn) ? smsTemplateEn.trim() : smsTemplateOverseas.trim();
    }

    private String sign(Map<String, String> params) throws Exception {
        StringBuilder canonicalizedQueryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (canonicalizedQueryString.length() > 0) {
                canonicalizedQueryString.append("&");
            }
            canonicalizedQueryString
                    .append(percentEncode(entry.getKey()))
                    .append("=")
                    .append(percentEncode(entry.getValue()));
        }
        String stringToSign = "POST&" + percentEncode("/") + "&" + percentEncode(canonicalizedQueryString.toString());
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((smsAccessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
    }

    private String postForm(Map<String, String> params) throws Exception {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (body.length() > 0) {
                body.append("&");
            }
            body.append(percentEncode(entry.getKey())).append("=").append(percentEncode(entry.getValue()));
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(SMS_ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        connection.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
        InputStream stream = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
        try {
            return readStream(stream);
        } finally {
            connection.disconnect();
        }
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        try {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } finally {
            reader.close();
        }
    }

    private String utcNow() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return format.format(new Date());
    }

    private String percentEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isSmsSuccess(String responseBody) {
        return responseBody != null && responseBody.replace(" ", "").contains("\"Code\":\"OK\"");
    }

    private String extractSmsMessage(String responseBody) {
        if (isBlank(responseBody)) {
            return "Aliyun response empty";
        }
        int index = responseBody.indexOf("\"Message\"");
        if (index < 0) {
            return responseBody;
        }
        int colonIndex = responseBody.indexOf(":", index);
        int startIndex = responseBody.indexOf("\"", colonIndex + 1);
        int endIndex = responseBody.indexOf("\"", startIndex + 1);
        if (colonIndex < 0 || startIndex < 0 || endIndex < 0) {
            return responseBody;
        }
        return responseBody.substring(startIndex + 1, endIndex);
    }

    private String buildEmailContent(String intro, String code, boolean english) {
        String tip = english
                ? "The code is valid for 5 minutes. Do not share it with anyone."
                : "验证码有效期为5分钟，请勿泄露给他人。";
        String greeting = english ? "Dear user," : "尊敬的用户，您好：";
        return "<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.8;color:#1f2d3d;\">"
                + "<p>" + greeting + "</p>"
                + "<p>" + intro + "</p>"
                + "<p style=\"font-size:28px;font-weight:700;letter-spacing:6px;color:#1677ff;\">" + code + "</p>"
                + "<p style=\"color:#6b7280;\">" + tip + "</p>"
                + "<p>ViiAD</p>"
                + "</div>";
    }

    private boolean isEnglish(String locale) {
        return "en-US".equalsIgnoreCase(locale) || "en".equalsIgnoreCase(locale);
    }

    private String maskTarget(String target) {
        if (target == null || target.length() <= 4) {
            return "****";
        }
        return target.substring(0, 2) + "****" + target.substring(target.length() - 2);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
