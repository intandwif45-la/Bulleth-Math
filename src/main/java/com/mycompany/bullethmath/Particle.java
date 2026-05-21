package com.mycompany.bullethmath;


/**
 * ============================================================
 *  Particle — Partikel gelembung untuk efek ledakan bawah air.
 *  Digunakan saat musuh dihancurkan atau tembakan salah.
 * ============================================================
 */
import java.awt.*;
import java.util.Random;

public class Particle {

    private float x, y;
    private float vx, vy;
    private float life = 1.0f;
    private Color color;
    private int   size;
    public  boolean alive = true;
    private boolean isBubble;   // true = gambar sebagai gelembung bening

    public Particle(int cx, int cy, Color col, Random rng) {
        x = cx; y = cy;

        double angle = rng.nextDouble() * Math.PI * 2;
        float  speed = 1.5f + rng.nextFloat() * 3.5f;
        vx = (float)(Math.cos(angle) * speed);
        vy = (float)(Math.sin(angle) * speed);

        color    = col;
        size     = 5 + rng.nextInt(8);
        isBubble = rng.nextFloat() > 0.4f;   // 60% kemungkinan gelembung
    }

    public void update() {
        x  += vx;
        y  += vy;
        vy -= 0.08f;    // Gravitasi terbalik (gelembung naik ke atas)
        vx *= 0.97f;
        life -= 0.032f;
        if (life <= 0) alive = false;
    }

    public void draw(Graphics2D g) {
        if (!alive) return;
        int alpha = (int)(life * 200);
        int s     = (int)(size * life);

        if (isBubble) {
            // Gelembung transparan dengan kilap
            g.setColor(new Color(200, 240, 255, Math.max(0, alpha / 2)));
            g.fillOval((int) x - s / 2, (int) y - s / 2, s, s);
            g.setColor(new Color(255, 255, 255, Math.max(0, alpha)));
            g.drawOval((int) x - s / 2, (int) y - s / 2, s, s);
            // Kilap kecil
            if (s > 4) {
                g.setColor(new Color(255, 255, 255, Math.max(0, alpha - 40)));
                g.fillOval((int) x - s / 4, (int) y - s / 3, s / 3, s / 4);
            }
        } else {
            // Partikel warna solid
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, alpha)));
            g.fillOval((int) x - s / 2, (int) y - s / 2, s, s);
        }
    }
}
