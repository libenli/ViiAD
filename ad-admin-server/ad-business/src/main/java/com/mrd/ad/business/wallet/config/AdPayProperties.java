package com.mrd.ad.business.wallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mrd.pay")
public class AdPayProperties {

    private Wechat wechat = new Wechat();
    private Alipay alipay = new Alipay();

    public Wechat getWechat() {
        return wechat;
    }

    public void setWechat(Wechat wechat) {
        this.wechat = wechat;
    }

    public Alipay getAlipay() {
        return alipay;
    }

    public void setAlipay(Alipay alipay) {
        this.alipay = alipay;
    }

    public static class Wechat {
        private boolean enabled;
        private String appId;
        private String mchId;
        private String apiKey;
        private String notifyUrl;
        private String unifiedOrderUrl = "https://api.mch.weixin.qq.com/pay/unifiedorder";
        private boolean refundEnabled;
        private String refundUrl = "https://api.mch.weixin.qq.com/secapi/pay/refund";
        private String refundQueryUrl = "https://api.mch.weixin.qq.com/pay/refundquery";
        private String refundCertPath;
        private String refundCertPassword;
        private String bodyPrefix = "ViiAD充值";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getMchId() {
            return mchId;
        }

        public void setMchId(String mchId) {
            this.mchId = mchId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getNotifyUrl() {
            return notifyUrl;
        }

        public void setNotifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
        }

        public String getUnifiedOrderUrl() {
            return unifiedOrderUrl;
        }

        public void setUnifiedOrderUrl(String unifiedOrderUrl) {
            this.unifiedOrderUrl = unifiedOrderUrl;
        }

        public boolean isRefundEnabled() {
            return refundEnabled;
        }

        public void setRefundEnabled(boolean refundEnabled) {
            this.refundEnabled = refundEnabled;
        }

        public String getRefundUrl() {
            return refundUrl;
        }

        public void setRefundUrl(String refundUrl) {
            this.refundUrl = refundUrl;
        }
        public String getRefundQueryUrl() {
            return refundQueryUrl;
        }

        public void setRefundQueryUrl(String refundQueryUrl) {
            this.refundQueryUrl = refundQueryUrl;
        }

        public String getRefundCertPath() {
            return refundCertPath;
        }

        public void setRefundCertPath(String refundCertPath) {
            this.refundCertPath = refundCertPath;
        }

        public String getRefundCertPassword() {
            return refundCertPassword;
        }

        public void setRefundCertPassword(String refundCertPassword) {
            this.refundCertPassword = refundCertPassword;
        }

        public String getBodyPrefix() {
            return bodyPrefix;
        }

        public void setBodyPrefix(String bodyPrefix) {
            this.bodyPrefix = bodyPrefix;
        }
    }

    public static class Alipay {
        private boolean enabled;
        private String appId;
        private String merchantPrivateKey;
        private String alipayPublicKey;
        private String notifyUrl;
        private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
        private boolean refundEnabled;
        private String charset = "UTF-8";
        private String signType = "RSA2";
        private String subjectPrefix = "ViiAD充值";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getMerchantPrivateKey() {
            return merchantPrivateKey;
        }

        public void setMerchantPrivateKey(String merchantPrivateKey) {
            this.merchantPrivateKey = merchantPrivateKey;
        }

        public String getAlipayPublicKey() {
            return alipayPublicKey;
        }

        public void setAlipayPublicKey(String alipayPublicKey) {
            this.alipayPublicKey = alipayPublicKey;
        }

        public String getNotifyUrl() {
            return notifyUrl;
        }

        public void setNotifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
        }

        public String getGatewayUrl() {
            return gatewayUrl;
        }

        public void setGatewayUrl(String gatewayUrl) {
            this.gatewayUrl = gatewayUrl;
        }

        public boolean isRefundEnabled() {
            return refundEnabled;
        }

        public void setRefundEnabled(boolean refundEnabled) {
            this.refundEnabled = refundEnabled;
        }

        public String getCharset() {
            return charset;
        }

        public void setCharset(String charset) {
            this.charset = charset;
        }

        public String getSignType() {
            return signType;
        }

        public void setSignType(String signType) {
            this.signType = signType;
        }

        public String getSubjectPrefix() {
            return subjectPrefix;
        }

        public void setSubjectPrefix(String subjectPrefix) {
            this.subjectPrefix = subjectPrefix;
        }
    }
}
