package com.dynamicisland.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

public class EqualizerVisualizer {
    public static final int BAR_COUNT = 6;
    private final float[] currentHeights = new float[BAR_COUNT];
    private final float[] targetHeights = new float[BAR_COUNT];
    private final float[] phases = new float[BAR_COUNT];
    private final float[] speeds = new float[BAR_COUNT];
    private final Random random = new Random();
    private Color[] colors;

    private static final float MAX_HEIGHT = 16f;
    private static final float MIN_HEIGHT = 3f;
    private static final float BAR_WIDTH = 2.5f;
    private static final float BAR_GAP = 2.5f;

    public EqualizerVisualizer() {
        for (int i = 0; i < BAR_COUNT; i++) {
            currentHeights[i] = MIN_HEIGHT;
            targetHeights[i] = MIN_HEIGHT;
            phases[i] = (float) (random.nextDouble() * Math.PI * 2);
            speeds[i] = 0.08f + (float) (random.nextDouble() * 0.07f);
        }
        colors = ColorExtractor.extractGradient(null, BAR_COUNT);
    }

    public void setColors(Color[] newColors) {
        if (newColors != null && newColors.length >= BAR_COUNT) {
            this.colors = newColors;
        }
    }

    public void update(boolean isPlaying, float delta) {
        for (int i = 0; i < BAR_COUNT; i++) {
            if (isPlaying) {
                phases[i] += speeds[i] * delta * 60f;
                float sinVal = (float) (Math.sin(phases[i]) * 0.5f + 0.5f);
                float noise = (random.nextFloat() - 0.5f) * 0.3f;
                targetHeights[i] = MIN_HEIGHT + (MAX_HEIGHT - MIN_HEIGHT) * Math.clamp(sinVal + noise, 0f, 1f);
            } else {
                targetHeights[i] = MIN_HEIGHT;
            }

            // Smooth interpolation (lerp)
            float lerpSpeed = isPlaying ? 0.22f : 0.12f;
            currentHeights[i] += (targetHeights[i] - currentHeights[i]) * lerpSpeed;
        }
    }

    public void render(Graphics2D g, float x, float centerY) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float totalWidth = BAR_COUNT * BAR_WIDTH + (BAR_COUNT - 1) * BAR_GAP;
        float startX = x;

        for (int i = 0; i < BAR_COUNT; i++) {
            float h = Math.max(MIN_HEIGHT, currentHeights[i]);
            float y = centerY - (h / 2f);
            float bx = startX + i * (BAR_WIDTH + BAR_GAP);

            Color barColor = (colors != null && i < colors.length) ? colors[i] : Color.WHITE;
            g.setColor(barColor);
            RoundRectangle2D.Float roundRect = new RoundRectangle2D.Float(bx, y, BAR_WIDTH, h, BAR_WIDTH, BAR_WIDTH);
            g.fill(roundRect);
        }
    }

    public float getTotalWidth() {
        return BAR_COUNT * BAR_WIDTH + (BAR_COUNT - 1) * BAR_GAP;
    }
}
