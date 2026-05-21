package com.mycompany.bullethmath;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * UITheme.java
 * Central style sheet untuk tema bawah laut lucu ramah anak.
 */
public class UITheme {

    // ── Palet Warna ───────────────────────────────────────────────────────
    public static final Color OCEAN_ABYSS  = new Color(0, 59, 126);
    public static final Color OCEAN_DEEP   = new Color(0, 89, 158);
    public static final Color OCEAN_MID    = new Color(0, 141, 204);
    public static final Color OCEAN_LIGHT  = new Color(32, 188, 224);
    public static final Color OCEAN_SURF   = new Color(170, 243, 255);

    public static final Color CORAL        = new Color(255, 123, 145);
    public static final Color CORAL_DARK   = new Color(219, 74, 103);
    public static final Color GOLD         = new Color(255, 214, 76);
    public static final Color GOLD_DARK    = new Color(235, 167, 36);
    public static final Color SEA_GREEN    = new Color(128, 204, 73);
    public static final Color SEA_GREEN_D  = new Color(87, 162, 47);
    public static final Color CRIMSON      = new Color(237, 84, 100);

    public static final Color PANEL_GLASS  = new Color(255, 248, 226, 210);
    public static final Color PANEL_BORDER = new Color(255, 255, 255, 205);

    public static final Color TEXT_WHITE   = new Color(255, 252, 244);
    public static final Color TEXT_DARK    = new Color(23, 69, 106);
    public static final Color TEXT_SOFT    = new Color(41, 92, 128);
    public static final Color TEXT_SHADOW  = new Color(0, 0, 0, 120);

    public static final Color STAR_ON      = new Color(255, 215, 0);
    public static final Color STAR_OFF     = new Color(100, 100, 130);
    public static final Color LOCK_COLOR   = new Color(60, 60, 90);

    // ── Font ──────────────────────────────────────────────────────────────
    public static final Font TITLE_FONT  = loadFont("SansSerif", Font.BOLD, 38);
    public static final Font HEADER_FONT = loadFont("SansSerif", Font.BOLD, 26);
    public static final Font SUBHEAD     = loadFont("SansSerif", Font.BOLD, 20);
    public static final Font BODY_FONT   = loadFont("SansSerif", Font.BOLD, 17);
    public static final Font SMALL_FONT  = loadFont("SansSerif", Font.PLAIN, 13);
    public static final Font HUGE_FONT   = loadFont("SansSerif", Font.BOLD, 52);
    public static final Font MONO_FONT   = loadFont("Monospaced", Font.BOLD, 18);

    private static Font loadFont(String family, int style, int size) {
        return new Font(family, style, size);
    }

    // ── Helper: Tombol Rounded ────────────────────────────────────────────
    public static JButton makeButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color c = bg;
                if (getModel().isPressed()) {
                    c = bg.darker().darker();
                } else if (getModel().isRollover()) {
                    c = bg.brighter();
                }

                // Shadow bawah
                g2.setColor(new Color(102, 59, 35, 85));
                g2.fillRoundRect(3, 6, getWidth() - 4, getHeight() - 5, 28, 28);

                // Body
                GradientPaint gp = new GradientPaint(
                    0, 0, c.brighter(),
                    0, getHeight(), c.darker()
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 5, 28, 28);

                // Highlight atas
                g2.setColor(new Color(255, 255, 255, 95));
                g2.fillRoundRect(
                    1, 1,
                    getWidth() - 4,
                    Math.max(12, (getHeight() - 5) / 2),
                    26, 26
                );

                g2.setColor(new Color(255, 255, 255, 150));
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(1, 1, getWidth() - 4, getHeight() - 7, 27, 27);

                // Teks
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - 2 - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - 4 + fm.getAscent() - fm.getDescent()) / 2;

                g2.setColor(new Color(0, 0, 0, 80));
                g2.drawString(getText(), tx + 1, ty + 1);

                g2.setColor(fg);
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };

        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(fg);
        btn.setFont(BODY_FONT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(230, 52));
        // SFX klik
        btn.addActionListener(e -> SoundManager.getInstance().play("click"));
        return btn;
    }

    public static JButton makeSmallButton(String text, Color bg, Color fg) {
        JButton btn = makeButton(text, bg, fg);
        btn.setFont(SMALL_FONT.deriveFont(Font.BOLD, 14f));
        btn.setPreferredSize(new Dimension(130, 40));
        return btn;
    }

    // ── Helper: Input Field ───────────────────────────────────────────────
    public static JTextField makeField() {
        JTextField field = new JTextField(20);
        field.setFont(BODY_FONT);
        field.setForeground(new Color(76, 50, 74));
        field.setBackground(new Color(255, 249, 234));
        field.setCaretColor(new Color(76, 50, 74));
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(new Color(255, 255, 255, 160), 14, 2),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        field.setOpaque(true);
        return field;
    }

    public static JPasswordField makePassField() {
        JPasswordField field = new JPasswordField(20);
        field.setFont(BODY_FONT);
        field.setForeground(new Color(76, 50, 74));
        field.setBackground(new Color(255, 249, 234));
        field.setCaretColor(new Color(76, 50, 74));
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(new Color(255, 255, 255, 160), 14, 2),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        field.setOpaque(true);
        return field;
    }

    // ── Background Laut Ilustratif ────────────────────────────────────────
    public static void drawOceanBg(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // ── 1. Gradien laut: biru muda cerah di atas → biru tua di bawah ─────
        GradientPaint ocean = new GradientPaint(
            0, 0,    new Color(30, 160, 220),
            0, h,    new Color(5,  55,  130)
        );
        g2.setPaint(ocean);
        g2.fillRect(0, 0, w, h);

        // ── 2. Cahaya matahari dari tengah atas (radial glow putih) ───────────
        RadialGradientPaint sunGlow = new RadialGradientPaint(
            new Point(w / 2, 0),
            (int)(Math.max(w, h) * 0.85f),
            new float[]{0f, 0.30f, 0.65f, 1f},
            new Color[]{
                new Color(255, 255, 255, 220),
                new Color(180, 235, 255, 130),
                new Color(80,  180, 230, 40),
                new Color(10,  80,  170, 0)
            }
        );
        g2.setPaint(sunGlow);
        g2.fillRect(0, 0, w, h);

        // ── 3. Sinar matahari (god rays) dari tengah atas ─────────────────────
        drawSunRays(g2, w, h);

        // ── 4. Bokeh gelembung besar transparan (kiri & kanan) ────────────────
        drawBokehBubbles(g2, w, h);

        // ── 5. Siluet ikan kecil di tengah air ───────────────────────────────
        drawFishSilhouettes(g2, w, h);

        // ── 6. Siluet terumbu karang di kejauhan (biru gelap) ─────────────────
        drawDistantReef(g2, w, h);

        // ── 7. Dasar pasir ────────────────────────────────────────────────────
        drawSeabed(g2, w, h);

        // ── 8. Gelembung kecil statis ─────────────────────────────────────────
        drawTinyBubbles(g2, w, h);

        // ── 9. Dekorasi foreground (karang, rumput laut, hewan) ───────────────
        drawForegroundDecor(g2, w, h);
    }

    private static void drawSunRays(Graphics2D g2, int w, int h) {
        // Sinar dari titik tengah atas, menyebar ke bawah seperti referensi
        int sunX = w / 2, sunY = -20;
        int numRays = 18;
        Composite old = g2.getComposite();
        for (int i = 0; i < numRays; i++) {
            double baseAngle = -Math.PI / 2 + (i - numRays / 2.0) * (Math.PI / numRays) * 1.6;
            double a1 = baseAngle - 0.03;
            double a2 = baseAngle + 0.03;
            int len = (int)(Math.max(w, h) * 1.1);
            float alpha = (i % 2 == 0) ? 0.13f : 0.07f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(new Color(200, 240, 255));
            g2.fillPolygon(
                new int[]{sunX, sunX + (int)(Math.cos(a1)*len), sunX + (int)(Math.cos(a2)*len)},
                new int[]{sunY, sunY + (int)(Math.sin(a1)*len), sunY + (int)(Math.sin(a2)*len)},
                3
            );
        }
        g2.setComposite(old);
    }

    /** Bokeh: gelembung besar transparan seperti di gambar referensi */
    private static void drawBokehBubbles(Graphics2D g2, int w, int h) {
        // [x, y, radius]
        int[][] bokehs = {
            {(int)(w*0.12), (int)(h*0.18), 48},
            {(int)(w*0.08), (int)(h*0.38), 28},
            {(int)(w*0.18), (int)(h*0.28), 18},
            {(int)(w*0.88), (int)(h*0.22), 36},
            {(int)(w*0.92), (int)(h*0.42), 22},
            {(int)(w*0.50), (int)(h*0.12), 20},
            {(int)(w*0.35), (int)(h*0.55), 14},
            {(int)(w*0.72), (int)(h*0.60), 16},
        };
        Composite old = g2.getComposite();
        for (int[] b : bokehs) {
            int bx = b[0], by = b[1], r = b[2];
            // Isi putih lembut
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            g2.setColor(new Color(220, 245, 255));
            g2.fillOval(bx - r, by - r, r * 2, r * 2);
            // Border putih
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(bx - r, by - r, r * 2, r * 2);
            // Kilap dalam
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.fillOval(bx - r/3, by - r/2, r/3, r/4);
        }
        g2.setComposite(old);
        g2.setStroke(new BasicStroke(1f));
    }

    /** Siluet ikan kecil di tengah air seperti gambar referensi */
    private static void drawFishSilhouettes(Graphics2D g2, int w, int h) {
        // Kelompok ikan siluet biru gelap
        int[][] groups = {
            {(int)(w*0.08), (int)(h*0.35), 3, 10},
            {(int)(w*0.12), (int)(h*0.48), 2, 8},
            {(int)(w*0.82), (int)(h*0.30), 4, 11},
            {(int)(w*0.88), (int)(h*0.45), 3, 9},
            {(int)(w*0.75), (int)(h*0.55), 2, 8},
            {(int)(w*0.25), (int)(h*0.52), 3, 10},
            {(int)(w*0.55), (int)(h*0.40), 2, 9},
        };
        Color silColor = new Color(10, 55, 120, 100);
        for (int[] grp : groups) {
            int gx = grp[0], gy = grp[1], cnt = grp[2], sz = grp[3];
            for (int f = 0; f < cnt; f++) {
                int fx = gx + f * (sz * 3);
                int fy = gy + (f % 2) * (sz / 2);
                drawSilFish(g2, fx, fy, sz, silColor);
            }
        }
    }

    private static void drawSilFish(Graphics2D g2, int cx, int cy, int sz, Color col) {
        g2.setColor(col);
        g2.fillOval(cx - sz/2, cy - sz/4, sz, sz/2);
        // Ekor
        g2.fillPolygon(
            new int[]{cx + sz/2, cx + sz/2 + sz/3, cx + sz/2},
            new int[]{cy - sz/5, cy, cy + sz/5}, 3
        );
        // Sirip atas kecil
        g2.fillPolygon(
            new int[]{cx - sz/6, cx, cx + sz/6},
            new int[]{cy - sz/4, cy - sz/4 - sz/3, cy - sz/4}, 3
        );
    }

    private static void drawDistantReef(Graphics2D g2, int w, int h) {
        // Siluet terumbu karang besar di kejauhan — biru gelap transparan
        Color reefColor = new Color(15, 60, 130, 130);
        g2.setColor(reefColor);
        // Gundukan besar kiri
        g2.fillOval(-60, (int)(h * 0.52), 220, 200);
        g2.fillOval(80,  (int)(h * 0.58), 180, 160);
        // Gundukan besar kanan
        g2.fillOval(w - 280, (int)(h * 0.54), 200, 190);
        g2.fillOval(w - 140, (int)(h * 0.50), 220, 210);
        // Tengah
        g2.fillOval(w/2 - 100, (int)(h * 0.62), 200, 130);

        // Batang karang siluet
        g2.setColor(new Color(12, 50, 115, 110));
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 12; i++) {
            int x = 30 + i * (w / 13);
            int baseY = (int)(h * 0.76);
            int ht = 40 + (i % 4) * 20;
            g2.drawArc(x - 10, baseY - ht, 22, ht, 90, 130);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private static void drawSeabed(Graphics2D g2, int w, int h) {
        // sandY = titik paling rendah gundukan pasir (garis dasar)
        int sandY = (int)(h * 0.80);

        // ── Layer 1: dasar pasir penuh ke bawah ───────────────────────────────
        g2.setPaint(new GradientPaint(
            0, sandY,  new Color(210, 175, 100),
            0, h,      new Color(155, 118, 62)
        ));
        g2.fillRect(0, sandY, w, h - sandY);

        // ── Layer 2: gundukan pasir bergelombang — 6 bukit bervariasi ─────────
        // Mirip gambar referensi: gundukan besar-kecil bergantian, warna cerah
        g2.setPaint(new GradientPaint(
            0, sandY - 80, new Color(255, 235, 160),
            0, sandY + 20, new Color(220, 185, 110)
        ));
        // Gundukan 1 (kiri, besar)
        g2.fillOval((int)(-0.05*w), sandY - 80, (int)(0.32*w), (int)(0.16*h));
        // Gundukan 2 (kiri-tengah, sedang)
        g2.fillOval((int)(0.18*w),  sandY - 60, (int)(0.28*w), (int)(0.13*h));
        // Gundukan 3 (tengah, besar)
        g2.fillOval((int)(0.35*w),  sandY - 85, (int)(0.34*w), (int)(0.17*h));
        // Gundukan 4 (tengah-kanan, sedang)
        g2.fillOval((int)(0.58*w),  sandY - 65, (int)(0.26*w), (int)(0.14*h));
        // Gundukan 5 (kanan, besar)
        g2.fillOval((int)(0.72*w),  sandY - 82, (int)(0.36*w), (int)(0.17*h));
        // Tutup celah antar gundukan
        g2.fillRect(0, sandY - 15, w, h - sandY + 15);

        // ── Layer 3: highlight cerah di puncak gundukan ───────────────────────
        g2.setPaint(new GradientPaint(
            0, sandY - 88, new Color(255, 248, 210, 200),
            0, sandY - 40, new Color(255, 248, 210, 0)
        ));
        g2.fillOval((int)(-0.05*w), sandY - 80, (int)(0.32*w), (int)(0.09*h));
        g2.fillOval((int)(0.18*w),  sandY - 60, (int)(0.28*w), (int)(0.07*h));
        g2.fillOval((int)(0.35*w),  sandY - 85, (int)(0.34*w), (int)(0.10*h));
        g2.fillOval((int)(0.58*w),  sandY - 65, (int)(0.26*w), (int)(0.08*h));
        g2.fillOval((int)(0.72*w),  sandY - 82, (int)(0.36*w), (int)(0.10*h));

        // ── Riak pasir halus ──────────────────────────────────────────────────
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 5; i++) {
            int ry = sandY + 10 + i * 16;
            if (ry > h - 5) break;
            int alpha = 38 - i * 6;
            g2.setColor(new Color(160, 120, 55, alpha));
            g2.drawArc(-10,      ry,     w/3 + 20, 12, 0, 180);
            g2.drawArc(w/3,      ry + 4, w/3 + 20, 10, 0, 180);
            g2.drawArc(w*2/3,    ry + 2, w/3 + 20, 12, 0, 180);
            // Highlight riak
            g2.setColor(new Color(255, 235, 170, alpha - 5));
            g2.drawArc(-8,       ry - 1, w/3 + 20, 12, 0, 180);
            g2.drawArc(w/3 + 2,  ry + 3, w/3 + 20, 10, 0, 180);
        }
        g2.setStroke(new BasicStroke(1f));

        // ── Batu-batu kecil di pasir ──────────────────────────────────────────
        drawSandRock(g2, (int)(w*0.02), sandY - 8,  58, 34, new Color(155, 142, 115));
        drawSandRock(g2, (int)(w*0.06), sandY - 2,  36, 22, new Color(168, 155, 128));
        drawSandRock(g2, (int)(w*0.88), sandY - 10, 64, 38, new Color(150, 138, 112));
        drawSandRock(g2, (int)(w*0.93), sandY - 3,  40, 24, new Color(162, 150, 124));
        drawSandRock(g2, (int)(w*0.48), sandY - 3,  28, 16, new Color(158, 146, 120));
    }

    /** Batu pasir dengan highlight */
    private static void drawSandRock(Graphics2D g2, int x, int y, int w, int h, Color col) {
        // Shadow
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillOval(x + 4, y + 6, w, h / 2);
        // Badan
        g2.setColor(col);
        g2.fillOval(x, y, w, h);
        // Highlight
        g2.setColor(new Color(255, 255, 255, 45));
        g2.fillOval(x + w/5, y + h/5, w/3, h/3);
    }

    private static void drawForegroundDecor(Graphics2D g2, int w, int h) {
        // sandY sama dengan drawSeabed
        int sy = (int)(h * 0.80);

        // ── 10 slot merata sepanjang layar, setiap dekorasi di tengah slot ────

        // SLOT 0 (x≈0.05w): Rumput laut hijau kiri
        drawNeatSeaweed(g2, (int)(w*0.04), sy, 90,  new Color(40, 130, 30), -1, 4f);
        drawNeatSeaweed(g2, (int)(w*0.07), sy, 110, new Color(60, 165, 45), 1,  5f);

        // SLOT 1 (x≈0.15w): Karang bercabang merah
        drawNeatBranchCoral(g2, (int)(w*0.15), sy, new Color(220, 55, 85), 5);

        // SLOT 2 (x≈0.24w): Karang pink fluffy
        drawNeatFluffyCoral(g2, (int)(w*0.24), sy, new Color(255, 145, 185), 48);

        // SLOT 3 (x≈0.34w): Karang tabung oranye
        drawNeatTubeCoral(g2, (int)(w*0.33), sy, new Color(210, 95, 30));

        // SLOT 4 (x≈0.43w): Bintang laut + kerang
        drawNeatStarfish(g2, (int)(w*0.42), sy + 2, new Color(255, 160, 40), 20);
        drawNeatShell(g2, (int)(w*0.46), sy + 5, new Color(240, 225, 205));

        // SLOT 5 (x≈0.52w): Anemon biru tengah
        drawNeatAnemone(g2, (int)(w*0.52), sy, new Color(90, 170, 235), 24);

        // SLOT 6 (x≈0.61w): Karang pink fluffy kanan-tengah
        drawNeatFluffyCoral(g2, (int)(w*0.61), sy, new Color(200, 130, 235), 44);

        // SLOT 7 (x≈0.70w): Karang kipas ungu
        drawNeatFanCoral(g2, (int)(w*0.70), sy, new Color(175, 105, 225), 52, 48);

        // SLOT 8 (x≈0.80w): Karang tabung oranye kanan
        drawNeatTubeCoral(g2, (int)(w*0.79), sy, new Color(195, 85, 28));

        // SLOT 9 (x≈0.90w): Rumput laut hijau kanan
        drawNeatSeaweed(g2, (int)(w*0.88), sy, 95,  new Color(40, 130, 30), 1,  4f);
        drawNeatSeaweed(g2, (int)(w*0.92), sy, 112, new Color(60, 165, 45), -1, 5f);

        // ── Bintang laut kecil di pasir (tidak overlap dekorasi utama) ────────
        drawNeatStarfish(g2, (int)(w*0.10), sy + 3, new Color(255, 140, 35), 14);
        drawNeatStarfish(g2, (int)(w*0.85), sy + 4, new Color(255, 100, 130), 13);

        // ── Kerang spiral di pasir ─────────────────────────────────────────────
        drawNeatSpiralShell(g2, (int)(w*0.29), sy + 6, new Color(235, 220, 200));
        drawNeatSpiralShell(g2, (int)(w*0.74), sy + 5, new Color(215, 230, 250));
    }

    private static void drawRock(Graphics2D g2, int x, int y, int w, int h, Color color) {
        g2.setColor(color);
        g2.fillOval(x, y, w, h);
        g2.setColor(new Color(255, 255, 255, 35));
        g2.fillOval(x + 10, y + 8, w / 3, h / 3);
    }

    private static void drawTinyBubbles(Graphics2D g2, int w, int h) {
        int[][] bubbles = {
            {(int)(w*0.06), (int)(h*0.42), 5},
            {(int)(w*0.09), (int)(h*0.55), 8},
            {(int)(w*0.94), (int)(h*0.38), 6},
            {(int)(w*0.91), (int)(h*0.52), 9},
            {(int)(w*0.50), (int)(h*0.65), 4},
            {(int)(w*0.30), (int)(h*0.70), 5},
            {(int)(w*0.70), (int)(h*0.68), 6},
        };
        for (int[] b : bubbles) {
            g2.setColor(new Color(255, 255, 255, 55));
            g2.fillOval(b[0]-b[2], b[1]-b[2], b[2]*2, b[2]*2);
            g2.setColor(new Color(255, 255, 255, 130));
            g2.drawOval(b[0]-b[2], b[1]-b[2], b[2]*2, b[2]*2);
            g2.setColor(new Color(255, 255, 255, 160));
            g2.fillOval(b[0]-b[2]/3, b[1]-b[2]/2, b[2]/3, b[2]/4);
        }
    }

    // ── Neat Drawing Methods (sesuai referensi) ───────────────────────────

    private static void drawNeatSeaweed(Graphics2D g2, int x, int y,
                                         int height, Color col, int dir, float thick) {
        int segs = 7;
        int segH = height / segs;
        g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int px = x, py = y;
        for (int i = 0; i < segs; i++) {
            float t = i / (float) segs;
            double sway = Math.sin(i * 1.1 + dir * 0.8) * (5 + i * 1.8) * dir;
            int nx = x + (int) sway;
            int ny = y - (i + 1) * segH;
            int r  = Math.min(255, col.getRed()   + (int)(t * 35));
            int gv = Math.min(255, col.getGreen() + (int)(t * 45));
            int b  = Math.min(255, col.getBlue()  + (int)(t * 18));
            g2.setColor(new Color(r, gv, b, 215 - (int)(t * 30)));
            g2.drawLine(px, py, nx, ny);
            if (i % 2 == 1 && i < segs - 1) {
                int ld = (i % 4 == 1) ? 1 : -1;
                int lw = (int)(thick * 3.2f);
                int lh = (int)(thick * 1.6f);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(Math.min(255,r+20), Math.min(255,gv+25), b, 200));
                java.awt.geom.AffineTransform old = g2.getTransform();
                double la = Math.atan2(ny - py, nx - px) + Math.PI / 2.3 * ld;
                g2.rotate(la, nx, ny);
                g2.fillOval(nx - lw/2, ny - lh/2, lw, lh);
                g2.setTransform(old);
                g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            px = nx; py = ny;
        }
        g2.setColor(new Color(Math.min(255,col.getRed()+50),
                              Math.min(255,col.getGreen()+60),
                              Math.min(255,col.getBlue()+25), 175));
        g2.fillOval(px - 3, py - 5, 7, 9);
        g2.setStroke(new BasicStroke(1f));
    }

    private static void drawNeatBranchCoral(Graphics2D g2, int x, int y,
                                             Color col, int branches) {
        g2.setColor(col.darker());
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x, y, x, y - 45);
        for (int i = 0; i < branches; i++) {
            double angle = -Math.PI/2 + (i - branches/2.0) * 0.55;
            int len = 28 + (branches - Math.abs(i - branches/2)) * 10;
            int ex = x + (int)(Math.cos(angle) * len);
            int ey = y - 45 + (int)(Math.sin(angle) * len);
            g2.setColor(col.darker());
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x, y - 45, ex, ey);
            double sa = angle + (i % 2 == 0 ? 0.45 : -0.45);
            int sx = ex + (int)(Math.cos(sa) * 16);
            int sy2 = ey + (int)(Math.sin(sa) * 16);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(col);
            g2.drawLine(ex, ey, sx, sy2);
            g2.setColor(col.brighter());
            g2.fillOval(sx - 5, sy2 - 5, 10, 10);
            g2.fillOval(ex - 5, ey - 5, 10, 10);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private static void drawNeatFluffyCoral(Graphics2D g2, int cx, int y,
                                             Color col, int r) {
        g2.setColor(col.darker());
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx, y, cx, y - r/2);
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16;
            int ox = (int)(Math.cos(angle) * r * 0.52);
            int oy = (int)(Math.sin(angle) * r * 0.45);
            int br = 11 + (i % 3) * 4;
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 210));
            g2.fillOval(cx + ox - br, y - r - oy - br, br*2, br*2);
        }
        g2.setColor(col.brighter());
        g2.fillOval(cx - r/2, y - (int)(r*1.35), r, r);
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 80));
        g2.fillOval(cx - r + 5, y - 8, (r*2) - 10, 14);
    }

    private static void drawNeatAnemone(Graphics2D g2, int cx, int y, Color col, int r) {
        g2.setColor(col.darker());
        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx, y, cx, y - r);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = -3; i <= 3; i++) {
            double angle = -Math.PI/2 + i * 0.28;
            int ex = cx + (int)(Math.cos(angle) * r);
            int ey = y - r + (int)(Math.sin(angle) * r * 0.5);
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 210));
            g2.drawLine(cx, y - r, ex, ey);
            g2.setColor(col.brighter());
            g2.fillOval(ex - 5, ey - 5, 10, 10);
        }
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(col.darker());
        g2.fillOval(cx - 9, y - 5, 18, 10);
    }

    private static void drawNeatFanCoral(Graphics2D g2, int cx, int y,
                                          Color col, int fw, int fh) {
        g2.setColor(col.darker());
        g2.setStroke(new BasicStroke(4f));
        g2.drawLine(cx, y, cx, y - fh/3);
        for (int i = -5; i <= 5; i++) {
            double angle = -Math.PI/2 + i * 0.16;
            int ex = cx + (int)(Math.cos(angle) * fw/2);
            int ey = y - fh/3 + (int)(Math.sin(angle) * fh * 0.7);
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 175));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx, y - fh/3, ex, ey);
        }
        for (int lv = 1; lv <= 3; lv++) {
            float frac = lv / 4f;
            int lx1 = cx - (int)(fw/2 * frac);
            int lx2 = cx + (int)(fw/2 * frac);
            int ly  = y - fh/3 - (int)(fh * 0.6f * frac);
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 110));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawLine(lx1, ly, lx2, ly);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private static void drawNeatTubeCoral(Graphics2D g2, int x, int y, Color col) {
        int[][] tubes = {{x, y, 20, 55}, {x+26, y, 16, 44}, {x+48, y, 14, 36}};
        for (int[] t : tubes) {
            int tx = t[0], ty = t[1], tw = t[2], th = t[3];
            g2.setPaint(new GradientPaint(tx, ty, col.brighter(), tx+tw, ty, col.darker()));
            g2.fillRoundRect(tx - tw/2, ty - th, tw, th, tw/2, tw/2);
            g2.setPaint(null);
            g2.setColor(col.darker());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(tx - tw/2, ty - th, tw, th, tw/2, tw/2);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(50, 25, 8, 190));
            g2.fillOval(tx - tw/2 + 1, ty - th, tw - 2, tw/2);
            g2.setColor(new Color(255, 255, 255, 55));
            g2.drawLine(tx - tw/2 + 3, ty - th + 6, tx - tw/2 + 3, ty - 6);
        }
    }

    private static void drawNeatStarfish(Graphics2D g2, int cx, int cy,
                                          Color col, int size) {
        int[] xp = new int[10], yp = new int[10];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI/2 + i * Math.PI/5;
            int r = (i % 2 == 0) ? size : size/2;
            xp[i] = cx + (int)(Math.cos(angle) * r);
            yp[i] = cy + (int)(Math.sin(angle) * r);
        }
        g2.setColor(new Color(0,0,0,28));
        g2.fillPolygon(xp, yp, 10);
        g2.setColor(col);
        int[] sxp = new int[10], syp = new int[10];
        for (int i = 0; i < 10; i++) { sxp[i] = xp[i]-1; syp[i] = yp[i]-1; }
        g2.fillPolygon(sxp, syp, 10);
        g2.setColor(col.darker());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(sxp, syp, 10);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(255,255,255,70));
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5 - Math.PI/2;
            g2.fillOval(cx+(int)(Math.cos(a)*size*0.55)-2,
                        cy+(int)(Math.sin(a)*size*0.55)-2, 5, 5);
        }
    }

    private static void drawNeatSpiralShell(Graphics2D g2, int cx, int cy, Color col) {
        g2.setColor(new Color(0,0,0,22));
        g2.fillOval(cx - 14, cy + 4, 28, 10);
        g2.setColor(col);
        g2.fillOval(cx - 13, cy - 10, 26, 20);
        g2.setColor(col.darker());
        g2.setStroke(new BasicStroke(1.3f));
        g2.drawOval(cx - 13, cy - 10, 26, 20);
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawArc(cx - 9, cy - 7, 18, 14, 15, 310);
        g2.drawArc(cx - 5, cy - 4, 10, 8, 20, 280);
        g2.drawArc(cx - 2, cy - 2, 5, 4, 30, 250);
        g2.setColor(new Color(255,255,255,90));
        g2.fillOval(cx - 7, cy - 8, 7, 5);
        g2.setStroke(new BasicStroke(1f));
    }

    private static void drawNeatShell(Graphics2D g2, int cx, int cy, Color col) {
        g2.setColor(new Color(0,0,0,22));
        g2.fillOval(cx - 16, cy + 2, 32, 10);
        g2.setColor(col);
        g2.fillArc(cx - 16, cy - 12, 32, 22, 0, 180);
        g2.setColor(col.darker());
        g2.setStroke(new BasicStroke(1.3f));
        g2.drawArc(cx - 16, cy - 12, 32, 22, 0, 180);
        for (int i = -3; i <= 3; i++) {
            double angle = Math.PI/2 + i * 0.38;
            int ex = cx + (int)(Math.cos(angle) * 14);
            int ey = cy - 12 + 11 + (int)(Math.sin(angle) * 11);
            g2.setColor(col.darker());
            g2.setStroke(new BasicStroke(0.9f));
            g2.drawLine(cx, cy - 1, ex, ey);
        }
        g2.setColor(new Color(255,255,255,80));
        g2.fillOval(cx - 8, cy - 10, 8, 5);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── Bubbles Animasi ───────────────────────────────────────────────────
    public static void drawBubbles(Graphics2D g2, float[] bx, float[] by, int[] br) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < bx.length; i++) {
            int r = br[i];

            g2.setColor(new Color(180, 230, 255, 25));
            g2.fillOval((int) bx[i] - r, (int) by[i] - r, r * 2, r * 2);

            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawOval((int) bx[i] - r, (int) by[i] - r, r * 2, r * 2);

            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillOval((int) bx[i] - r / 3, (int) by[i] - r / 2, r / 3, r / 4);
        }
    }

    // ── Panel Bubble ──────────────────────────────────────────────────────
    public static JPanel makeGlassPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(123, 73, 38, 45));
                g2.fillRoundRect(4, 6, getWidth() - 8, getHeight() - 8, 24, 24);

                g2.setColor(PANEL_GLASS);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 4, 24, 24);

                g2.setColor(new Color(255, 255, 255, 90));
                g2.fillRoundRect(2, 2, getWidth() - 6, Math.max(20, getHeight() / 3), 22, 22);

                g2.setColor(PANEL_BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 4, getHeight() - 6, 24, 24);

                g2.dispose();
            }
        };

        panel.setOpaque(false);
        return panel;
    }

    public static JPanel makeDarkGlassPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(0, 42, 84, 90));
                g2.fillRoundRect(4, 7, getWidth() - 8, getHeight() - 10, 28, 28);

                GradientPaint body = new GradientPaint(
                    0, 0, new Color(218, 246, 255, 220),
                    0, getHeight(), new Color(145, 211, 236, 205)
                );
                g2.setPaint(body);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 5, 28, 28);

                g2.setColor(new Color(255, 255, 255, 110));
                g2.fillRoundRect(3, 3, getWidth() - 8, Math.max(24, getHeight() / 3), 24, 24);

                g2.setColor(new Color(255, 255, 255, 210));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(1, 1, getWidth() - 4, getHeight() - 7, 28, 28);

                g2.dispose();
            }
        };

        panel.setOpaque(false);
        return panel;
    }

    // ── Teks ──────────────────────────────────────────────────────────────
    public static void drawShadowText(Graphics2D g2, String text, Font font, Color color, int x, int y) {
        g2.setFont(font);
        g2.setColor(TEXT_SHADOW);
        g2.drawString(text, x + 2, y + 2);

        g2.setColor(color);
        g2.drawString(text, x, y);
    }

    public static void drawCenterText(Graphics2D g2, String text, Font font, Color color, int centerX, int y) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;
        drawShadowText(g2, text, font, color, x, y);
    }

    // ── Bintang & Hati ────────────────────────────────────────────────────
    public static void drawStars(Graphics2D g2, int stars, int cx, int cy, int size) {
        int[][] positions = {
            {cx - size * 2, cy},
            {cx, cy - size / 2},
            {cx + size * 2, cy}
        };
        for (int i = 0; i < 3; i++) {
            drawStar(g2, positions[i][0], positions[i][1], size, i < stars ? STAR_ON : STAR_OFF);
        }
    }

    private static void drawStar(Graphics2D g2, int cx, int cy, int size, Color color) {
        int[] xp = new int[10], yp = new int[10];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            int r = (i % 2 == 0) ? size : size / 2;
            xp[i] = cx + (int)(r * Math.cos(angle));
            yp[i] = cy - (int)(r * Math.sin(angle));
        }
        g2.setColor(color);
        g2.fillPolygon(xp, yp, 10);
        g2.setColor(color.darker());
        g2.drawPolygon(xp, yp, 10);
    }

    public static void drawHeart(Graphics2D g2, int x, int y, int size, Color color) {
        g2.setColor(color);
        g2.fillArc(x, y, size / 2, size / 2, 0, 180);
        g2.fillArc(x + size / 2, y, size / 2, size / 2, 0, 180);
        int[] hx = {x, x + size / 2, x + size};
        int[] hy = {y + size / 4, y + size, y + size / 4};
        g2.fillPolygon(hx, hy, 3);
    }
}