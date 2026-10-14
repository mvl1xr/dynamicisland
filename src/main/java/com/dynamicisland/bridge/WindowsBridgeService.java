package com.dynamicisland.bridge;

import com.dynamicisland.model.MediaInfo;
import com.dynamicisland.model.NetworkStatus;
import com.dynamicisland.model.NotificationInfo;
import com.dynamicisland.render.ColorExtractor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

public class WindowsBridgeService {
    public interface Listener {
        void onMediaUpdated(MediaInfo mediaInfo);
        void onNotificationReceived(NotificationInfo notifInfo);
        void onNetworkUpdated(NetworkStatus netStatus);
    }

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private Process bridgeProcess;
    private volatile boolean isRunning = false;

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;

        Thread bridgeThread = new Thread(this::runBridgeLoop, "WindowsBridgeProcessThread");
        bridgeThread.setDaemon(true);
        bridgeThread.start();
    }

    private void runBridgeLoop() {
        File script = new File("scripts/bridge.ps1");
        if (!script.exists()) {
            System.err.println("bridge.ps1 not found at " + script.getAbsolutePath());
            return;
        }

        while (isRunning) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-File", script.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                bridgeProcess = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(bridgeProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while (isRunning && (line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("EVENT:")) {
                            String jsonStr = line.substring(6).trim();
                            handleEventJson(jsonStr);
                        } else {
                            System.out.println("[Bridge] " + line);
                        }
                    }
                }

                if (bridgeProcess != null) {
                    bridgeProcess.waitFor();
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("Bridge error: " + e.getMessage());
                }
            }

            if (isRunning) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void handleEventJson(String jsonStr) {
        try {
            JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "";

            switch (type) {
                case "media" -> {
                    String title = obj.has("title") ? obj.get("title").getAsString() : "";
                    String artist = obj.has("artist") ? obj.get("artist").getAsString() : "";
                    boolean isPlaying = obj.has("isPlaying") && obj.get("isPlaying").getAsBoolean();
                    String coverPath = obj.has("coverPath") ? obj.get("coverPath").getAsString() : "";

                    BufferedImage cover = null;
                    Color[] gradient = null;
                    if (!coverPath.isEmpty()) {
                        File cf = new File(coverPath);
                        if (cf.exists()) {
                            try {
                                cover = ImageIO.read(cf);
                                gradient = ColorExtractor.extractGradient(cover, 6);
                            } catch (Exception ignored) {}
                        }
                    }

                    if (gradient == null) {
                        gradient = ColorExtractor.extractGradient(null, 6);
                    }

                    MediaInfo media = new MediaInfo(title, artist, isPlaying, cover, gradient);
                    System.out.println("[App] Media updated: " + (title.isEmpty() ? "Idle" : title + " by " + artist + " (playing=" + isPlaying + ")"));
                    for (Listener l : listeners) {
                        l.onMediaUpdated(media);
                    }
                }
                case "notification" -> {
                    String appName = obj.has("appName") ? obj.get("appName").getAsString() : "Уведомление";
                    String notifTitle = obj.has("title") ? obj.get("title").getAsString() : "";
                    String text = obj.has("text") ? obj.get("text").getAsString() : "";
                    String iconPath = obj.has("iconPath") ? obj.get("iconPath").getAsString() : "";

                    BufferedImage appIcon = null;
                    if (!iconPath.isEmpty()) {
                        File iconFile = new File(iconPath);
                        if (iconFile.exists()) {
                            try {
                                appIcon = ImageIO.read(iconFile);
                            } catch (Exception ignored) {}
                        }
                    }

                    System.out.println("[App] Notification received from " + appName + ": " + notifTitle + " - " + text);
                    NotificationInfo notif = new NotificationInfo(appName, notifTitle, text, appIcon);
                    for (Listener l : listeners) {
                        l.onNotificationReceived(notif);
                    }
                }
                case "network" -> {
                    int level = obj.has("wifiLevel") ? obj.get("wifiLevel").getAsInt() : 1;
                    boolean conn = !obj.has("connected") || obj.get("connected").getAsBoolean();
                    System.out.println("[App] Network updated: wifiLevel=" + level + ", connected=" + conn);
                    NetworkStatus net = new NetworkStatus(level, conn, false);
                    for (Listener l : listeners) {
                        l.onNetworkUpdated(net);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing bridge json: " + e.getMessage());
        }
    }

    public synchronized void stop() {
        isRunning = false;
        if (bridgeProcess != null) {
            bridgeProcess.destroyForcibly();
            bridgeProcess = null;
        }
    }
}
