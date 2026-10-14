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
    private Point dragOffset;

    public DynamicIslandWindow() {
        panel = new DynamicIslandPanel();
        setContentPane(panel);

        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        setType(Type.UTILITY);

        int winWidth = 740;
        int winHeight = 65;
        setSize(winWidth, winHeight);

        // Center at top of primary screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int posX = (screen.width - winWidth) / 2;
        int posY = 0;
        setLocation(posX, posY);

        // Setup mouse listeners for right click menu & dragging
        initMouseInteractions(posX, posY);

        // Start brightness detection for left (wifi/airplane) and right (clock) areas
        Point leftSample = new Point(posX + 70, posY + 25);
        Point rightSample = new Point(posX + winWidth - 70, posY + 25);
        BrightnessDetector.startMonitoring(leftSample, rightSample);
    }

    private void initMouseInteractions(int defaultX, int defaultY) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem testNotifItem = new JMenuItem("Тестовое уведомление");
        testNotifItem.addActionListener(e -> {
            NotificationInfo testNotif = new NotificationInfo("Telegram", "Дуров", "Привет! Dynamic Island работает идеально", null);
            panel.setNotification(testNotif);
        });
        menu.add(testNotifItem);

        JMenuItem resetPosItem = new JMenuItem("Сбросить позицию (Центр сверху)");
        resetPosItem.addActionListener(e -> setLocation(defaultX, defaultY));
        menu.add(resetPosItem);

        menu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> System.exit(0));
        menu.add(exitItem);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    menu.show(panel, e.getX(), e.getY());
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    dragOffset = e.getPoint();
                }
            }
        });

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset != null && SwingUtilities.isLeftMouseButton(e)) {
                    Point current = getLocation();
                    setLocation(current.x + e.getX() - dragOffset.x, current.y + e.getY() - dragOffset.y);
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
