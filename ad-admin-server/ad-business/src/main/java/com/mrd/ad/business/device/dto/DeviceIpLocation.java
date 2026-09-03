package com.mrd.ad.business.device.dto;

public class DeviceIpLocation {

    private String ip;
    private String countryName;
    private String provinceName;
    private String cityName;
    private String regionName;
    private boolean resolved;

    public DeviceIpLocation() {
    }

    public DeviceIpLocation(String ip, String countryName, String provinceName, String cityName, String regionName, boolean resolved) {
        this.ip = ip;
        this.countryName = countryName;
        this.provinceName = provinceName;
        this.cityName = cityName;
        this.regionName = regionName;
        this.resolved = resolved;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
