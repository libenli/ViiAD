package com.mrd.ad.business.device.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ViitalkDeviceOnlineStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mzNumber;
    private boolean online;
    private long onlineCount;
    private List<ViitalkDeviceOnlineSession> sessions = new ArrayList<ViitalkDeviceOnlineSession>();

    public String getMzNumber() {
        return mzNumber;
    }

    public void setMzNumber(String mzNumber) {
        this.mzNumber = mzNumber;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(long onlineCount) {
        this.onlineCount = onlineCount;
    }

    public List<ViitalkDeviceOnlineSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<ViitalkDeviceOnlineSession> sessions) {
        this.sessions = sessions;
    }
}