package com.mrd.ad.business.system.dto;

public class AuthCaptchaResponse {

    private String uuid;
    private String image;

    public AuthCaptchaResponse() {
    }

    public AuthCaptchaResponse(String uuid, String image) {
        this.uuid = uuid;
        this.image = image;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
