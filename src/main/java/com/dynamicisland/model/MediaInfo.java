package com.dynamicisland.model;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class MediaInfo {
    private String title = "";
    private String artist = "";
    private boolean isPlaying = false;
    private BufferedImage cover = null;
    private Color[] gradientColors = null;

    public MediaInfo() {}

    public MediaInfo(String title, String artist, boolean isPlaying, BufferedImage cover, Color[] gradientColors) {
        this.title = title != null ? title : "";
        this.artist = artist != null ? artist : "";
        this.isPlaying = isPlaying;
        this.cover = cover;
        this.gradientColors = gradientColors;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public BufferedImage getCover() {
        return cover;
    }

    public Color[] getGradientColors() {
        return gradientColors;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public void setArtist(String artist) {
        this.artist = artist != null ? artist : "";
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public void setCover(BufferedImage cover) {
        this.cover = cover;
    }

    public void setGradientColors(Color[] gradientColors) {
        this.gradientColors = gradientColors;
    }
}
