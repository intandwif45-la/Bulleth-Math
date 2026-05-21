package com.mycompany.bullethmath;

import javax.swing.*;
import java.awt.*;

/**
 * TransitionManager — Efek flash gelap CapCut style.
 * Layar gelap cepat (80ms) → ganti tampilan → terang kembali cepat (200ms).
 */
public class TransitionManager {

    private static final int FPS       = 60;
    private static final int DARK_MS   = 80;    // gelap dalam 80ms
    private static final int BRIGHT_MS = 200;   // terang kembali dalam 200ms

    /** Flash gelap lalu langsung ganti tampilan. */
    public static void fadeOut(JFrame frame, Runnable action) {
        JPanel overlay = createOverlay(new Color(0, 0, 0, 0));
        attachOverlay(frame, overlay);

        int darkSteps = Math.max(1, FPS * DARK_MS / 1000);
        final int[] step = {0};

        Timer t = new Timer(1000 / FPS, null);
        t.addActionListener(e -> {
            step[0]++;
            float a = Math.min(1f, (float) step[0] / darkSteps);
            overlay.setBackground(new Color(0, 0, 0, (int)(a * 255)));
            overlay.repaint();
            if (step[0] >= darkSteps) {
                t.stop();
                // Langsung ganti tampilan saat sudah gelap penuh
                SwingUtilities.invokeLater(action);
            }
        });
        t.start();
    }

    /** Tampilan baru muncul dari gelap, terang kembali cepat. */
    public static void fadeIn(JFrame frame) {
        JPanel overlay = createOverlay(new Color(0, 0, 0, 255));
        attachOverlay(frame, overlay);

        int brightSteps = Math.max(1, FPS * BRIGHT_MS / 1000);
        final int[] step = {0};

        Timer t = new Timer(1000 / FPS, null);
        t.addActionListener(e -> {
            step[0]++;
            float a = Math.max(0f, 1f - (float) step[0] / brightSteps);
            overlay.setBackground(new Color(0, 0, 0, (int)(a * 255)));
            overlay.repaint();
            if (step[0] >= brightSteps) {
                t.stop();
                frame.getLayeredPane().remove(overlay);
                frame.getLayeredPane().repaint();
            }
        });
        SwingUtilities.invokeLater(t::start);
    }

    private static JPanel createOverlay(Color initial) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setOpaque(false);
        p.setBackground(initial);
        return p;
    }

    private static void attachOverlay(JFrame frame, JPanel overlay) {
        JLayeredPane lp = frame.getLayeredPane();
        overlay.setBounds(0, 0, frame.getWidth(), frame.getHeight());
        lp.add(overlay, JLayeredPane.DRAG_LAYER);
        overlay.setVisible(true);
    }
}
