package com.dynamicisland.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiquidGlassRenderer {
    public enum BackgroundStyle {
        SOLID_BLACK,
        LIQUID_GLASS
    }

    // Default to Liquid Glass as requested!
    private static BackgroundStyle currentStyle = BackgroundStyle.LIQUID_GLASS;
    private static final Map<String, BufferedImage> glassCache = new ConcurrentHashMap<>();

    public static BackgroundStyle getBackgroundStyle() {
        return currentStyle;
    }

    public static void setBackgroundStyle(BackgroundStyle style) {
        currentStyle = style;
    }

    public static void toggleBackgroundStyle() {
        if (currentStyle == BackgroundStyle.SOLID_BLACK) {
            currentStyle = BackgroundStyle.LIQUID_GLASS;
        } else {
            currentStyle = BackgroundStyle.SOLID_BLACK;
        }
    }

    /**
     * Renders the capsule background according to current style:
     * - SOLID_BLACK: pure solid black capsule.
     * - LIQUID_GLASS: pure liquid glass with refraction, specular lighting, and refractive edge.
     */
    public static void renderBackground(Graphics2D g, float x, float y, float w, float h) {
        if (currentStyle == BackgroundStyle.SOLID_BLACK) {
            g.setColor(Color.BLACK);
            RoundRectangle2D.Float capsule = new RoundRectangle2D.Float(x, y, w, h, h, h);
            g.fill(capsule);

            g.setColor(new Color(255, 255, 255, 20));
            g.setStroke(new BasicStroke(0.8f));
            g.draw(capsule);
            return;
        }

        // PURE LIQUID GLASS SHADER RENDERING
        int intW = Math.max(10, Math.round(w));
        int intH = Math.max(10, Math.round(h));
        String key = intW + "x" + intH;

        BufferedImage glassImg = glassCache.computeIfAbsent(key, k -> {
            BufferedImage img = new BufferedImage(intW, intH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0, 0, intW, intH, intH, intH);

            // 1. Pure liquid glass body: sleek dark translucent tint
            g2.setColor(new Color(15, 17, 24, 185));
            g2.fill(shape);

            // 2. Liquid specular gloss reflection (from shader rb1 * gradient)
            GradientPaint specGrad = new GradientPaint(
                0, 0, new Color(255, 255, 255, 110),
                0, intH * 0.5f, new Color(255, 255, 255, 15)
            );
            g2.setPaint(specGrad);
            g2.fill(shape);

            // 3. Shadow / refraction gradient inside bottom (from shader rb3)
            GradientPaint shadowGrad = new GradientPaint(
                0, intH * 0.5f, new Color(0, 0, 0, 0),
                0, intH, new Color(0, 0, 0, 80)
            );
            g2.setPaint(shadowGrad);
            g2.fill(shape);

            // 4. Glowing refractive liquid borders (from shader rb2)
            g2.setStroke(new BasicStroke(1.2f));
            GradientPaint borderGrad = new GradientPaint(
                0, 0, new Color(255, 255, 255, 190),
                0, intH, new Color(255, 255, 255, 45)
            );
            g2.setPaint(borderGrad);
            g2.draw(shape);

            // 5. Subtle inner refraction highlight line
            RoundRectangle2D.Float inner = new RoundRectangle2D.Float(1, 1, intW - 2, intH - 2, intH - 2, intH - 2);
            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(0.6f));
            g2.draw(inner);

            g2.dispose();
            return img;
        });

        g.drawImage(glassImg, (int) x, (int) y, null);
    }
}
