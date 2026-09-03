package com.mrd.ad.business.device.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mrd.mq")
public class ViiAdMqProperties {

    private boolean enabled = false;
    private String host = "127.0.0.1";
    private int port = 5672;
    private String username = "guest";
    private String password = "guest";
    private String viiadC2sRule = "{\"exchangeType\":\"direct\",\"exchange\":\"mysher.exchange.direct.command.c2s\",\"binding\":\"vii_ad.c2s\",\"queueName\":\"mysher.server.viiad.c2s\"}";
    private boolean onlyChinaDevice = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getViiadC2sRule() {
        return viiadC2sRule;
    }

    public void setViiadC2sRule(String viiadC2sRule) {
        this.viiadC2sRule = viiadC2sRule;
    }

    public boolean isOnlyChinaDevice() {
        return onlyChinaDevice;
    }

    public void setOnlyChinaDevice(boolean onlyChinaDevice) {
        this.onlyChinaDevice = onlyChinaDevice;
    }
}
