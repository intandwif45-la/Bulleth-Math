package com.mycompany.bullethmath;

/**
 * ============================================================
 *  FloatText — Teks mengambang yang muncul saat skor berubah.
 *  Bergerak ke atas dan memudar secara perlahan.
 * ============================================================
 */
import java.awt.*;

public class FloatText {

    private float  x, y;
    private String text;
    private Color  color;
    private float  life = 1.0f;   // 1.0 = penuh, 0.0 = hilang
    public  boolean alive = true;

    public FloatText(String text, int x, int y, Color col) {
        this.text  = text;
        this.x     = x;
        this.y     = y;
        this.color = col;
    }

    /** Update posisi dan transparansi. */
    public void update() {
        y    -= 1.5f;     // Naik ke atas
        life -= 0.025f;   // Memudar
        if (life <= 0) alive = false;
    }

    /** Gambar teks dengan bayangan dan fade. */
    public void draw(Graphics2D g) {
        if (!alive) return;
        int alpha = (int)(life * 255);

        g.setFont(new Font("Arial Black", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        int tx = (int) x - fm.stringWidth(text) / 2;

        // Bayangan
        g.setColor(new Color(0, 0, 0, Math.max(0, alpha - 80)));
        g.drawString(text, tx + 2, (int) y + 2);

        // Teks utama
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, alpha)));
        g.drawString(text, tx, (int) y);
    }
}
