package com.dynamicisland.ui;

import com.dynamicisland.audio.SoundPlayer;
import com.dynamicisland.model.IslandMode;
import com.dynamicisland.model.MediaInfo;
import com.dynamicisland.model.NetworkStatus;
import com.dynamicisland.model.NotificationInfo;
import com.dynamicisland.render.EqualizerVisualizer;
import com.dynamicisland.render.FontManager;
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
import java.awt.Image;
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

    // Dynamic island capsule geometry
    private float currentWidth = 160f;
    private float targetWidth = 160f;
    private float currentHeight = 36f;
    private float targetHeight = 36f;

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
                // Return to music or idle
                if (currentMedia != null && currentMedia.isPlaying()) {
                    setMode(IslandMode.MUSIC);
                } else {
                    setMode(IslandMode.IDLE);
                }
            }
        }

        // 2. Interpolate island geometry
        float lerpSpeed = 0.16f;
        currentWidth += (targetWidth - currentWidth) * lerpSpeed;
        currentHeight += (targetHeight - currentHeight) * lerpSpeed;

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

        // If not showing notification, adjust mode
        if (currentMode != IslandMode.NOTIFICATION) {
            if (media != null && !media.getTitle().isEmpty()) {
                setMode(IslandMode.MUSIC);
            } else {
                setMode(IslandMode.IDLE);
            }
        }
    }

    public void setNotification(NotificationInfo notif) {
        this.currentNotification = notif;
        notificationExpireTime = System.currentTimeMillis() + 4500; // 4.5 seconds
        SoundPlayer.playDing();
        setMode(IslandMode.NOTIFICATION);
    }

    public void setNetwork(NetworkStatus net) {
        this.currentNetwork = net;
    }

    private void setMode(IslandMode newMode) {
        if (this.currentMode != newMode) {
            this.currentMode = newMode;
            // Trigger slide & fade transition
            this.contentAlpha = 0.0f;
            this.slideX = 25f; // slide in from right
            this.targetSlideX = 0f;
            this.targetContentAlpha = 1.0f;
        }

        // Determine target island dimensions
        switch (currentMode) {
            case IDLE -> {
                targetWidth = 170f;
                targetHeight = 36f;
            }
            case MUSIC -> {
                String title = (currentMedia != null) ? currentMedia.getTitle() : "Музыка";
                Font font = FontManager.getBold(13.5f);
                FontMetrics fm = getFontMetrics(font);
                int textW = Math.min(220, fm.stringWidth(title));
                // cover (28) + gap(10) + text + gap(14) + equalizer(32) + padding(28)
                targetWidth = Math.max(260f, Math.min(420f, 28 + 10 + textW + 14 + equalizer.getTotalWidth() + 32));
                targetHeight = 38f;
            }
            case NOTIFICATION -> {
                String app = (currentNotification != null) ? currentNotification.getAppName() : "Уведомление";
                String msg = (currentNotification != null && !currentNotification.getTitle().isEmpty())
                        ? currentNotification.getTitle()
                        : ((currentNotification != null) ? currentNotification.getText() : "");
                Font font = FontManager.getBold(13.5f);
                FontMetrics fm = getFontMetrics(font);
                int textW = Math.min(240, fm.stringWidth(app + (msg.isEmpty() ? "" : ": " + msg)));
                targetWidth = Math.max(300f, Math.min(460f, 28 + 12 + textW + 36));
                targetHeight = 42f;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // High quality rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        float centerX = panelWidth / 2f;
        float islandY = 6f;

        float islandW = currentWidth;
        float islandH = currentHeight;
        float islandX = centerX - (islandW / 2f);

        boolean isLightBg = BrightnessDetector.isLightBackground();
        Color statusColor = BrightnessDetector.getAdaptiveColor(DARK_THEME_COLOR, LIGHT_THEME_COLOR);

        // 1. Draw Left Status Area: Airplane + WiFi
        drawLeftStatus(g2, islandX, islandY + (islandH / 2f), isLightBg);

        // 2. Draw Dynamic Island Capsule (SOLID BLACK, NO TRANSPARENCY)
        drawIslandCapsule(g2, islandX, islandY, islandW, islandH);

        // 3. Draw Right Status Area: Clock Time (Nunito-Medium)
        drawRightStatus(g2, islandX + islandW, islandY + (islandH / 2f), statusColor);

        g2.dispose();
    }

    private void drawLeftStatus(Graphics2D g, float islandLeftX, float centerY, boolean isLightBg) {
        float iconSize = 17f;
        float gap = 7f;
        float totalLeftW = (iconSize * 2) + gap;
        float startX = islandLeftX - totalLeftW - 14f;

        // Render Airplane Icon
        String airplaneIcon = currentNetwork.getAirplaneSvgName(isLightBg);
        SvgRenderer.renderSvg(g, airplaneIcon, startX, centerY - (iconSize / 2f), iconSize, iconSize, this);

        // Render WiFi Icon (wifi1-4 if dark bg, wifi5-8 if light bg)
        String wifiIcon = currentNetwork.getWifiSvgName(isLightBg);
        SvgRenderer.renderSvg(g, wifiIcon, startX + iconSize + gap, centerY - (iconSize / 2f), iconSize, iconSize, this);
    }

    private void drawRightStatus(Graphics2D g, float islandRightX, float centerY, Color textColor) {
        String timeStr = LocalTime.now().format(timeFormatter);
        Font font = FontManager.getMedium(14.5f);
        g.setFont(font);
        g.setColor(textColor);

        FontMetrics fm = g.getFontMetrics();
        float textX = islandRightX + 14f;
        float textY = centerY + (fm.getAscent() / 2f) - 2f;

        g.drawString(timeStr, textX, textY);
    }

    private void drawIslandCapsule(Graphics2D g, float x, float y, float w, float h) {
        // Pure solid black rounded pill (no transparency)
        g.setColor(Color.BLACK);
        RoundRectangle2D.Float capsule = new RoundRectangle2D.Float(x, y, w, h, h, h);
        g.fill(capsule);

        // Subtle elegant border rim
        g.setColor(new Color(255, 255, 255, 18));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(capsule);

        // Clip inside capsule for content
        Graphics2D gClip = (Graphics2D) g.create();
        gClip.clip(capsule);

        // Apply slide & fade animation
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
        float paddingLeft = 7f;
        float paddingRight = 14f;
        float coverSize = 26f;

        // 1. Album Cover Art
        float coverX = x + paddingLeft;
        float coverY = centerY - (coverSize / 2f);

        BufferedImage coverImg = (currentMedia != null) ? currentMedia.getCover() : null;
        if (coverImg != null) {
            // Rounded cover art
            Graphics2D gCover = (Graphics2D) g.create();
            RoundRectangle2D.Float coverClip = new RoundRectangle2D.Float(coverX, coverY, coverSize, coverSize, 7, 7);
            gCover.clip(coverClip);
            gCover.drawImage(coverImg, (int) coverX, (int) coverY, (int) coverSize, (int) coverSize, null);
            gCover.dispose();
        } else {
            // Modern fallback music icon / placeholder
            g.setColor(new Color(40, 40, 45));
            g.fill(new RoundRectangle2D.Float(coverX, coverY, coverSize, coverSize, 7, 7));
            g.setColor(new Color(255, 45, 85));
            g.fillOval((int) coverX + 8, (int) coverY + 7, 10, 10);
        }

        // 2. Equalizer (6 thin gradient bars) on the right
        float eqW = equalizer.getTotalWidth();
        float eqX = x + w - paddingRight - eqW;
        equalizer.render(g, eqX, centerY);

        // 3. Track Title in Nunito-Bold (White color)
        float textX = coverX + coverSize + 9f;
        float maxTextW = eqX - textX - 8f;

        if (maxTextW > 20) {
            String title = (currentMedia != null && !currentMedia.getTitle().isEmpty())
                    ? currentMedia.getTitle()
                    : "Воспроизведение";

            Font font = FontManager.getBold(13.5f);
            g.setFont(font);
            g.setColor(Color.WHITE);

            FontMetrics fm = g.getFontMetrics();
            int titleW = fm.stringWidth(title);

            float textY = centerY + (fm.getAscent() / 2f) - 2f;

            Graphics2D gText = (Graphics2D) g.create();
            gText.clipRect((int) textX, (int) y, (int) maxTextW, (int) h);

            if (titleW > maxTextW) {
                // Marquee scrolling
                float loopW = titleW + 40f;
                float curOffset = marqueeOffset % loopW;
                gText.drawString(title, textX - curOffset, textY);
                gText.drawString(title, textX - curOffset + loopW, textY);
            } else {
                gText.drawString(title, textX, textY);
            }
            gText.dispose();
        }
    }

    private void drawNotificationContent(Graphics2D g, float x, float y, float w, float h, float centerY) {
        float paddingLeft = 8f;
        float paddingRight = 14f;
        float iconSize = 26f;

        float iconX = x + paddingLeft;
        float iconY = centerY - (iconSize / 2f);

        // 1. App Icon / Logo
        BufferedImage appIcon = (currentNotification != null) ? currentNotification.getAppIcon() : null;
        if (appIcon != null) {
            Graphics2D gIcon = (Graphics2D) g.create();
            RoundRectangle2D.Float iconClip = new RoundRectangle2D.Float(iconX, iconY, iconSize, iconSize, 8, 8);
            gIcon.clip(iconClip);
            gIcon.drawImage(appIcon, (int) iconX, (int) iconY, (int) iconSize, (int) iconSize, null);
            gIcon.dispose();
        } else {
            // Elegant app icon badge
            g.setColor(new Color(0, 122, 255));
            g.fill(new RoundRectangle2D.Float(iconX, iconY, iconSize, iconSize, 8, 8));
            g.setColor(Color.WHITE);
            Font f = FontManager.getBold(13f);
            g.setFont(f);
            String firstLetter = (currentNotification != null && !currentNotification.getAppName().isEmpty())
                    ? currentNotification.getAppName().substring(0, 1).toUpperCase()
                    : "!";
            g.drawString(firstLetter, iconX + 8f, centerY + 5f);
        }

        // 2. App Name & Notification Message in Nunito-Bold (White color)
        float textX = iconX + iconSize + 9f;
        float maxTextW = x + w - textX - paddingRight;

        String appName = (currentNotification != null) ? currentNotification.getAppName() : "Уведомление";
        String extraText = (currentNotification != null && !currentNotification.getTitle().isEmpty())
                ? currentNotification.getTitle()
                : ((currentNotification != null) ? currentNotification.getText() : "");

        String displayStr = extraText.isEmpty() ? appName : (appName + " • " + extraText);

        Font font = FontManager.getBold(13f);
        g.setFont(font);
        g.setColor(Color.WHITE);

        FontMetrics fm = g.getFontMetrics();
        float textY = centerY + (fm.getAscent() / 2f) - 2f;

        Graphics2D gText = (Graphics2D) g.create();
        gText.clipRect((int) textX, (int) y, (int) maxTextW, (int) h);
        gText.drawString(displayStr, textX, textY);
        gText.dispose();
    }

    private void drawIdleContent(Graphics2D g, float x, float y, float w, float h, float centerY) {
        // iPhone-like subtle camera / sensor cutouts inside the island
        float dotSize = 10f;
        float sensorW = 12f;

        // Camera lens (right circle)
        g.setColor(new Color(15, 15, 20));
        g.fillOval((int) (x + w - 24f), (int) (centerY - (dotSize / 2f)), (int) dotSize, (int) dotSize);

        // Subtle lens reflection
        g.setColor(new Color(20, 25, 45));
        g.fillOval((int) (x + w - 22f), (int) (centerY - 3f), 4, 4);

        // FaceID / Ambient sensor (left pill)
        g.setColor(new Color(12, 12, 15));
        g.fillRoundRect((int) (x + 14f), (int) (centerY - 3.5f), (int) sensorW, 7, 7, 7);
    }
}
