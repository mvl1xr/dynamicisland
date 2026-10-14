package com.dynamicisland.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FontGrid64Renderer {
    private static final Map<String, CachedText> textCache = new ConcurrentHashMap<>();

    private static class CachedText {
        final BufferedImage image;
        final int baselineY;

        CachedText(BufferedImage image, int baselineY) {
            this.image = image;
            this.baselineY = baselineY;
        }
    }

    /**
     * Renders text using 64-grid 2x super-sampled rasterization,
     * ensuring perfectly smooth, non-pixelated curves and edges.
     */
    public static void drawString64(Graphics2D g, String text, Font font, Color color, float x, float baselineY) {
        if (text == null || text.isEmpty()) return;

        String key = text + "_" + font.getName() + "_" + font.getSize2D() + "_" + color.getRGB();
        CachedText cached = textCache.computeIfAbsent(key, k -> {
            // Measure at 2x grid
            float scale = 2.0f;
            Font hiFont = font.deriveFont(font.getSize2D() * scale);

            BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gD = dummy.createGraphics();
            gD.setFont(hiFont);
            FontMetrics fm = gD.getFontMetrics();
            int w = Math.max(1, fm.stringWidth(text) + 8);
            int h = Math.max(1, fm.getHeight() + 8);
            int base = fm.getAscent() + 4;
            gD.dispose();

            BufferedImage textImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gT = textImg.createGraphics();
            gT.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gT.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gT.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gT.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            gT.setFont(hiFont);
            gT.setColor(color);
            gT.drawString(text, 4, base);
            gT.dispose();

            return new CachedText(textImg, base);
        });

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        float drawW = cached.image.getWidth() / 2.0f;
        float drawH = cached.image.getHeight() / 2.0f;
        float drawX = x - 2f;
        float drawY = baselineY - (cached.baselineY / 2.0f);

        g2.drawImage(cached.image, (int) drawX, (int) drawY, (int) drawW, (int) drawH, null);
        g2.dispose();
    }
}
