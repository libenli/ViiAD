package com.mrd.ad.business.device.dto;

import java.io.Serializable;
import java.util.Date;

public class ViitalkDeviceOnlineSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jid;
    private String username;
    private String mzNumber;
    private String ip;
    private String appVer;
    private String proVer;
    private String presenceState;
    private Date onlineTime;
    private Integer priority;
    private Byte presenceType;
    private Byte presenceShow;
    private Boolean secure;

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMzNumber() {
        return mzNumber;
    }

    public void setMzNumber(String mzNumber) {
        this.mzNumber = mzNumber;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAppVer() {
        return appVer;
    }

    public void setAppVer(String appVer) {
        this.appVer = appVer;
    }

    public String getProVer() {
        return proVer;
    }

    public void setProVer(String proVer) {
        this.proVer = proVer;
    }

    public String getPresenceState() {
        return presenceState;
    }

    public void setPresenceState(String presenceState) {
        this.presenceState = presenceState;
    }

    public Date getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(Date onlineTime) {
        this.onlineTime = onlineTime;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Byte getPresenceType() {
        return presenceType;
    }

    public void setPresenceType(Byte presenceType) {
        this.presenceType = presenceType;
    }

    public Byte getPresenceShow() {
        return presenceShow;
    }

    public void setPresenceShow(Byte presenceShow) {
        this.presenceShow = presenceShow;
    }

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }
}