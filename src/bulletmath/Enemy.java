package bulletmath;

import java.awt.*;
import java.util.Random;

/**
 * Enemy — Ikan musuh yang membawa ekspresi matematika.
 * Bergerak dari atas ke bawah layar, persis seperti ikan turun ke dasar laut.
 *
 * Setiap ikan digambar dengan:
 *   • Badan oval (warna tropis)
 *   • Ekor segitiga
 *   • Sirip atas kecil
 *   • Mata lucu dengan sorot
 *   • Label ekspresi di bawah ikan
 */
public class Enemy {

    // ── Ukuran hitbox ─────────────────────────────────────────────────────────
    public static final int W = 110;   // Lebar area klik
    public static final int H = 70;    // Tinggi area klik (termasuk label)

    // ── Posisi dan gerak ──────────────────────────────────────────────────────
    private float x, y;
    private float speed;

    // ── Data game ────────────────────────────────────────────────────────────
    private final String expression;  // Ekspresi matematika yang ditampilkan
    private final int    value;       // Hasil ekspresi (untuk verifikasi jawaban)
    private final boolean isTarget;   // true = ikan yang harus ditembak

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean alive     = true;
    private boolean exploding = false;
    private int     expTimer  = 0;

    // ── Tampilan ──────────────────────────────────────────────────────────────
    private final Color bodyColor;   // Warna utama ikan
    private final Color bellColor;   // Warna perut (lebih terang)
    private final int   wobbleAmp;   // Amplitudo gerakan zig-zag kecil
    private float       wobblePhase; // Fase saat ini untuk gerakan zig-zag
    private int         scale;       // Ukuran relatif ikan (80–115%)

    private static final Random rng = new Random();

    // Palet warna ikan tropis
    private static final Color[][] FISH_PALETTES = {
        { new Color(231, 76,  60),  new Color(245,176,167) },  // Merah-Oranye (Ikan Badut)
        { new Color( 52,152,219),  new Color(174,214,241) },  // Biru (Surgeonfish)
        { new Color(155, 89,182),  new Color(215,189,226) },  // Ungu (Tang Biru)
        { new Color( 39,174, 96),  new Color(163,228,215) },  // Hijau (Wrasse)
        { new Color(243,156, 18),  new Color(250,215,160) },  // Kuning (Ikan Bidadari)
        { new Color( 26,188,156),  new Color(163,228,215) },  // Toska (Parrotfish)
    };

    // ── Konstruktor ───────────────────────────────────────────────────────────
    public Enemy(float x, String expression, int value, boolean isTarget, float speed) {
        this.x          = x;
        this.y          = -H - 15;   // Mulai di atas layar
        this.expression = expression;
        this.value      = value;
        this.isTarget   = isTarget;
        this.speed      = speed;
        this.wobbleAmp  = 1 + rng.nextInt(3);
        this.scale      = 80 + rng.nextInt(36);  // 80%–115%

        // Pilih warna acak dari palet
        Color[] pair = FISH_PALETTES[rng.nextInt(FISH_PALETTES.length)];
        bodyColor = pair[0];
        bellColor = pair[1];
    }

    // ── Update posisi setiap frame ────────────────────────────────────────────
    public void update() {
        if (exploding) { expTimer++; return; }

        y += speed;                     // Turun ke bawah
        wobblePhase += 0.06f;          // Zig-zag halus kiri-kanan
    }

    // ── Render ikan ───────────────────────────────────────────────────────────
    public void draw(Graphics2D g) {
        if (!alive) return;

        int dx = (int) x + (int)(Math.sin(wobblePhase) * wobbleAmp);
        int dy = (int) y;

        if (exploding) {
            drawBubbleExplosion(g, dx + W/2, dy + H/2 - 10);
            return;
        }

        // Ukuran ikan berdasarkan scale%
        int fw = W  * scale / 100;   // Lebar badan ikan
        int fh = 38 * scale / 100;   // Tinggi badan ikan
        int fx = dx + (W - fw) / 2;  // Tengahkan di hitbox
        int fy = dy + 6;

        drawFish(g, fx, fy, fw, fh);

        // Label ekspresi di bawah ikan
        drawExpressionLabel(g, dx, dy, fw, fx);
    }

    // ── Gambar ikan ──────────────────────────────────────────────────────────
    private void drawFish(Graphics2D g, int fx, int fy, int fw, int fh) {
        // — Bayangan —
        g.setColor(new Color(0,0,0,40));
        g.fillOval(fx+4, fy+5, fw, fh);

        // — Ekor (segitiga di kiri ikan yang bergerak ke kanan) —
        int tailW  = fw / 3;
        int tailH  = fh + 8;
        int[] tx   = { fx,           fx - tailW,    fx - tailW };
        int[] ty   = { fy + fh/2,   fy - tailH/4,  fy + fh + tailH/4 };
        g.setColor(bodyColor.darker());
        g.fillPolygon(tx, ty, 3);

        // — Sirip atas —
        int[] finX = { fx + fw/4, fx + fw*2/5, fx + fw/2 };
        int[] finY = { fy,        fy - fh/2,   fy };
        g.setColor(bodyColor);
        g.fillPolygon(finX, finY, 3);

        // — Badan utama —
        GradientPaint bodyGrad = new GradientPaint(
            fx, fy,        bodyColor.brighter(),
            fx, fy + fh,   bodyColor.darker());
        g.setPaint(bodyGrad);
        g.fillOval(fx, fy, fw, fh);
        g.setPaint(null);

        // — Perut (lebih terang, garis bawah) —
        g.setColor(bellColor);
        g.fillOval(fx + fw/5, fy + fh/3, fw*3/5, fh*2/3);

        // — Garis stripes jika ikan merah (Ikan Badut) —
        if (bodyColor.getRed() > 200 && bodyColor.getGreen() < 100) {
            g.setColor(new Color(255,255,255,130));
            g.setStroke(new BasicStroke(3f));
            int sx = fx + fw/3;
            g.drawLine(sx, fy+2, sx, fy+fh-2);
            g.drawLine(sx + fw/5, fy+2, sx + fw/5, fy+fh-2);
            g.setStroke(new BasicStroke(1f));
        }

        // — Border badan —
        g.setColor(bodyColor.darker().darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(fx, fy, fw, fh);
        g.setStroke(new BasicStroke(1f));

        // — Mata —
        int eyeX = fx + fw * 3/4;
        int eyeY = fy + fh/4;
        int eyeR = Math.max(5, fh/5);
        g.setColor(Color.WHITE);
        g.fillOval(eyeX, eyeY, eyeR*2, eyeR*2);
        g.setColor(new Color(20,30,40));
        g.fillOval(eyeX + eyeR/2, eyeY + eyeR/2, eyeR, eyeR);
        // Sorot mata
        g.setColor(Color.WHITE);
        g.fillOval(eyeX + eyeR/2 + 2, eyeY + eyeR/2, eyeR/3, eyeR/3);

        // — Mulut —
        g.setColor(new Color(30,10,10,160));
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc(eyeX - eyeR, fy + fh*3/4, eyeR*2, eyeR, 0, -180);
        g.setStroke(new BasicStroke(1f));
    }

    // ── Label ekspresi ────────────────────────────────────────────────────────
    private void drawExpressionLabel(Graphics2D g, int dx, int dy, int fw, int fx) {
        g.setFont(new Font("Consolas", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        int tw  = fm.stringWidth(expression);
        int lx  = dx + (W - tw) / 2;
        int ly  = dy + 50;

        // Latar label
        g.setColor(new Color(0,0,0,170));
        g.fillRoundRect(lx - 8, ly - 14, tw + 16, 20, 8, 8);

        // Teks ekspresi
        g.setColor(Color.WHITE);
        g.drawString(expression, lx, ly);

        // Tanda bintang untuk target (petunjuk halus)
        if (isTarget) {
            g.setColor(new Color(255,215,0,100));  // Transparan — agar tidak terlalu mudah
            g.fillOval(dx + W - 14, dy + 2, 10, 10);
        }
    }

    // ── Animasi gelembung saat ditembak ──────────────────────────────────────
    private void drawBubbleExplosion(Graphics2D g, int cx, int cy) {
        float prog = Math.min(1f, expTimer / 20f);
        int   maxR = 55;
        int   r    = (int)(maxR * prog);
        int   alpha= Math.max(0, 220 - (int)(220 * prog));

        // Lingkaran gelembung besar
        g.setColor(new Color(100,200,255,alpha));
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(cx - r, cy - r, r*2, r*2);
        g.setStroke(new BasicStroke(1f));

        // Lingkaran dalam
        int r2 = (int)(r * 0.6);
        g.setColor(new Color(200,240,255,alpha/2));
        g.fillOval(cx-r2, cy-r2, r2*2, r2*2);

        // Gelembung kecil di sekitar
        for (int i = 0; i < 6; i++) {
            double ang = i * Math.PI / 3 + prog * 2;
            int bx = cx + (int)(Math.cos(ang) * r * 1.2);
            int by = cy + (int)(Math.sin(ang) * r * 1.2);
            int br = Math.max(1, (int)(6 * (1 - prog)));
            g.setColor(new Color(180,230,255,alpha));
            g.fillOval(bx - br, by - br, br*2, br*2);
        }
    }

    // ── Mulai animasi ledakan ─────────────────────────────────────────────────
    public void explode() {
        exploding = true;
        expTimer  = 0;
    }

    // ── Cek apakah koordinat klik ada di dalam ikan ───────────────────────────
    public boolean contains(int mx, int my) {
        return mx >= x && mx <= x + W && my >= y && my <= y + H;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public boolean isAlive()          { return alive; }
    public void    setAlive(boolean b){ alive = b; }
    public boolean isExploding()      { return exploding; }
    public int     getExpTimer()      { return expTimer; }
    public boolean isTarget()         { return isTarget; }
    public int     getValue()         { return value; }
    public String  getExpression()    { return expression; }
    public float   getY()             { return y; }
    public float   getX()             { return x; }
}