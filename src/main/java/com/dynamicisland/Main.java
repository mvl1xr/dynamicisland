package com.dynamicisland;

import com.dynamicisland.bridge.WindowsBridgeService;
import com.dynamicisland.model.MediaInfo;
import com.dynamicisland.model.NetworkStatus;
import com.dynamicisland.model.NotificationInfo;
import com.dynamicisland.ui.DynamicIslandWindow;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // High DPI and smooth rendering properties
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                DynamicIslandWindow window = new DynamicIslandWindow();
                window.setVisible(true);

                // Start Windows Bridge
                WindowsBridgeService bridge = new WindowsBridgeService();
                bridge.addListener(new WindowsBridgeService.Listener() {
                    @Override
                    public void onMediaUpdated(MediaInfo mediaInfo) {
                        window.updateMedia(mediaInfo);
                    }

                    @Override
                    public void onNotificationReceived(NotificationInfo notifInfo) {
                        window.triggerNotification(notifInfo);
                    }

                    @Override
                    public void onNetworkUpdated(NetworkStatus netStatus) {
                        window.updateNetwork(netStatus);
                    }
                });

                bridge.start();

                // Shutdown hook
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Shutting down Dynamic Island...");
                    bridge.stop();
                }));

                System.out.println("Dynamic Island started successfully!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
