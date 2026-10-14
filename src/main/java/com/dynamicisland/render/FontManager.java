package com.dynamicisland.render;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static Font boldBase;
    private static Font mediumBase;
    private static Font regularBase;

    private static final Map<String, Font> fontCache = new HashMap<>();

    static {
        boldBase = loadFont("Nunito-Bold.ttf", Font.BOLD, 14f);
        mediumBase = loadFont("Nunito-Medium.ttf", Font.PLAIN, 13f);
        regularBase = loadFont("Nunito-Regular.ttf", Font.PLAIN, 13f);
    }

    private static Font loadFont(String filename, int fallbackStyle, float defaultSize) {
        try {
            File f = new File("fonts/" + filename);
            if (f.exists()) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font;
            }
            InputStream is = FontManager.class.getResourceAsStream("/fonts/" + filename);
            if (is != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font;
            }
        } catch (Exception e) {
            System.err.println("Failed to load font " + filename + ": " + e.getMessage());
        }
        return new Font("SansSerif", fallbackStyle, (int) defaultSize);
    }

    public static Font getBold(float size) {
        String key = "bold_" + size;
        return fontCache.computeIfAbsent(key, k -> boldBase.deriveFont(Font.BOLD, size));
    }

    public static Font getMedium(float size) {
        String key = "medium_" + size;
        return fontCache.computeIfAbsent(key, k -> mediumBase.deriveFont(Font.PLAIN, size));
    }

    public static Font getRegular(float size) {
        String key = "reg_" + size;
        return fontCache.computeIfAbsent(key, k -> regularBase.deriveFont(Font.PLAIN, size));
    }
}
