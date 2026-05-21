package com.mycompany.bullethmath;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * BaseForm.java
 * JFrame dasar dengan latar belakang laut animasi:
 * gelembung naik, banyak ikan bergerak, kepiting, kerang, dan bintang laut.
 * Semua form di game ini extends BaseForm.
 */
public abstract class BaseForm extends JFrame {

    protected final int W, H;

    // Gelembung animasi
    private static final int BUBBLE_COUNT = 18;
    private final float[] bx = new float[BUBBLE_COUNT];
    private final float[] by = new float[BUBBLE_COUNT];
    private final int[] br = new int[BUBBLE_COUNT];
    private final float[] bspeed = new float[BUBBLE_COUNT];

    // Ikan animasi background
    private static final int FISH_COUNT = 20;
    private final float[] fishX = new float[FISH_COUNT];
    private final float[] fishY = new float[FISH_COUNT];
    private final float[] fishBaseY = new float[FISH_COUNT];
    private final float[] fishSpeed = new float[FISH_COUNT];
    private final float[] fishPhase = new float[FISH_COUNT];
    private final int[] fishW = new int[FISH_COUNT];
    private final int[] fishH = new int[FISH_COUNT];
    private final int[] fishDir = new int[FISH_COUNT];
    private final Color[] fishColor = new Color[FISH_COUNT];

    private final Random rng = new Random();

    // ── Kepiting animasi ──────────────────────────────────────────────────
    private static final int CRAB_COUNT = 3;
    private final float[] crabX     = new float[CRAB_COUNT];
    private final float[] crabY     = new float[CRAB_COUNT];
    private final float[] crabSpeed = new float[CRAB_COUNT];
    private final int[]   crabDir   = new int[CRAB_COUNT];
    private final Color[] crabColor = {
        new Color(255, 118, 90),
        new Color(255, 140, 105),
        new Color(240, 100, 80)
    };
    private float crabLegPhase = 0f;

    // ── Ubur-ubur animasi ─────────────────────────────────────────────────
    private static final int JELLY_COUNT = 4;
    private final float[] jellyX     = new float[JELLY_COUNT];
    private final float[] jellyY     = new float[JELLY_COUNT];
    private final float[] jellyPhase = new float[JELLY_COUNT];  // wobble
    private final float[] jellySpeed = new float[JELLY_COUNT];  // naik
    private final int[]   jellySize  = new int[JELLY_COUNT];
    private final Color[] jellyColor = {
        new Color(220, 150, 255, 200),
        new Color(130, 200, 255, 190),
        new Color(255, 160, 210, 195),
        new Color(160, 240, 220, 185)
    };

    // ── Penyu animasi ─────────────────────────────────────────────────────
    private static final int TURTLE_COUNT = 2;
    private final float[] turtleX     = new float[TURTLE_COUNT];
    private final float[] turtleY     = new float[TURTLE_COUNT];
    private final float[] turtlePhase = new float[TURTLE_COUNT]; // naik-turun
    private final float[] turtleSpeed = new float[TURTLE_COUNT];
    private final int[]   turtleDir   = new int[TURTLE_COUNT];
    private final int[]   turtleSize  = new int[TURTLE_COUNT];
    private float turtleFlipperPhase  = 0f;

    protected JPanel contentPane;
    private Timer bubbleTimer;

    protected BaseForm(String title, int width, int height) {
        super(title);
        // Gunakan ukuran layar penuh (dikurangi taskbar)
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                               .getDefaultScreenDevice()
                               .getDefaultConfiguration()
        );
        int sw = screen.width  - insets.left - insets.right;
        int sh = screen.height - insets.top  - insets.bottom;
        this.W = sw;
        this.H = sh;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setExtendedState(JFrame.MAXIMIZED_BOTH);   // maximize penuh
        setSize(W, H);
        setLocationRelativeTo(null);

        initBubbles();
        initFish();
        initCrabs();
        initJellies();
        initTurtles();
        setupContentPane();
        startBubbleTimer();
    }

    private void initBubbles() {
        for (int i = 0; i < BUBBLE_COUNT; i++) {
            resetBubble(i, true);
        }
    }

    private void resetBubble(int i, boolean randomY) {
        br[i] = 6 + rng.nextInt(22);
        bx[i] = rng.nextFloat() * (W - 20) + 10;
        by[i] = randomY ? rng.nextFloat() * H : H + br[i] + rng.nextInt(60);
        bspeed[i] = 0.8f + rng.nextFloat() * 2.0f;   // lebih cepat
    }

    private void initFish() {
        for (int i = 0; i < FISH_COUNT; i++) {
            resetFish(i, true);
        }
    }

    private void resetFish(int i, boolean randomX) {
        fishDir[i] = rng.nextBoolean() ? 1 : -1;
        fishW[i] = 24 + rng.nextInt(28);
        fishH[i] = Math.max(14, fishW[i] / 2);
        fishSpeed[i] = 0.7f + rng.nextFloat() * 1.8f;   // lebih cepat
        fishPhase[i] = rng.nextFloat() * 6.28f;
        fishBaseY[i] = 55 + rng.nextFloat() * Math.max(80, H - 220);
        fishY[i] = fishBaseY[i];

        fishX[i] = randomX
            ? rng.nextFloat() * W
            : (fishDir[i] > 0
                ? -fishW[i] - rng.nextInt(90)
                : W + rng.nextInt(90));

        Color[] palette = {
            new Color(255, 160, 122, 185),
            new Color(255, 209, 102, 185),
            new Color(125, 211, 252, 185),
            new Color(255, 180, 162, 185),
            new Color(154, 230, 180, 185),
            new Color(208, 180, 255, 185)
        };

        fishColor[i] = palette[rng.nextInt(palette.length)];
    }

    private void initCrabs() {
        // Posisi awal kepiting tersebar di area pasir bawah
        float[] startX = { 62f, W - 82f, W / 2f + 170f };
        for (int i = 0; i < CRAB_COUNT; i++) {
            crabX[i]     = startX[i];
            crabY[i]     = H - 70 - i * 2;
            crabDir[i]   = (i % 2 == 0) ? 1 : -1;
            crabSpeed[i] = 0.8f + rng.nextFloat() * 0.9f;
        }
    }

    private void resetCrab(int i) {
        // Muncul dari sisi berlawanan setelah keluar layar
        crabDir[i]   = (rng.nextBoolean()) ? 1 : -1;
        crabSpeed[i] = 0.8f + rng.nextFloat() * 0.9f;
        crabX[i]     = (crabDir[i] > 0) ? -50f : W + 50f;
        crabY[i]     = H - 65 - rng.nextInt(20);
    }

    private void initJellies() {
        for (int i = 0; i < JELLY_COUNT; i++) {
            jellySize[i]  = 38 + rng.nextInt(28);
            jellyX[i]     = 60 + rng.nextFloat() * (W - 120);
            jellyY[i]     = (int)(H * 0.30f) + rng.nextFloat() * (int)(H * 0.45f);
            jellyPhase[i] = rng.nextFloat() * 6.28f;
            jellySpeed[i] = 0.18f + rng.nextFloat() * 0.22f;
        }
    }

    private void resetJelly(int i) {
        jellySize[i]  = 38 + rng.nextInt(28);
        jellyX[i]     = 40 + rng.nextFloat() * (W - 80);
        jellyY[i]     = H + jellySize[i] + 20;
        jellyPhase[i] = rng.nextFloat() * 6.28f;
        jellySpeed[i] = 0.18f + rng.nextFloat() * 0.22f;
    }

    private void initTurtles() {
        for (int i = 0; i < TURTLE_COUNT; i++) {
            turtleSize[i]  = 52 + rng.nextInt(24);
            turtleDir[i]   = (i % 2 == 0) ? 1 : -1;
            turtleSpeed[i] = 0.28f + rng.nextFloat() * 0.22f;
            turtlePhase[i] = rng.nextFloat() * 6.28f;
            turtleX[i]     = rng.nextFloat() * W;
            turtleY[i]     = (int)(H * 0.25f) + rng.nextFloat() * (int)(H * 0.40f);
        }
    }

    private void resetTurtle(int i) {
        turtleSize[i]  = 52 + rng.nextInt(24);
        turtleDir[i]   = (rng.nextBoolean()) ? 1 : -1;
        turtleSpeed[i] = 0.28f + rng.nextFloat() * 0.22f;
        turtlePhase[i] = rng.nextFloat() * 6.28f;
        turtleX[i]     = (turtleDir[i] > 0) ? -turtleSize[i] - 20 : W + turtleSize[i] + 20;
        turtleY[i]     = (int)(H * 0.20f) + rng.nextFloat() * (int)(H * 0.45f);
    }

    private void setupContentPane() {
        contentPane = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                UITheme.drawOceanBg(g2, getWidth(), getHeight());
                drawMovingFish(g2);
                drawAnimatedJellies(g2);
                drawAnimatedTurtles(g2);
                UITheme.drawBubbles(g2, bx, by, br);
                drawSharedDecor(g2);
                drawExtras(g2);

                g2.dispose();
            }
        };

        contentPane.setOpaque(true);
        contentPane.setPreferredSize(new Dimension(W, H));
        setContentPane(contentPane);
    }

    /** Override di subclass untuk menambah elemen dekoratif tambahan */
    protected void drawExtras(Graphics2D g2) {}

    private void drawMovingFish(Graphics2D g2) {
        for (int i = 0; i < FISH_COUNT; i++) {
            int x = (int) fishX[i];
            int y = (int) fishY[i];
            int w = fishW[i];
            int h = fishH[i];
            int d = fishDir[i];
            Color body = fishColor[i];

            Graphics2D fishG = (Graphics2D) g2.create();

            if (d < 0) {
                fishG.translate(x + w, y);
                fishG.scale(-1, 1);
                x = 0;
                y = 0;
            }

            // Bayangan lembut
            fishG.setColor(new Color(33, 111, 154, 28));
            fishG.fillOval(x - 2, y + h - 2, w + 8, Math.max(6, h / 2));

            // Badan ikan
            fishG.setColor(body);
            fishG.fillOval(x, y, w, h);

            // Ekor
            int[] tailX = {x + 3, x - h, x - h};
            int[] tailY = {y + h / 2, y, y + h};
            fishG.fillPolygon(tailX, tailY, 3);

            // Kilau badan
            fishG.setColor(new Color(255, 255, 255, 95));
            fishG.fillOval(x + 5, y + 3, Math.max(8, w / 3), Math.max(5, h / 3));

            // Mata
            fishG.setColor(Color.WHITE);
            fishG.fillOval(x + w - 10, y + h / 3, 7, 7);

            fishG.setColor(new Color(40, 70, 90));
            fishG.fillOval(x + w - 8, y + h / 3 + 2, 3, 3);

            // Senyum kecil
            fishG.setColor(body.darker());
            fishG.setStroke(new BasicStroke(1.2f));
            fishG.drawArc(x + w - 15, y + h / 2 - 1, 8, 6, 200, 140);

            fishG.dispose();
        }
    }

    private void drawSharedDecor(Graphics2D g2) {
        // ── Rumput laut berlapis (belakang dulu) ──────────────────────────────
        // Lapisan belakang: gelap, pendek
        drawSeaweedBack(g2, 14,  H - 22, new Color(45, 105, 32, 190), 0.15f);
        drawSeaweedBack(g2, 32,  H - 20, new Color(38, 95,  28, 180), 1.0f);
        drawSeaweedBack(g2, 52,  H - 24, new Color(50, 112, 36, 185), 0.4f);
        drawSeaweedBack(g2, W - 66, H - 22, new Color(42, 100, 30, 185), 0.8f);
        drawSeaweedBack(g2, W - 44, H - 20, new Color(48, 108, 34, 180), 0.2f);
        drawSeaweedBack(g2, W - 22, H - 24, new Color(44, 102, 32, 190), 0.6f);
        // Tengah
        drawSeaweedBack(g2, W / 4 - 8,  H - 20, new Color(40, 98,  30, 175), 0.3f);
        drawSeaweedBack(g2, W * 3/4 + 6, H - 22, new Color(46, 106, 34, 175), 0.7f);

        // Lapisan depan: terang, tinggi, bergoyang
        drawSeaweedCluster(g2, 22,  H - 24, new Color(88, 194, 65, 210), 0.2f);
        drawSeaweedCluster(g2, 46,  H - 18, new Color(72, 181, 55, 220), 1.1f);
        drawSeaweedCluster(g2, 70,  H - 22, new Color(96, 200, 70, 210), 0.5f);
        drawSeaweedCluster(g2, W - 90, H - 22, new Color(80, 188, 60, 215), 0.9f);
        drawSeaweedCluster(g2, W - 62, H - 18, new Color(90, 196, 68, 220), 0.3f);
        drawSeaweedCluster(g2, W - 34, H - 22, new Color(76, 184, 58, 210), 0.7f);
        // Tengah kiri & kanan
        drawSeaweedCluster(g2, W / 4 + 5,  H - 20, new Color(84, 192, 63, 200), 0.4f);
        drawSeaweedCluster(g2, W * 3/4 - 5, H - 22, new Color(92, 198, 67, 200), 0.8f);

        // ── Karang ────────────────────────────────────────────────────────────
        drawCoral(g2, 98,  H - 28, new Color(255, 118, 142, 210));
        drawCoral(g2, 136, H - 24, new Color(255, 145, 80,  210));
        drawCoral(g2, W - 122, H - 28, new Color(165, 100, 238, 210));
        drawCoral(g2, W - 158, H - 24, new Color(255, 100, 160, 210));

        // ── Kerang ────────────────────────────────────────────────────────────
        drawShell(g2, 168,  H - 30, new Color(255, 200, 218));
        drawShell(g2, W - 192, H - 32, new Color(218, 195, 255));
        drawShell(g2, W / 2 - 125, H - 24, new Color(255, 228, 180));
        drawShell(g2, W / 2 + 70,  H - 26, new Color(200, 230, 255));

        // ── Bintang laut ──────────────────────────────────────────────────────
        drawStarfish(g2, W / 2,     H - 18, new Color(255, 172, 80));
        drawStarfish(g2, 238,       H - 22, new Color(255, 198, 100));
        drawStarfish(g2, W - 260,   H - 20, new Color(255, 155, 70));

        // ── Bulu babi ─────────────────────────────────────────────────────────
        drawUrchin(g2, W / 3 + 15,  H - 14, new Color(75, 45, 115));
        drawUrchin(g2, W * 2/3 - 15, H - 14, new Color(110, 55, 135));
        drawUrchin(g2, 195,          H - 14, new Color(55, 38, 95));

        // ── Kepiting animasi ──────────────────────────────────────────────────
        drawAnimatedCrabs(g2);

        // ── Siput laut ────────────────────────────────────────────────────────
        drawSnail(g2, W / 2 - 15, H - 16, new Color(195, 155, 215));
        drawSnail(g2, W * 3/4 + 35, H - 14, new Color(175, 215, 250));
    }

    private void drawSeaweedCluster(Graphics2D g2, int x, int y, Color color, float phase) {
        // Batang utama berlapis dengan daun oval
        int segments = 6;
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int px = x, py = y;
        for (int i = 0; i < segments; i++) {
            float t = i / (float) segments;
            int sway = (int)(Math.sin((by[0] + phase + i * 0.8f) * 0.04f) * (8 + i * 2));
            int nx = x + sway;
            int ny = y - (i + 1) * (50 / segments);

            int r = Math.min(255, color.getRed()   + (int)(t * 35));
            int g = Math.min(255, color.getGreen() + (int)(t * 40));
            int b = Math.min(255, color.getBlue()  + (int)(t * 15));
            int alpha = color.getAlpha() - (int)(t * 25);
            g2.setColor(new Color(r, g, b, Math.max(0, alpha)));
            g2.drawLine(px, py, nx, ny);

            // Daun di setiap 2 segmen
            if (i % 2 == 0) {
                int leafDir = (i % 4 == 0) ? 1 : -1;
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(Math.min(255, r + 15), Math.min(255, g + 20), b, 190));
                g2.fillOval(nx + leafDir * 4, ny - 3, 12, 6);
                g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            px = nx; py = ny;
        }
        // Ujung
        g2.setColor(new Color(
            Math.min(255, color.getRed() + 40),
            Math.min(255, color.getGreen() + 45),
            Math.min(255, color.getBlue() + 20),
            180
        ));
        g2.fillOval(px - 4, py - 5, 8, 10);
        g2.setStroke(new BasicStroke(1f));
    }

    /** Rumput laut lapisan belakang: lebih tipis, lebih gelap */
    private void drawSeaweedBack(Graphics2D g2, int x, int y, Color color, float phase) {
        int segments = 5;
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int px = x, py = y;
        for (int i = 0; i < segments; i++) {
            int sway = (int)(Math.sin((by[0] + phase + i * 0.7f) * 0.035f) * (5 + i * 1.5));
            int nx = x + sway;
            int ny = y - (i + 1) * (38 / segments);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                                  color.getAlpha() - i * 18));
            g2.drawLine(px, py, nx, ny);
            px = nx; py = ny;
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawCoral(Graphics2D g2, int x, int y, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.drawLine(x, y, x, y - 48);
        g2.drawLine(x, y - 22, x - 18, y - 38);
        g2.drawLine(x, y - 18, x + 18, y - 38);
        g2.drawLine(x + 2, y - 36, x + 18, y - 56);

        g2.setStroke(new BasicStroke(1f));
        g2.setColor(color.brighter());

        g2.fillOval(x - 6, y - 54, 12, 12);
        g2.fillOval(x - 24, y - 44, 12, 12);
        g2.fillOval(x + 12, y - 44, 12, 12);
        g2.fillOval(x + 12, y - 62, 12, 12);
    }

    private void drawShell(Graphics2D g2, int x, int y, Color color) {
        g2.setColor(new Color(107, 69, 95, 70));
        g2.fillOval(x - 18, y - 8, 42, 16);

        g2.setColor(color);
        g2.fillArc(x - 18, y - 28, 36, 32, 0, 180);

        g2.setColor(color.darker());
        for (int i = -1; i <= 1; i++) {
            g2.drawArc(x - 16 + i * 4, y - 26, 28 - Math.abs(i) * 5, 26, 0, 180);
        }
    }

    private void drawStarfish(Graphics2D g2, int cx, int cy, Color color) {
        int[] xp = new int[10];
        int[] yp = new int[10];

        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            int r = (i % 2 == 0) ? 18 : 8;

            xp[i] = cx + (int) (Math.cos(angle) * r);
            yp[i] = cy + (int) (Math.sin(angle) * r);
        }

        g2.setColor(color);
        g2.fillPolygon(xp, yp, 10);

        g2.setColor(color.darker());
        g2.drawPolygon(xp, yp, 10);
    }

    /** Gambar semua kepiting animasi */
    private void drawAnimatedCrabs(Graphics2D g2) {
        for (int i = 0; i < CRAB_COUNT; i++) {
            drawAnimatedCrab(g2, (int) crabX[i], (int) crabY[i],
                             crabColor[i], crabDir[i], crabLegPhase + i * 1.2f);
        }
    }

    // ── Ubur-ubur kawaii animasi ───────────────────────────────────────────
    private void drawAnimatedJellies(Graphics2D g2) {
        for (int i = 0; i < JELLY_COUNT; i++) {
            drawKawaiiJellyfish(g2, (int) jellyX[i], (int) jellyY[i],
                                jellySize[i], jellyColor[i], jellyPhase[i]);
        }
    }

    private void drawKawaiiJellyfish(Graphics2D g2, int cx, int cy,
                                      int size, Color col, float phase) {
        // Pulse: dome mengembang-mengempis
        float pulse = 1f + (float)(Math.sin(phase * 2) * 0.06f);
        int dw = (int)(size * pulse);
        int dh = (int)(size * 0.65f * pulse);

        // ── Aura glow luar ────────────────────────────────────────────────
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 35));
        g2.fillOval(cx - dw/2 - 8, cy - dh/2 - 8, dw + 16, dh + 16);

        // ── Dome utama ────────────────────────────────────────────────────
        GradientPaint gp = new GradientPaint(
            cx, cy - dh/2, col.brighter(),
            cx, cy + dh/2, new Color(col.getRed(), col.getGreen(), col.getBlue(), 160)
        );
        g2.setPaint(gp);
        g2.fillArc(cx - dw/2, cy - dh/2, dw, dh, 0, 180);
        g2.setPaint(null);

        // ── Kilap dome ────────────────────────────────────────────────────
        g2.setColor(new Color(255, 255, 255, 80));
        g2.fillArc(cx - dw/4, cy - dh/2 + 4, dw/3, dh/3, 30, 120);

        // ── Pola dalam dome (lingkaran konsentris) ────────────────────────
        g2.setColor(new Color(255, 255, 255, 40));
        g2.setStroke(new BasicStroke(1f));
        g2.drawArc(cx - dw/3, cy - dh/3, dw*2/3, dh/2, 10, 160);

        // ── Tentakel bergoyang ────────────────────────────────────────────
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int tentCount = 6;
        for (int t = 0; t < tentCount; t++) {
            float tx = cx - dw/2 + (float)(t + 0.5f) * dw / tentCount;
            float sway = (float)(Math.sin(phase + t * 0.8f) * 8);
            float sway2 = (float)(Math.sin(phase * 1.3f + t * 0.6f) * 6);
            int tentLen = size / 2 + (t % 2) * (size / 4);
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 180));
            // Tentakel melengkung 2 segmen
            g2.drawLine((int)tx, cy, (int)(tx + sway), cy + tentLen/2);
            g2.drawLine((int)(tx + sway), cy + tentLen/2,
                        (int)(tx + sway + sway2), cy + tentLen);
        }
        g2.setStroke(new BasicStroke(1f));

        // ── Wajah kawaii ──────────────────────────────────────────────────
        int faceY = cy - dh/4;
        // Mata kiri
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 10, faceY - 4, 9, 9);
        g2.setColor(new Color(30, 20, 50));
        g2.fillOval(cx - 8,  faceY - 2, 5, 5);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillOval(cx - 7,  faceY - 2, 2, 2);
        // Mata kanan
        g2.setColor(Color.WHITE);
        g2.fillOval(cx + 1,  faceY - 4, 9, 9);
        g2.setColor(new Color(30, 20, 50));
        g2.fillOval(cx + 3,  faceY - 2, 5, 5);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillOval(cx + 4,  faceY - 2, 2, 2);
        // Senyum
        g2.setColor(new Color(40, 20, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawArc(cx - 5, faceY + 3, 10, 6, 200, 140);
        // Pipi merah
        g2.setColor(new Color(255, 160, 180, 110));
        g2.fillOval(cx - 14, faceY + 2, 8, 5);
        g2.fillOval(cx + 6,  faceY + 2, 8, 5);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── Penyu kawaii animasi ───────────────────────────────────────────────
    private void drawAnimatedTurtles(Graphics2D g2) {
        for (int i = 0; i < TURTLE_COUNT; i++) {
            drawKawaiiTurtle(g2, (int) turtleX[i], (int) turtleY[i],
                             turtleSize[i], turtleDir[i],
                             turtleFlipperPhase + i * 1.5f);
        }
    }

    private void drawKawaiiTurtle(Graphics2D g2, int cx, int cy,
                                   int size, int dir, float phase) {
        Graphics2D tg = (Graphics2D) g2.create();
        tg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // Flip jika ke kiri
        if (dir < 0) {
            tg.translate(cx * 2, 0);
            tg.scale(-1, 1);
        }

        int sw = (int)(size * 1.1f);   // lebar cangkang
        int sh = (int)(size * 0.75f);  // tinggi cangkang

        // ── Bayangan ──────────────────────────────────────────────────────
        tg.setColor(new Color(0, 0, 0, 28));
        tg.fillOval(cx - sw/2 + 4, cy + sh/2 - 2, sw - 4, 12);

        // ── Sirip belakang ────────────────────────────────────────────────
        float flipB = (float)(Math.sin(phase + Math.PI) * 12);
        tg.setColor(new Color(60, 160, 80, 200));
        tg.fillOval(cx - sw/2 - 8, cy + (int)flipB - 4, 18, 12);

        // ── Kaki depan (bergerak mendayung) ───────────────────────────────
        float flipF = (float)(Math.sin(phase) * 14);
        tg.setColor(new Color(60, 160, 80, 210));
        // Kaki depan atas
        tg.fillOval(cx + sw/2 - 6, cy - 8 - (int)flipF, 20, 12);
        // Kaki depan bawah
        tg.fillOval(cx + sw/2 - 6, cy + sh/2 - 4 + (int)flipF, 20, 12);
        // Kaki belakang atas
        tg.fillOval(cx - sw/2 - 10, cy - 6 + (int)flipB/2, 18, 11);
        // Kaki belakang bawah
        tg.fillOval(cx - sw/2 - 10, cy + sh/2 - 6 - (int)flipB/2, 18, 11);

        // ── Cangkang ──────────────────────────────────────────────────────
        GradientPaint shellGp = new GradientPaint(
            cx, cy - sh/2, new Color(55, 175, 85),
            cx, cy + sh/2, new Color(30, 120, 55)
        );
        tg.setPaint(shellGp);
        tg.fillOval(cx - sw/2, cy - sh/2, sw, sh);
        tg.setPaint(null);

        // Pola cangkang (hexagon-ish)
        tg.setColor(new Color(25, 100, 45, 120));
        tg.setStroke(new BasicStroke(1.5f));
        tg.drawOval(cx - sw/2 + 4, cy - sh/2 + 4, sw - 8, sh - 8);
        // Garis pola
        tg.setStroke(new BasicStroke(1.2f));
        tg.drawLine(cx, cy - sh/2 + 4, cx, cy + sh/2 - 4);
        tg.drawLine(cx - sw/4, cy - sh/4, cx + sw/4, cy + sh/4);
        tg.drawLine(cx - sw/4, cy + sh/4, cx + sw/4, cy - sh/4);

        // Kilap cangkang
        tg.setColor(new Color(255, 255, 255, 55));
        tg.fillOval(cx - sw/4, cy - sh/3, sw/3, sh/4);

        // ── Kepala ────────────────────────────────────────────────────────
        int hx = cx + sw/2 - 4;
        int hy = cy - sh/6;
        tg.setColor(new Color(65, 175, 90));
        tg.fillOval(hx, hy, (int)(size * 0.38f), (int)(size * 0.32f));

        // Wajah kawaii
        int hw = (int)(size * 0.38f);
        int hh = (int)(size * 0.32f);
        // Mata
        tg.setColor(Color.WHITE);
        tg.fillOval(hx + hw - 10, hy + 3, 8, 8);
        tg.setColor(new Color(20, 30, 50));
        tg.fillOval(hx + hw - 8,  hy + 5, 4, 4);
        tg.setColor(new Color(255, 255, 255, 220));
        tg.fillOval(hx + hw - 7,  hy + 5, 2, 2);
        // Senyum
        tg.setColor(new Color(30, 100, 50));
        tg.setStroke(new BasicStroke(1.4f));
        tg.drawArc(hx + hw - 12, hy + hh - 7, 8, 5, 200, 140);
        // Pipi
        tg.setColor(new Color(255, 180, 160, 120));
        tg.fillOval(hx + hw - 14, hy + hh - 6, 6, 4);

        tg.setStroke(new BasicStroke(1f));
        tg.dispose();
    }

    /**
     * Kepiting animasi: kaki bergerak naik-turun bergantian,
     * capit naik-turun, badan sedikit bounce, flip horizontal sesuai arah.
     */
    private void drawAnimatedCrab(Graphics2D g2, int cx, int cy,
                                   Color color, int dir, float phase) {
        Graphics2D cg = (Graphics2D) g2.create();
        cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // Flip horizontal jika jalan ke kiri
        if (dir < 0) {
            cg.translate(cx * 2, 0);
            cg.scale(-1, 1);
        }

        // Bounce badan kecil saat jalan
        int bounce = (int)(Math.abs(Math.sin(phase * 2)) * 3);
        int y = cy - bounce;

        // ── Bayangan ──────────────────────────────────────────────────────
        cg.setColor(new Color(0, 0, 0, 35));
        cg.fillOval(cx - 24, y + 20, 48, 12);

        // ── Kaki (4 pasang, bergantian naik-turun) ────────────────────────
        cg.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[][] legBase = {{-20, 6}, {-12, 8}, {12, 8}, {20, 6}};
        for (int i = 0; i < 4; i++) {
            // Kaki kiri & kanan bergantian fase
            float legOff = (float) Math.sin(phase + i * 0.9f) * 5f;
            int lx = cx + legBase[i][0];
            int ly = y + legBase[i][1];
            // Kaki kiri
            cg.setColor(color.darker());
            cg.drawLine(lx, ly, lx - 10, ly + 12 + (int) legOff);
            // Kaki kanan (fase berlawanan)
            float legOffR = (float) Math.sin(phase + i * 0.9f + Math.PI) * 5f;
            cg.drawLine(lx, ly, lx + 10, ly + 12 + (int) legOffR);
        }

        // ── Capit (naik-turun) ────────────────────────────────────────────
        float clawAngle = (float) Math.sin(phase * 1.5f) * 0.3f;
        // Capit kiri
        cg.setColor(color.darker());
        int clawLX = cx - 34;
        int clawLY = y - 8 + (int)(Math.sin(phase) * 4);
        cg.fillOval(clawLX, clawLY, 18, 14);
        // Ujung capit (buka-tutup)
        cg.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int openL = (int)(Math.abs(Math.sin(phase * 1.8f)) * 5);
        cg.drawLine(clawLX, clawLY + 7, clawLX - 8, clawLY + 7 - openL);
        cg.drawLine(clawLX, clawLY + 7, clawLX - 8, clawLY + 7 + openL);
        // Capit kanan
        int clawRX = cx + 16;
        int clawRY = y - 8 + (int)(Math.sin(phase + 0.5f) * 4);
        cg.setColor(color.darker());
        cg.fillOval(clawRX, clawRY, 18, 14);
        int openR = (int)(Math.abs(Math.sin(phase * 1.8f + 0.4f)) * 5);
        cg.drawLine(clawRX + 18, clawRY + 7, clawRX + 26, clawRY + 7 - openR);
        cg.drawLine(clawRX + 18, clawRY + 7, clawRX + 26, clawRY + 7 + openR);

        // ── Badan utama ───────────────────────────────────────────────────
        cg.setPaint(new GradientPaint(cx - 22, y - 8, color.brighter(),
                                      cx + 22, y + 20, color.darker()));
        cg.fillOval(cx - 22, y - 8, 44, 28);
        cg.setPaint(null);
        // Highlight badan
        cg.setColor(new Color(255, 255, 255, 75));
        cg.fillOval(cx - 14, y - 4, 16, 8);
        // Pola badan (garis melengkung)
        cg.setColor(new Color(0, 0, 0, 30));
        cg.setStroke(new BasicStroke(1f));
        cg.drawArc(cx - 16, y - 2, 32, 18, 0, 180);
        cg.drawArc(cx - 10, y + 2, 20, 12, 0, 180);

        // ── Tangkai mata ──────────────────────────────────────────────────
        cg.setColor(color.darker());
        cg.setStroke(new BasicStroke(2f));
        cg.drawLine(cx - 10, y - 8, cx - 10, y - 18);
        cg.drawLine(cx + 10, y - 8, cx + 10, y - 18);

        // ── Mata ──────────────────────────────────────────────────────────
        cg.setColor(Color.WHITE);
        cg.fillOval(cx - 15, y - 24, 10, 10);
        cg.fillOval(cx + 5,  y - 24, 10, 10);
        cg.setColor(new Color(20, 30, 50));
        cg.fillOval(cx - 12, y - 21, 5, 5);
        cg.fillOval(cx + 8,  y - 21, 5, 5);
        // Kilap mata
        cg.setColor(new Color(255, 255, 255, 200));
        cg.fillOval(cx - 11, y - 22, 2, 2);
        cg.fillOval(cx + 9,  y - 22, 2, 2);

        // ── Senyum ────────────────────────────────────────────────────────
        cg.setColor(color.darker());
        cg.setStroke(new BasicStroke(1.8f));
        cg.drawArc(cx - 8, y + 1, 16, 10, 200, 140);

        cg.setStroke(new BasicStroke(1f));
        cg.dispose();
    }

    /** Bulu babi (sea urchin) */
    private void drawUrchin(Graphics2D g2, int cx, int cy, Color color) {
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16;
            int x1 = cx + (int)(Math.cos(angle) * 7);
            int y1 = cy + (int)(Math.sin(angle) * 7);
            int x2 = cx + (int)(Math.cos(angle) * 14);
            int y2 = cy + (int)(Math.sin(angle) * 14);
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.setColor(color);
        g2.fillOval(cx - 7, cy - 7, 14, 14);
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(cx - 7, cy - 7, 14, 14);
        g2.setColor(new Color(255, 255, 255, 55));
        g2.fillOval(cx - 3, cy - 4, 5, 4);
        g2.setStroke(new BasicStroke(1f));
    }

    /** Siput laut kecil */
    private void drawSnail(Graphics2D g2, int cx, int cy, Color color) {
        g2.setColor(color);
        g2.fillOval(cx - 10, cy - 8, 20, 16);
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(cx - 10, cy - 8, 20, 16);
        g2.setStroke(new BasicStroke(1f));
        g2.drawArc(cx - 6, cy - 5, 12, 10, 20, 300);
        g2.drawArc(cx - 3, cy - 3, 6, 5, 30, 280);
        g2.setColor(new Color(255, 255, 255, 80));
        g2.fillOval(cx - 6, cy - 6, 6, 4);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
        g2.fillOval(cx - 12, cy + 4, 24, 8);
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx - 4, cy - 8, cx - 8, cy - 16);
        g2.drawLine(cx + 2, cy - 8, cx + 6, cy - 16);
        g2.fillOval(cx - 10, cy - 18, 4, 4);
        g2.fillOval(cx + 4, cy - 18, 4, 4);
        g2.setStroke(new BasicStroke(1f));
    }

    private void startBubbleTimer() {
        bubbleTimer = new Timer(20, e -> {   // 20ms = ~50fps, lebih smooth
            // ── Gelembung ─────────────────────────────────────────────────
            for (int i = 0; i < BUBBLE_COUNT; i++) {
                by[i] -= bspeed[i];
                bx[i] += (float) (Math.sin(by[i] * 0.03 + i) * 0.3);
                if (by[i] + br[i] < 0) resetBubble(i, false);
            }

            // ── Ikan ──────────────────────────────────────────────────────
            for (int i = 0; i < FISH_COUNT; i++) {
                fishPhase[i] += 0.04f + i * 0.0008f;
                fishX[i] += fishDir[i] * fishSpeed[i];
                fishY[i] = fishBaseY[i]
                    + (float) (Math.sin(fishPhase[i]) * (5 + (i % 4)));
                boolean offRight = fishDir[i] > 0 && fishX[i] > W + fishW[i] + 24;
                boolean offLeft  = fishDir[i] < 0 && fishX[i] < -fishW[i] - 24;
                if (offRight || offLeft) resetFish(i, false);
            }

            // ── Kepiting ──────────────────────────────────────────────────
            crabLegPhase += 0.20f;   // kaki lebih cepat
            for (int i = 0; i < CRAB_COUNT; i++) {
                crabX[i] += crabDir[i] * crabSpeed[i];
                if (crabX[i] > W + 60) resetCrab(i);
                if (crabX[i] < -60)    resetCrab(i);
            }

            // ── Ubur-ubur ─────────────────────────────────────────────────
            for (int i = 0; i < JELLY_COUNT; i++) {
                jellyPhase[i] += 0.08f;   // lebih cepat
                jellyY[i] -= jellySpeed[i] * 1.8f;
                jellyX[i] += (float)(Math.sin(jellyPhase[i] * 0.6f) * 0.6f);
                if (jellyY[i] + jellySize[i] < 0) resetJelly(i);
            }

            // ── Penyu ─────────────────────────────────────────────────────
            turtleFlipperPhase += 0.14f;   // sirip lebih cepat
            for (int i = 0; i < TURTLE_COUNT; i++) {
                turtlePhase[i] += 0.04f;
                turtleX[i] += turtleDir[i] * turtleSpeed[i] * 1.6f;
                turtleY[i] += (float)(Math.sin(turtlePhase[i]) * 0.5f);
                if (turtleX[i] > W + turtleSize[i] + 30) resetTurtle(i);
                if (turtleX[i] < -turtleSize[i] - 30)    resetTurtle(i);
            }

            contentPane.repaint();
        });
        bubbleTimer.start();
    }

    @Override
    public void dispose() {
        if (bubbleTimer != null) {
            bubbleTimer.stop();
        }

        super.dispose();
    }

    // ── Helper: tambah komponen ke posisi absolut ─────────────────────────────
    protected void place(JComponent comp, int x, int y, int w, int h) {
        comp.setBounds(x, y, w, h);
        contentPane.add(comp);
    }

    protected void place(JComponent comp, Rectangle r) {
        place(comp, r.x, r.y, r.width, r.height);
    }

    /** Label teks transparan (teks di atas latar) */
    protected JLabel makeLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(font);
        lbl.setForeground(color);
        lbl.setOpaque(false);
        return lbl;
    }
}