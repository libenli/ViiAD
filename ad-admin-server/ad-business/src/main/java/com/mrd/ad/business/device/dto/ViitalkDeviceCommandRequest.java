package com.mrd.ad.business.device.dto;

import java.io.Serializable;
import java.util.Map;

public class ViitalkDeviceCommandRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mzNumber;
    private String command;
    private String content;
    private Map<String, Object> payload;

    public String getMzNumber() {
        return mzNumber;
    }

    public void setMzNumber(String mzNumber) {
        this.mzNumber = mzNumber;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}