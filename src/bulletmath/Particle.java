package bulletmath;
 
import java.awt.*;
import java.util.Random;
 
/**
 * Particle — Gelembung kecil yang muncul saat ikan ditembak.
 *
 * Setiap gelembung bergerak ke atas dan ke samping secara acak,
 * lalu perlahan-lahan memudar sampai hilang.
 * Tampilan ini menggantikan partikel ledakan versi luar angkasa
 * dengan efek gelembung yang cocok untuk tema lautan.
 */
public class Particle {
 
    // ── Posisi dan kecepatan ──────────────────────────────────────────────────
    private float x, y;
    private float vx, vy;
 
    // ── Tampilan ──────────────────────────────────────────────────────────────
    private float   life  = 1.0f;   // 1.0 = penuh, 0.0 = hilang
    private Color   color;
    private int     size;
    private boolean isRing;         // true = gelembung transparan (cincin), false = titik penuh
 
    public boolean alive = true;
 
    // ── Konstruktor ───────────────────────────────────────────────────────────
    public Particle(int cx, int cy, Color col, Random rng) {
        x = cx;
        y = cy;
 
        // Arah acak
        double angle = rng.nextDouble() * Math.PI * 2;
        float  speed = 1.5f + rng.nextFloat() * 3.5f;
        vx    = (float)(Math.cos(angle) * speed);
        vy    = (float)(Math.sin(angle) * speed) - 1.5f;  // Sedikit ke atas (seperti gelembung)
 
        color  = col;
        size   = 5 + rng.nextInt(8);
        isRing = rng.nextBoolean();  // Setengah partikel tampak sebagai cincin gelembung
    }
 
    // ── Update setiap frame ───────────────────────────────────────────────────
    public void update() {
        x  += vx;
        y  += vy;
 
        // Gelembung cenderung naik ke atas (berlawanan gravitasi)
        vy -= 0.05f;
 
        // Hambatan horizontal
        vx *= 0.97f;
 
        // Pudar
        life -= 0.03f;
        if (life <= 0) alive = false;
    }
 
    // ── Render ────────────────────────────────────────────────────────────────
    public void draw(Graphics2D g) {
        if (!alive) return;
 
        int alpha = (int)(life * 200);
        alpha = Math.max(0, Math.min(255, alpha));
        int s = Math.max(1, (int)(size * life));
 
        if (isRing) {
            // Gelembung: lingkaran transparan dengan border tipis
            g.setColor(new Color(180, 230, 255, alpha));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval((int)x - s/2, (int)y - s/2, s, s);
            g.setStroke(new BasicStroke(1f));
            // Sorot kecil di dalam gelembung
            g.setColor(new Color(255,255,255,alpha/3));
            g.fillOval((int)x - s/4, (int)y - s/3, s/3, s/3);
        } else {
            // Titik kecil berwarna
            g.setColor(new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                alpha
            ));
            g.fillOval((int)x - s/2, (int)y - s/2, s, s);
        }
    }
}