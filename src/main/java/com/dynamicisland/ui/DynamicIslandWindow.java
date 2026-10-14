package com.dynamicisland.ui;

import com.dynamicisland.model.MediaInfo;
import com.dynamicisland.model.NetworkStatus;
import com.dynamicisland.model.NotificationInfo;
import com.dynamicisland.util.BrightnessDetector;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DynamicIslandWindow extends JWindow {
    private final DynamicIslandPanel panel;

    public DynamicIslandWindow() {
        panel = new DynamicIslandPanel();
        setContentPane(panel);

        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        setType(Type.UTILITY);

        int winWidth = 740;
        int winHeight = 38;
        setSize(winWidth, winHeight);

        // Fixed static position: pinned at top center of primary display
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int posX = (screen.width - winWidth) / 2;
        int posY = 0;
        setLocation(posX, posY);

        // Setup context menu (Color theme test & notifications)
        initInteractions();

        // Start brightness detection sampling wallpaper below the window
        Point leftSample = new Point(posX + 120, posY + 65);
        Point rightSample = new Point(posX + winWidth - 120, posY + 65);
        BrightnessDetector.startMonitoring(leftSample, rightSample);
    }

    private void initInteractions() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem bgStyleItem = new JMenuItem("Стиль фона: Черный / Liquid Glass (Шейдер)");
        bgStyleItem.addActionListener(e -> {
            com.dynamicisland.render.LiquidGlassRenderer.toggleBackgroundStyle();
            System.out.println("[App] Background style toggled to: " + com.dynamicisland.render.LiquidGlassRenderer.getBackgroundStyle());
        });
        menu.add(bgStyleItem);

        JMenuItem themeTestItem = new JMenuItem("Тест цвета: Переключить тему (Светлая / Темная / Авто)");
        themeTestItem.addActionListener(e -> {
            BrightnessDetector.toggleThemeTest();
            BrightnessDetector.ThemeMode mode = BrightnessDetector.getThemeMode();
            System.out.println("[App] Theme mode toggled to: " + mode);
        });
        menu.add(themeTestItem);

        JMenuItem testNotifItem = new JMenuItem("Тестовое уведомление");
        testNotifItem.addActionListener(e -> {
            NotificationInfo testNotif = new NotificationInfo("Telegram", "Дуров", "Dynamic Island работает отлично", null);
            panel.setNotification(testNotif);
        });
        menu.add(testNotifItem);

        menu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> System.exit(0));
        menu.add(exitItem);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    menu.show(panel, e.getX(), e.getY());
                } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    // Double click to quickly toggle color theme test
                    BrightnessDetector.toggleThemeTest();
                }
            }
        });
    }

    public void updateMedia(MediaInfo media) {
        SwingUtilities.invokeLater(() -> panel.setMedia(media));
    }

    public void triggerNotification(NotificationInfo notif) {
        SwingUtilities.invokeLater(() -> panel.setNotification(notif));
    }

    public void updateNetwork(NetworkStatus net) {
        SwingUtilities.invokeLater(() -> panel.setNetwork(net));
    }
}
