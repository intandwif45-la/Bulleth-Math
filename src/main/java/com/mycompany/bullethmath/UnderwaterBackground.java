package com.mycompany.bullethmath;
/**
 * ============================================================
 *  UnderwaterBackground — Menangani semua rendering latar belakang.
 *  Fitur:
 *    - Gradasi lautan biru bertingkat
 *    - Sinar matahari dari permukaan (caustic rays)
 *    - Efek caustic bergerak (refleksi air)
 *    - Dasar laut berpasir dengan batu
 *    - Karang berwarna
 *    - Rumput laut bergoyang
 *    - Gelembung naik ke permukaan
 *    - Partikel debu laut mengambang
 * ============================================================
 */
import java.awt.*;
import java.util.Random;

public class UnderwaterBackground {

    private final int W, H;
    private float bgScroll = 0f;

    // Partikel debu laut
    private final int[][] dustParticles;
    // Gelembung latar
    private final float[] bubbleX, bubbleY, bubbleSpeed, bubbleR;

    private static final int DUST_COUNT   = 80;
    private static final int BUBBLE_COUNT = 18;

    public UnderwaterBackground(int panelW, int panelH) {
        this.W = panelW;
        this.H = panelH;
        Random rng = new Random();

        // Inisialisasi partikel debu
        dustParticles = new int[DUST_COUNT][3];
        for (int[] d : dustParticles) {
            d[0] = rng.nextInt(W);
            d[1] = rng.nextInt(H);
            d[2] = rng.nextInt(3) + 1;  // ukuran 1–3
        }

        // Inisialisasi gelembung latar
        bubbleX     = new float[BUBBLE_COUNT];
        bubbleY     = new float[BUBBLE_COUNT];
        bubbleSpeed = new float[BUBBLE_COUNT];
        bubbleR     = new float[BUBBLE_COUNT];
        for (int i = 0; i < BUBBLE_COUNT; i++) {
            bubbleX[i]     = rng.nextFloat() * W;
            bubbleY[i]     = rng.nextFloat() * H;
            bubbleSpeed[i] = 0.4f + rng.nextFloat() * 0.8f;
            bubbleR[i]     = 4f + rng.nextInt(10);
        }
    }

    /** Panggil setiap frame untuk menggerakkan elemen. */
    public void update() {
        bgScroll += 0.55f;
        if (bgScroll > H) bgScroll -= H;

        // Gelembung naik ke atas
        for (int i = 0; i < BUBBLE_COUNT; i++) {
            bubbleY[i] -= bubbleSpeed[i];
            bubbleX[i] += (float) Math.sin(bgScroll * 0.03f + i) * 0.5f;
            if (bubbleY[i] < -20) bubbleY[i] = H + 10;
        }
    }

    /** Render semua elemen latar ke Graphics2D. */
    public void draw(Graphics2D g) {
        drawOceanGradient(g);
        drawSunRays(g);
        drawCausticLines(g);
        drawDustParticles(g);
        drawBubbles(g);
        drawDistantFish(g);
        drawSeabed(g);
        drawRocks(g);
        drawCoral(g);
        drawSeaweed(g);
    }

    // ── Gradasi lautan ────────────────────────────────────────────────────────
    private void drawOceanGradient(Graphics2D g) {
        // Gradasi utama: biru muda di atas → biru tua di bawah
        GradientPaint ocean = new GradientPaint(
            0, 0,   new Color(40, 170, 210),
            0, H,   new Color(3, 18, 50)
        );
        g.setPaint(ocean);
        g.fillRect(0, 0, W, H);

        // Lapisan gelap tengah ke bawah (efek kedalaman)
        GradientPaint depth = new GradientPaint(
            0, H / 2,  new Color(0, 20, 50, 0),
            0, H,      new Color(0, 10, 30, 120)
        );
        g.setPaint(depth);
        g.fillRect(0, H / 2, W, H / 2);
        g.setPaint(null);
    }

    // ── Sinar matahari dari atas ───────────────────────────────────────────────
    private void drawSunRays(Graphics2D g) {
        for (int i = 0; i < 7; i++) {
            int rx = -120 + i * 110 + (int)(Math.sin(bgScroll * 0.018f + i) * 22);

            Polygon ray = new Polygon();
            ray.addPoint(rx,          0);
            ray.addPoint(rx + 50,     0);
            ray.addPoint(rx + 170,    H);
            ray.addPoint(rx - 110,    H);

            g.setColor(new Color(200, 240, 255, 18));
            g.fillPolygon(ray);
        }
    }

    // ── Efek caustic (refleksi cahaya air bergerak) ───────────────────────────
    private void drawCausticLines(Graphics2D g) {
        g.setStroke(new BasicStroke(1.3f));
        for (int row = 0; row < 8; row++) {
            int baseY = 80 + row * 50;
            int move  = (int)(bgScroll * 2 + row * 40);

            g.setColor(new Color(180, 240, 255, 20));
            for (int bx = -120; bx < W + 120; bx += 85) {
                int cx = bx + (move % 85);
                int cy = baseY + (int)(Math.sin((cx + bgScroll * 3) * 0.025f) * 10);
                g.drawArc(cx, cy, 95, 26, 15, 150);
                g.drawArc(cx + 22, cy + 12, 80, 20, 200, 140);
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ── Partikel debu laut mengambang ─────────────────────────────────────────
    private void drawDustParticles(Graphics2D g) {
        for (int[] d : dustParticles) {
            int px   = d[0] + (int)(Math.sin(bgScroll * 0.025f + d[1]) * 8);
            int py   = (int)((d[1] - bgScroll * 1.2f + H * 2) % H);
            int size = Math.max(1, d[2]);
            g.setColor(new Color(220, 245, 255, 30 + size * 12));
            g.fillOval(px, py, size, size);
        }
    }

    // ── Gelembung naik ke permukaan ───────────────────────────────────────────
    private void drawBubbles(Graphics2D g) {
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < BUBBLE_COUNT; i++) {
            int bx = (int) bubbleX[i];
            int by = (int) bubbleY[i];
            int r  = (int) bubbleR[i];

            // Lingkaran gelembung
            g.setColor(new Color(200, 240, 255, 70));
            g.drawOval(bx, by, r * 2, r * 2);
            // Kilauan dalam gelembung
            g.setColor(new Color(255, 255, 255, 90));
            g.fillOval(bx + r / 2, by + r / 2, Math.max(2, r / 2), Math.max(2, r / 2));
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ── Siluet ikan jauh (dekoratif, sangat transparan) ──────────────────────
    private void drawDistantFish(Graphics2D g) {
        for (int i = 0; i < 6; i++) {
            int fy = 100 + i * 70;
            int fx = (int)((bgScroll * (0.6f + i * 0.1f) + i * 110) % (W + 130)) - 80;

            g.setColor(new Color(2, 25, 50, 60));
            g.fillOval(fx, fy, 30, 11);
            Polygon t = new Polygon();
            t.addPoint(fx, fy + 5);
            t.addPoint(fx - 12, fy);
            t.addPoint(fx - 12, fy + 11);
            g.fillPolygon(t);
        }
    }

    // ── Dasar laut berpasir ────────────────────────────────────────────────────
    private void drawSeabed(Graphics2D g) {
        GradientPaint sand = new GradientPaint(
            0, H - 90, new Color(160, 135, 88),
            0, H,      new Color(90, 68, 45)
        );
        g.setPaint(sand);
        g.fillArc(-120, H - 100, 320, 140, 0, 180);
        g.fillArc( 80,  H - 88,  350, 130, 0, 180);
        g.fillArc(330,  H - 105, 380, 155, 0, 180);
        g.fillRect(0, H - 50, W, 50);
        g.setPaint(null);
    }

    // ── Batu / karang gelap ───────────────────────────────────────────────────
    private void drawRocks(Graphics2D g) {
        g.setColor(new Color(30, 40, 60, 180));
        g.fillOval(30,  H - 60, 80, 38);
        g.fillOval(430, H - 68, 100, 45);
        g.fillOval(510, H - 50, 70, 32);
        g.fillOval(180, H - 55, 60, 28);
    }

    // ── Karang berwarna ────────────────────────────────────────────────────────
    private void drawCoral(Graphics2D g) {
        drawCoralBranch(g, 90,  H - 48, 45, new Color(220, 60, 90,  180), 0);
        drawCoralBranch(g, 480, H - 55, 58, new Color(255, 130, 40, 180), 1);
        drawCoralBranch(g, 250, H - 42, 38, new Color(255, 80, 180,  160), 2);
        drawCoralBranch(g, 360, H - 50, 48, new Color(80, 200, 180,  160), 0);
    }

    private void drawCoralBranch(Graphics2D g, int x, int y, int h, Color col, int variant) {
        g.setColor(col);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x, y, x, y - h);
        g.drawLine(x, y - h / 2, x - 18, y - h / 2 - 20);
        g.drawLine(x, y - h / 2, x + 16, y - h / 2 - 16);

        if (variant >= 1) {
            g.drawLine(x + 4, y - h + 15, x + 22, y - h - 8);
        }
        if (variant >= 2) {
            g.drawLine(x - 3, y - h + 20, x - 22, y - h + 5);
        }

        // Ujung karang bulat
        g.setColor(col.brighter());
        g.setStroke(new BasicStroke(1f));
        g.fillOval(x - 5, y - h - 5, 10, 10);
        g.fillOval(x - 23, y - h / 2 - 28, 9, 9);
        g.fillOval(x + 11, y - h / 2 - 24, 9, 9);
    }

    // ── Rumput laut bergoyang ─────────────────────────────────────────────────
    private void drawSeaweed(Graphics2D g) {
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 18; i++) {
            int baseX  = 5 + i * 36;
            int baseY  = H - 22;
            int height = 35 + (i % 5) * 14;
            int sway   = (int)(Math.sin(bgScroll * 0.03f + i * 0.75f) * 14);

            // Warna rumput laut bergantian
            Color weedColor = (i % 3 == 0)
                ? new Color(20, 130, 80, 160)
                : (i % 3 == 1)
                    ? new Color(15, 100, 60, 140)
                    : new Color(40, 160, 90, 150);

            g.setColor(weedColor);
            int midX = baseX + sway / 2;
            int topX = baseX + sway;
            // Dua segmen untuk efek melengkung
            g.drawLine(baseX, baseY, midX, baseY - height / 2);
            g.drawLine(midX,  baseY - height / 2, topX, baseY - height);
        }
        g.setStroke(new BasicStroke(1f));
    }

    public float getBgScroll() { return bgScroll; }
}
