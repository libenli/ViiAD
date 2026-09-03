package com.mrd.ad.business.device.dto;

import java.io.Serializable;
import java.util.Map;

public class ViitalkDeviceCommandAckRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;
    private String mzNumber;
    private String command;
    private String status;
    private String message;
    private Map<String, Object> payload;
    private Long timestamp;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
