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

    private static BackgroundStyle currentStyle = BackgroundStyle.SOLID_BLACK;
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
     * - LIQUID_GLASS: glass refraction, specular highlight gradient, frosted blur, and refractive rim.
     */
    public static void renderBackground(Graphics2D g, float x, float y, float w, float h) {
        if (currentStyle == BackgroundStyle.SOLID_BLACK) {
            g.setColor(Color.BLACK);
            RoundRectangle2D.Float capsule = new RoundRectangle2D.Float(x, y, w, h, h, h);
            g.fill(capsule);

            // Subtle dark rim
            g.setColor(new Color(255, 255, 255, 20));
            g.setStroke(new BasicStroke(0.8f));
            g.draw(capsule);
            return;
        }

        // LIQUID GLASS SHADER RENDERING
        int intW = Math.round(w);
        int intH = Math.round(h);
        String key = intW + "x" + intH;

        BufferedImage glassImg = glassCache.computeIfAbsent(key, k -> {
            BufferedImage img = new BufferedImage(intW, intH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0, 0, intW, intH, intH, intH);

            // 1. Frosted dark glass base (translucent refractive tint)
            g2.setColor(new Color(12, 14, 20, 210));
            g2.fill(shape);

            // 2. Specular lighting gradient (m2.y lighting from shader)
            GradientPaint specGrad = new GradientPaint(
                0, 0, new Color(255, 255, 255, 75),
                0, intH * 0.55f, new Color(255, 255, 255, 5)
            );
            g2.setPaint(specGrad);
            g2.fill(shape);

            // 3. Shadow gradient inside (rb3 from shader)
            GradientPaint shadowGrad = new GradientPaint(
                0, intH * 0.6f, new Color(0, 0, 0, 0),
                0, intH, new Color(0, 0, 0, 95)
            );
            g2.setPaint(shadowGrad);
            g2.fill(shape);

            // 4. Refractive glass borders (rb2 from shader: border highlight)
            g2.setStroke(new BasicStroke(1.2f));
            GradientPaint borderGrad = new GradientPaint(
                0, 0, new Color(255, 255, 255, 160),
                0, intH, new Color(255, 255, 255, 30)
            );
            g2.setPaint(borderGrad);
            g2.draw(shape);

            // Inner subtle refraction edge
            RoundRectangle2D.Float inner = new RoundRectangle2D.Float(1, 1, intW - 2, intH - 2, intH - 2, intH - 2);
            g2.setColor(new Color(255, 255, 255, 25));
            g2.setStroke(new BasicStroke(0.6f));
            g2.draw(inner);

            g2.dispose();
            return img;
        });

        g.drawImage(glassImg, (int) x, (int) y, null);
    }
}
