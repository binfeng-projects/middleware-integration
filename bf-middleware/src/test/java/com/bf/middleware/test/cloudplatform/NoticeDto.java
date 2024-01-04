package com.bf.middleware.test.cloudplatform;

import java.io.Serializable;

public class NoticeDto implements Serializable {
    private String text;
    private String mobiles;

    public String getMobiles() {
        return mobiles;
    }

    public void setMobiles(String mobiles) {
        this.mobiles = mobiles;
    }

    private String appName;
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
