package com.mrd.ad.business.device.dto;

import java.io.Serializable;
import java.util.Date;

public class ViitalkDeviceCommandAckResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;
    private String mzNumber;
    private String command;
    private String status;
    private String message;
    private Date sendTime;
    private Date ackTime;

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

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public Date getAckTime() {
        return ackTime;
    }

    public void setAckTime(Date ackTime) {
        this.ackTime = ackTime;
    }
}
