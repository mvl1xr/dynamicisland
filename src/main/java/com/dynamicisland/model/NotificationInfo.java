package com.dynamicisland.model;

import java.awt.image.BufferedImage;

public class NotificationInfo {
    private String appName = "";
    private String title = "";
    private String text = "";
    private BufferedImage appIcon = null;
    private long timestamp = System.currentTimeMillis();
    private boolean isTelegram = false;
    private boolean isSilent = false;
    private boolean isDiscord = false;
    private boolean isMention = false;

    public NotificationInfo() {}

    public NotificationInfo(String appName, String title, String text, BufferedImage appIcon) {
        this(appName, title, text, appIcon, false, false, false, false);
    }

    public NotificationInfo(String appName, String title, String text, BufferedImage appIcon,
                            boolean isTelegram, boolean isSilent, boolean isDiscord, boolean isMention) {
        this.appName = appName != null ? appName : "Уведомление";
        this.title = title != null ? title : "";
        this.text = text != null ? text : "";
        this.appIcon = appIcon;
        this.timestamp = System.currentTimeMillis();
        this.isTelegram = isTelegram;
        this.isSilent = isSilent;
        this.isDiscord = isDiscord;
        this.isMention = isMention;
    }

    public String getAppName() {
        return appName;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public BufferedImage getAppIcon() {
        return appIcon;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isTelegram() {
        return isTelegram;
    }

    public boolean isSilent() {
        return isSilent;
    }

    public boolean isDiscord() {
        return isDiscord;
    }

    public boolean isMention() {
        return isMention;
    }
}
