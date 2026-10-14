package com.dynamicisland.render;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.SVGLoader;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SvgRenderer {
    private static final SVGLoader loader = new SVGLoader();
    private static final Map<String, SVGDocument> docCache = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();

    public static SVGDocument getDocument(String name) {
        return docCache.computeIfAbsent(name, key -> {
            try {
                File f = new File("icons/" + key);
                if (f.exists()) {
                    try (InputStream is = new FileInputStream(f)) {
                        return loader.load(is);
                    }
                }
                InputStream is = SvgRenderer.class.getResourceAsStream("/icons/" + key);
                if (is != null) {
                    try (is) {
                        return loader.load(is);
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not load SVG " + key + ": " + e.getMessage());
            }
            return null;
        });
    }

    public static void renderSvg(Graphics2D g, String name, float x, float y, float width, float height, Component c) {
        SVGDocument doc = getDocument(name);
        if (doc != null) {
            ViewBox viewBox = new ViewBox(x, y, width, height);
            doc.render(c, g, viewBox);
        }
    }

    public static BufferedImage getRenderedImage(String name, int width, int height) {
        String cacheKey = name + "_" + width + "x" + height;
        return imageCache.computeIfAbsent(cacheKey, k -> {
            SVGDocument doc = getDocument(name);
            if (doc == null) return null;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            doc.render(null, g2, new ViewBox(0, 0, width, height));
            g2.dispose();
            return img;
        });
    }
}
