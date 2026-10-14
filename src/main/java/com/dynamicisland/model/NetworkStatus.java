package com.dynamicisland.model;

public class NetworkStatus {
    private int wifiLevel = 1; // 1 = ok, 2 = worse, 3 = even worse, 4 = no internet
    private boolean connected = true;
    private boolean airplaneMode = false;

    public NetworkStatus() {}

    public NetworkStatus(int wifiLevel, boolean connected, boolean airplaneMode) {
        this.wifiLevel = Math.clamp(wifiLevel, 1, 4);
        this.connected = connected;
        this.airplaneMode = airplaneMode;
    }

    public int getWifiLevel() {
        return wifiLevel;
    }

    public void setWifiLevel(int wifiLevel) {
        this.wifiLevel = Math.clamp(wifiLevel, 1, 4);
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isAirplaneMode() {
        return airplaneMode;
    }

    public void setAirplaneMode(boolean airplaneMode) {
        this.airplaneMode = airplaneMode;
    }

    // Resolves SVG file name based on wifi level and light/dark theme
    // wifi1-4 are white (for dark background)
    // wifi5-8 are black (for light background)
    public String getWifiSvgName(boolean isLightBackground) {
        int base = isLightBackground ? 4 : 0;
        int index = base + wifiLevel;
        return "wifi" + index + ".svg";
    }

    public String getAirplaneSvgName(boolean isLightBackground) {
        return isLightBackground ? "airplane_black.svg" : "airplane_white.svg";
    }
}
