package com.dynamicisland.util;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BrightnessDetector {
    private static Robot robot;
    private static volatile boolean isLightBackground = false;
    private static float currentFade = 0.0f; // 0.0 = dark background (white icons), 1.0 = light background (black icons)
    private static float targetFade = 0.0f;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.err.println("Robot could not be initialized: " + e.getMessage());
        }
    }

    public static void startMonitoring(Point leftSamplePoint, Point rightSamplePoint) {
        if (robot == null) return;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BrightnessDetectorThread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (leftSamplePoint == null || rightSamplePoint == null) return;

                // Sample points around left and right areas
                double totalLuminance = 0;
                int count = 0;

                Point[] sampleLocs = new Point[] {
                    new Point(leftSamplePoint.x, leftSamplePoint.y),
                    new Point(leftSamplePoint.x - 15, leftSamplePoint.y),
                    new Point(leftSamplePoint.x + 15, leftSamplePoint.y),
                    new Point(rightSamplePoint.x, rightSamplePoint.y),
                    new Point(rightSamplePoint.x - 15, rightSamplePoint.y),
                    new Point(rightSamplePoint.x + 15, rightSamplePoint.y)
                };

                for (Point p : sampleLocs) {
                    if (p.x >= 0 && p.y >= 0) {
                        Color c = robot.getPixelColor(p.x, p.y);
                        // Perceived luminance formula
                        double lum = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
                        totalLuminance += lum;
                        count++;
                    }
                }

                if (count > 0) {
                    double avgLum = totalLuminance / count;
                    boolean light = avgLum > 0.52;
                    isLightBackground = light;
                    targetFade = light ? 1.0f : 0.0f;
                }
            } catch (Exception ignored) {}
        }, 500, 400, TimeUnit.MILLISECONDS);
    }

    public static void updateFade(float delta) {
        float speed = 0.18f; // fast fade
        currentFade += (targetFade - currentFade) * speed;
        if (Math.abs(targetFade - currentFade) < 0.01f) {
            currentFade = targetFade;
        }
    }

    public static boolean isLightBackground() {
        return isLightBackground;
    }

    public static float getFadeProgress() {
        return currentFade;
    }

    // Blend between dark theme color (white) and light theme color (black) based on fade
    public static Color getAdaptiveColor(Color darkColor, Color lightColor) {
        float f = Math.clamp(currentFade, 0f, 1f);
        int r = (int) (darkColor.getRed() + f * (lightColor.getRed() - darkColor.getRed()));
        int g = (int) (darkColor.getGreen() + f * (lightColor.getGreen() - darkColor.getGreen()));
        int b = (int) (darkColor.getBlue() + f * (lightColor.getBlue() - darkColor.getBlue()));
        int a = (int) (darkColor.getAlpha() + f * (lightColor.getAlpha() - darkColor.getAlpha()));
        return new Color(r, g, b, a);
    }
}
