package com.mrd.ad.business.system.dto;

public class SysDictOption {

    private String label;
    private String value;
    private String type;

    public SysDictOption() {
    }

    public SysDictOption(String label, String value, String type) {
        this.label = label;
        this.value = value;
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
