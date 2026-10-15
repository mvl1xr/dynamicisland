package com.dynamicisland.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiquidGlassRenderer {
    public enum BackgroundStyle {
        LIQUID_GLASS,
        SOLID_BLACK
    }

    private static BackgroundStyle currentStyle = BackgroundStyle.LIQUID_GLASS;
    private static final Map<String, BufferedImage> glassCache = new ConcurrentHashMap<>();

    public static BackgroundStyle getBackgroundStyle() {
        return currentStyle;
    }

    public static void setBackgroundStyle(BackgroundStyle style) {
        currentStyle = style;
    }

    public static void toggleBackgroundStyle() {
        if (currentStyle == BackgroundStyle.LIQUID_GLASS) {
            currentStyle = BackgroundStyle.SOLID_BLACK;
        } else {
            currentStyle = BackgroundStyle.LIQUID_GLASS;
        }
    }

    private static float clamp01(float a) {
        return Math.max(0.0f, Math.min(1.0f, a));
    }

    /**
     * Exact Shader implementation based on Shadertoy GLSL:
     * - Rounded box SDF
     * - rb1: interior mask with smoothstep
     * - rb2: refractive border highlight
     * - rb3: gradient rim
     * - Specular curved liquid lighting
     * - Translucent liquid glass refraction body
     */
    public static void renderBackground(Graphics2D g, float x, float y, float w, float h) {
        if (currentStyle == BackgroundStyle.SOLID_BLACK) {
            g.setColor(Color.BLACK);
            RoundRectangle2D.Float capsule = new RoundRectangle2D.Float(x, y, w, h, h, h);
            g.fill(capsule);
            return;
        }

        int intW = Math.max(10, Math.round(w));
        int intH = Math.max(10, Math.round(h));
        String key = intW + "x" + intH;

        BufferedImage glassImg = glassCache.computeIfAbsent(key, k -> {
            BufferedImage img = new BufferedImage(intW, intH, BufferedImage.TYPE_INT_ARGB);
            int[] pixels = new int[intW * intH];

            float irX = intW;
            float irY = intH;
            float radius = 0.5f; // capsule corner radius
            float halfH = 0.5f;
            float halfW = (irX / irY) * 0.5f;
            float capStraightHalfW = Math.max(0.0f, halfW - halfH);

            float sharp = 0.045f;
            float borderThick = 1.0f / irY;
            float gradThick = 4.0f / irY;

            for (int py = 0; py < intH; py++) {
                float uvY = (py + 0.5f) / irY;
                float m2Y = uvY - 0.5f;

                for (int px = 0; px < intW; px++) {
                    float uvX = (px + 0.5f) / irX;
                    float m2X = (uvX - 0.5f) * (irX / irY);

                    // Box SDF with rounded corner radius
                    float qx = Math.abs(m2X) - capStraightHalfW;
                    float qy = Math.abs(m2Y) - (halfH - radius);
                    float d = (float) Math.hypot(Math.max(qx, 0.0f), Math.max(qy, 0.0f))
                            + Math.min(Math.max(qx, qy), 0.0f) - radius;

                    // rb1: box interior mask
                    float rb1 = clamp01(-d / sharp);

                    // rb2: glowing refractive border
                    float rb2 = clamp01(-(d + borderThick) / sharp) - clamp01(-d / sharp);

                    // rb3: gradient rim
                    float rb3 = clamp01(-(d + gradThick) / sharp) - clamp01(-(d - gradThick) / sharp);

                    float transition = rb1 * rb1 * (3.0f - 2.0f * rb1); // smoothstep(0, 1, rb1)

                    if (transition <= 0.001f) {
                        pixels[py * intW + px] = 0;
                        continue;
                    }

                    // Lighting gradient from user shader
                    float term1 = clamp01(Math.min(0.2f, Math.max(0.0f, m2Y)) + 0.1f) * 0.5f;
                    float term2 = clamp01(Math.min(0.2f, Math.max(-1.0f, -m2Y)) * rb3 + 0.1f) * 0.5f;
                    float gradient = term1 + term2;

                    // Specular gloss on upper half (liquid lens reflection)
                    float topGleam = clamp01(-m2Y * 2.0f) * rb1 * 0.35f;

                    // Sleek translucent glass base (translucent, not opaque black!)
                    float baseAlpha = 0.65f; // Translucent acrylic glass
                    float alpha = clamp01((baseAlpha * rb1 + rb2 * 0.85f + gradient * 0.45f + topGleam * 0.4f) * transition);

                    // Color calculation with glowing border & specular highlights
                    float highlight = (rb2 * 230f) + (gradient * 140f) + (topGleam * 180f);
                    int red = Math.min(255, (int) (14f * rb1 + highlight));
                    int green = Math.min(255, (int) (16f * rb1 + highlight));
                    int blue = Math.min(255, (int) (22f * rb1 + highlight + 8f));
                    int alphaVal = (int) (alpha * 255f);

                    pixels[py * intW + px] = (alphaVal << 24) | (red << 16) | (green << 8) | blue;
                }
            }

            img.setRGB(0, 0, intW, intH, pixels, 0, intW);
            return img;
        });

        g.drawImage(glassImg, (int) x, (int) y, null);
    }
}
