package com.mycompany.bullethmath;

import java.awt.*;
import javax.swing.border.AbstractBorder;

/**
 * RoundBorder.java
 * Border rounded lembut untuk field dan panel bertema bawah laut lucu.
 */
public class RoundBorder extends AbstractBorder {
    private final Color color;
    private final int radius;
    private final float thickness;

    public RoundBorder(Color color, int radius, float thickness) {
        this.color = color;
        this.radius = radius;
        this.thickness = thickness;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int strokeFix = Math.round(thickness);

        // glow luar lembut
        g2.setColor(new Color(255, 255, 255, 90));
        g2.setStroke(new BasicStroke(thickness + 1.5f));
        g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, radius, radius);

        // outline utama
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));
        g2.drawRoundRect(
            x + strokeFix / 2,
            y + strokeFix / 2,
            w - strokeFix - 1,
            h - strokeFix - 1,
            radius,
            radius
        );

        // highlight tipis bagian atas supaya terasa glossy
        Shape oldClip = g2.getClip();
        g2.setClip(x, y, w, h / 2);
        g2.setColor(new Color(255, 255, 255, 110));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x + 2, y + 2, w - 5, h - 5, radius, radius);
        g2.setClip(oldClip);

        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        int pad = Math.max(6, (int) Math.ceil(thickness) + 4);
        return new Insets(pad, pad + 2, pad, pad + 2);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int pad = Math.max(6, (int) Math.ceil(thickness) + 4);
        insets.set(pad, pad + 2, pad, pad + 2);
        return insets;
    }
}