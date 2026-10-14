package com.dynamicisland.render;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ColorExtractor {
    private static final Color DEFAULT_START = new Color(255, 45, 85);   // iOS Neon Pink
    private static final Color DEFAULT_END = new Color(0, 122, 255);     // iOS Neon Blue

    public static Color[] extractGradient(BufferedImage image, int barCount) {
        Color[] bars = new Color[barCount];
        if (image == null) {
            return generateInterpolation(DEFAULT_START, DEFAULT_END, barCount);
        }

        try {
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) {
                return generateInterpolation(DEFAULT_START, DEFAULT_END, barCount);
            }

            // Sample colors from quadrants and center
            List<Color> samples = new ArrayList<>();
            int stepX = Math.max(1, width / 8);
            int stepY = Math.max(1, height / 8);

            for (int y = stepY; y < height; y += stepY) {
                for (int x = stepX; x < width; x += stepX) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    samples.add(new Color(r, g, b));
                }
            }

            // Find two most vibrant/distinct colors
            Color primary = findMostVibrant(samples, null);
            Color secondary = findMostVibrant(samples, primary);

            if (primary == null) primary = DEFAULT_START;
            if (secondary == null || colorDistance(primary, secondary) < 40) {
                // Generate complementary or shifted color for vibrant contrast
                float[] hsb = Color.RGBtoHSB(primary.getRed(), primary.getGreen(), primary.getBlue(), null);
                secondary = Color.getHSBColor((hsb[0] + 0.35f) % 1.0f, Math.min(1.0f, hsb[1] + 0.2f), Math.max(0.7f, hsb[2]));
            }

            // Ensure bars are adequately bright and saturated to look stunning on black background
            primary = boostForBlackBackground(primary);
            secondary = boostForBlackBackground(secondary);

            return generateInterpolation(primary, secondary, barCount);
        } catch (Exception e) {
            return generateInterpolation(DEFAULT_START, DEFAULT_END, barCount);
        }
    }

    private static Color boostForBlackBackground(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float saturation = Math.max(0.65f, hsb[1]);
        float brightness = Math.max(0.85f, hsb[2]);
        return Color.getHSBColor(hsb[0], saturation, brightness);
    }

    private static Color findMostVibrant(List<Color> colors, Color exclude) {
        Color best = null;
        float maxScore = -1f;

        for (Color c : colors) {
            if (exclude != null && colorDistance(c, exclude) < 60) {
                continue;
            }
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            float saturation = hsb[1];
            float brightness = hsb[2];

            // Ignore almost pitch black or almost stark white
            if (brightness < 0.15f || (brightness > 0.95f && saturation < 0.1f)) {
                continue;
            }

            float score = saturation * 1.5f + brightness;
            if (score > maxScore) {
                maxScore = score;
                best = c;
            }
        }
        return best;
    }

    private static double colorDistance(Color c1, Color c2) {
        int r = c1.getRed() - c2.getRed();
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        return Math.sqrt(r * r + g * g + b * b);
    }

    private static Color[] generateInterpolation(Color start, Color end, int count) {
        Color[] res = new Color[count];
        for (int i = 0; i < count; i++) {
            float frac = (count > 1) ? (float) i / (count - 1) : 0f;
            int r = (int) (start.getRed() + frac * (end.getRed() - start.getRed()));
            int g = (int) (start.getGreen() + frac * (end.getGreen() - start.getGreen()));
            int b = (int) (start.getBlue() + frac * (end.getBlue() - start.getBlue()));
            res[i] = new Color(Math.clamp(r, 0, 255), Math.clamp(g, 0, 255), Math.clamp(b, 0, 255));
        }
        return res;
    }
}
