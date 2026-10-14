package com.dynamicisland.ui;

import com.dynamicisland.audio.SoundPlayer;
import com.dynamicisland.model.IslandMode;
import com.dynamicisland.model.MediaInfo;
import com.dynamicisland.model.NetworkStatus;
import com.dynamicisland.model.NotificationInfo;
import com.dynamicisland.render.EqualizerVisualizer;
import com.dynamicisland.render.FontGrid64Renderer;
import com.dynamicisland.render.FontManager;
import com.dynamicisland.render.LiquidGlassRenderer;
import com.dynamicisland.render.PhotoGrid64Renderer;
import com.dynamicisland.render.SvgRenderer;
import com.dynamicisland.util.BrightnessDetector;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DynamicIslandPanel extends JPanel {
    private IslandMode currentMode = IslandMode.IDLE;
    private MediaInfo currentMedia = new MediaInfo();
    private NotificationInfo currentNotification = null;
    private NetworkStatus currentNetwork = new NetworkStatus(1, true, false);

    // Fixed static height (only width animates)
    private static final float FIXED_HEIGHT = 35f;
    private float currentWidth = 120f;
    private float targetWidth = 120f;

    // Animations: Slide and Fade
    private float contentAlpha = 1.0f;
    private float targetContentAlpha = 1.0f;
    private float slideX = 0f;
    private float targetSlideX = 0f;

    // Equalizer
    private final EqualizerVisualizer equalizer = new EqualizerVisualizer();

    // Time & Marquee
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private float marqueeOffset = 0f;
    private long notificationExpireTime = 0;

    // Colors
    private static final Color DARK_THEME_COLOR = Color.WHITE;
    private static final Color LIGHT_THEME_COLOR = new Color(20, 20, 20);

    public DynamicIslandPanel() {
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));

        // 60 FPS animation loop
        Timer animTimer = new Timer(16, e -> updateFrame());
        animTimer.start();
    }

    private void updateFrame() {
        float dt = 0.016f;

        // 1. Check notification expiry
        if (currentMode == IslandMode.NOTIFICATION) {
            if (System.currentTimeMillis() > notificationExpireTime) {
                if (currentMedia != null && currentMedia.isPlaying()) {
                    setMode(IslandMode.MUSIC);
                } else {
                    setMode(IslandMode.IDLE);
                }
            }
        }

        // 2. Interpolate ONLY width (smooth easing)
        float lerpSpeed = 0.18f;
        currentWidth += (targetWidth - currentWidth) * lerpSpeed;

        // 3. Interpolate content fade & slide
        contentAlpha += (targetContentAlpha - contentAlpha) * 0.22f;
        slideX += (targetSlideX - slideX) * 0.20f;

        // 4. Update Brightness fade
        BrightnessDetector.updateFade(dt);

        // 5. Update Equalizer
        boolean isMusicActive = (currentMode == IslandMode.MUSIC && currentMedia != null && currentMedia.isPlaying());
        equalizer.update(isMusicActive, dt);

        // 6. Marquee offset
        if (isMusicActive) {
            marqueeOffset += 0.5f;
        } else {
            marqueeOffset = 0f;
        }

        repaint();
    }

    public void setMedia(MediaInfo media) {
        this.currentMedia = media;
        if (media != null && media.getGradientColors() != null) {
            equalizer.setColors(media.getGradientColors());
        }

        if (currentMode != IslandMode.NOTIFICATION) {
            if (media != null && !media.getTitle().isEmpty()) {
                setMode(IslandMode.MUSIC);
            } else {
                setMode(IslandMode.IDLE);
            }
        } else {
            updateTargetDimensions();
        }
    }

    public void setNotification(NotificationInfo notif) {
        this.currentNotification = notif;
        notificationExpireTime = System.currentTimeMillis() + 4500;

        // Mute check: do NOT play sound if Telegram message is silent, or Discord message is background
        boolean shouldPlaySound = true;
        if (notif.isTelegram() && notif.isSilent()) {
            shouldPlaySound = false;
        }
        if (notif.isDiscord() && !notif.isMention()) {
            shouldPlaySound = false;
        }

        if (shouldPlaySound) {
            SoundPlayer.playDing();
        }
        setMode(IslandMode.NOTIFICATION);
    }

    public void setNetwork(NetworkStatus net) {
        this.currentNetwork = net;
    }

    private void setMode(IslandMode newMode) {
        if (this.currentMode != newMode) {
            this.currentMode = newMode;
            this.contentAlpha = 0.0f;
            this.slideX = 18f;
            this.targetSlideX = 0f;
            this.targetContentAlpha = 1.0f;
        }
        updateTargetDimensions();
    }

    private void updateTargetDimensions() {
        switch (currentMode) {
            case IDLE -> targetWidth = 110f;
            case MUSIC -> {
                String title = (currentMedia != null && !currentMedia.getTitle().isEmpty()) ? currentMedia.getTitle() : "Музыка";
                Font font = FontManager.getBold(13f);
                FontMetrics fm = getFontMetrics(font);
                int titleW = fm.stringWidth(title);
                int visibleTextW = Math.min(180, titleW);

                float coverSize = 18f;
                float gapCoverText = 7f;
                float gapTextEq = 7f;
                float eqW = equalizer.getTotalWidth(); // 17.8f
                float paddingLeft = 10f;
                float paddingRight = 10f;

                // Tightly wraps without extra empty tails!
                targetWidth = paddingLeft + coverSize + gapCoverText + visibleTextW + gapTextEq + eqW + paddingRight;
            }
            case NOTIFICATION -> {
                String app = (currentNotification != null) ? currentNotification.getAppName() : "Уведомление";
                String msg = (currentNotification != null && !currentNotification.getTitle().isEmpty())
                        ? currentNotification.getTitle()
                        : ((currentNotification != null) ? currentNotification.getText() : "");
                String full = msg.isEmpty() ? app : (app + " • " + msg);
                Font font = FontManager.getBold(13f);
                FontMetrics fm = getFontMetrics(font);
                int textW = Math.min(220, fm.stringWidth(full));

                float iconSize = 18f;
                targetWidth = 10f + iconSize + 8f + textW + 12f;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // 64-grid rendering hints for ultra-smooth anti-aliased output
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int panelWidth = getWidth();
        float centerX = panelWidth / 2f;
        float islandY = 4f;

        float islandW = currentWidth;
        float islandH = FIXED_HEIGHT;
        float islandX = centerX - (islandW / 2f);

        boolean isLightBg = BrightnessDetector.isLightBackground();
        Color statusColor = BrightnessDetector.getAdaptiveColor(DARK_THEME_COLOR, LIGHT_THEME_COLOR);

        // 1. Draw Left Status Area: Wi-Fi ONLY (24x24, no airplane)
        drawLeftStatus(g2, islandX, islandY + (islandH / 2f), isLightBg);

        // 2. Draw Dynamic Island Capsule (Solid black, strictly balanced symmetry)
        drawIslandCapsule(g2, islandX, islandY, islandW, islandH);

        // 3. Draw Right Status Area: Clock Time (Nunito-SemiBold, 64-grid rendered)
        drawRightStatus(g2, islandX + islandW, islandY + (islandH / 2f), statusColor);

        g2.dispose();
    }

    private void drawLeftStatus(Graphics2D g, float islandLeftX, float centerY, boolean isLightBg) {
        float iconSize = 24f;
        float startX = islandLeftX - iconSize - 16f;

        String wifiIcon = currentNetwork.getWifiSvgName(isLightBg);
        SvgRenderer.renderSvg(g, wifiIcon, startX, centerY - (iconSize / 2f), iconSize, iconSize, this);
    }

    private void drawRightStatus(Graphics2D g, float islandRightX, float centerY, Color textColor) {
        String timeStr = LocalTime.now().format(timeFormatter);
        Font font = FontManager.getSemiBold(14.5f);
        FontMetrics fm = g.getFontMetrics(font);
        float textX = islandRightX + 16f;
        float textY = centerY + (fm.getAscent() / 2f) - 2f;

        // 64-grid super-sampled font rendering
        FontGrid64Renderer.drawString64(g, timeStr, font, textColor, textX, textY);
    }

    private void drawIslandCapsule(Graphics2D g, float x, float y, float w, float h) {
        // Render capsule background (Solid Black or Liquid Glass Shader)
        LiquidGlassRenderer.renderBackground(g, x, y, w, h);

        RoundRectangle2D.Float capsule = new RoundRectangle2D.Float(x, y, w, h, h, h);

        // Clip inside capsule
        Graphics2D gClip = (Graphics2D) g.create();
        gClip.clip(capsule);

        // Slide & fade animation
        float alpha = Math.clamp(contentAlpha, 0f, 1f);
        gClip.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        gClip.translate(slideX, 0);

        float centerY = y + (h / 2f);

        switch (currentMode) {
            case MUSIC -> drawMusicContent(gClip, x, y, w, h, centerY);
            case NOTIFICATION -> drawNotificationContent(gClip, x, y, w, h, centerY);
            case IDLE -> drawIdleContent(gClip, x, y, w, h, centerY);
        }

        gClip.dispose();
    }

    private void drawMusicContent(Graphics2D g, float x, float y, float w, float h, float centerY) {
        float paddingLeft = 10f;
        float paddingRight = 10f;
        float coverSize = 18f;

        // 1. Album Cover Art with 64-grid progressive downsampling
        float coverX = x + paddingLeft;
        float coverY = centerY - (coverSize / 2f);

        BufferedImage coverImg = (currentMedia != null) ? currentMedia.getCover() : null;
        PhotoGrid64Renderer.render64GridPhoto(g, coverImg, coverX, coverY, coverSize, 4.5f);

        // 2. Equalizer placed with exact paddingRight from the right edge
        float eqW = equalizer.getTotalWidth();
        float eqX = x + w - paddingRight - eqW;
        equalizer.render(g, eqX, centerY);

        // 3. Track Title (Nunito-Bold, 64-grid rendered)
        String title = (currentMedia != null && !currentMedia.getTitle().isEmpty())
                ? currentMedia.getTitle()
                : "Воспроизведение";

        Font font = FontManager.getBold(13f);
        FontMetrics fm = g.getFontMetrics(font);
        int fullTitleW = fm.stringWidth(title);

        float textX = coverX + coverSize + 7f;
        float availableTextW = Math.max(10f, eqX - textX - 7f);
        float textY = centerY + (fm.getAscent() / 2f) - 2f;

        Graphics2D gText = (Graphics2D) g.create();
        gText.clipRect((int) textX, (int) y, (int) availableTextW, (int) h);

        if (fullTitleW > availableTextW + 5) {
            float loopW = fullTitleW + 30f;
            float curOffset = marqueeOffset % loopW;
            FontGrid64Renderer.drawString64(gText, title, font, Color.WHITE, textX - curOffset, textY);
            FontGrid64Renderer.drawString64(gText, title, font, Color.WHITE, textX - curOffset + loopW, textY);
        } else {
            FontGrid64Renderer.drawString64(gText, title, font, Color.WHITE, textX, textY);
        }
        gText.dispose();
    }

    private void drawNotificationContent(Graphics2D g, float x, float y, float w, float h, float centerY) {
        float paddingLeft = 10f;
        float iconSize = 18f;

        float iconX = x + paddingLeft;
        float iconY = centerY - (iconSize / 2f);

        // 1. App Icon with 64-grid multi-step downsampling
        BufferedImage appIcon = (currentNotification != null) ? currentNotification.getAppIcon() : null;
        if (appIcon != null) {
            PhotoGrid64Renderer.render64GridPhoto(g, appIcon, iconX, iconY, iconSize, 4.5f);
        } else {
            g.setColor(new Color(0, 122, 255));
            g.fill(new RoundRectangle2D.Float(iconX, iconY, iconSize, iconSize, 4.5f, 4.5f));
            g.setColor(Color.WHITE);
            Font f = FontManager.getBold(11f);
            String firstLetter = (currentNotification != null && !currentNotification.getAppName().isEmpty())
                    ? currentNotification.getAppName().substring(0, 1).toUpperCase()
                    : "!";
            FontGrid64Renderer.drawString64(g, firstLetter, f, Color.WHITE, iconX + 5f, centerY + 4f);
        }

        // 2. App Name & Notification Message with 64-grid font rendering
        float textX = iconX + iconSize + 8f;
        float maxTextW = x + w - textX - 10f;

        String appName = (currentNotification != null) ? currentNotification.getAppName() : "Уведомление";
        String extraText = (currentNotification != null && !currentNotification.getTitle().isEmpty())
                ? currentNotification.getTitle()
                : ((currentNotification != null) ? currentNotification.getText() : "");

        String displayStr = extraText.isEmpty() ? appName : (appName + " • " + extraText);

        Font font = FontManager.getBold(13f);
        FontMetrics fm = g.getFontMetrics(font);
        float textY = centerY + (fm.getAscent() / 2f) - 2f;

        Graphics2D gText = (Graphics2D) g.create();
        gText.clipRect((int) textX, (int) y, (int) maxTextW, (int) h);
        FontGrid64Renderer.drawString64(gText, displayStr, font, Color.WHITE, textX, textY);
        gText.dispose();
    }

    private void drawIdleContent(Graphics2D g, float x, float y, float w, float h, float centerY) {
        float dotSize = 9f;
        float sensorW = 11f;

        // Camera lens
        g.setColor(new Color(18, 18, 22));
        g.fillOval((int) (x + w - 20f), (int) (centerY - (dotSize / 2f)), (int) dotSize, (int) dotSize);

        // Reflection
        g.setColor(new Color(25, 30, 50));
        g.fillOval((int) (x + w - 18f), (int) (centerY - 2.5f), 3, 3);

        // Sensor
        g.setColor(new Color(14, 14, 18));
        g.fillRoundRect((int) (x + 12f), (int) (centerY - 3f), (int) sensorW, 6, 6, 6);
    }
}
