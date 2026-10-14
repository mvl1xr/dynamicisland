package com.dynamicisland.audio;

import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundPlayer {
    private static final ExecutorService soundPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "SoundPlayerThread");
        t.setDaemon(true);
        return t;
    });

    public static void playDing() {
        soundPool.submit(() -> {
            try {
                InputStream is = null;
                File f = new File("ding.mp3");
                if (f.exists()) {
                    is = new FileInputStream(f);
                } else {
                    is = SoundPlayer.class.getResourceAsStream("/ding.mp3");
                }

                if (is != null) {
                    try (BufferedInputStream bis = new BufferedInputStream(is)) {
                        Player player = new Player(bis);
                        player.play();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error playing ding sound: " + e.getMessage());
            }
        });
    }
}
