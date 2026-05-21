
package com.mycompany.bullethmath;
/**
 * ============================================================
 *  SeaAnimal — Base class untuk semua hewan laut dekoratif.
 *  Subclass: Fish, Jellyfish, Turtle
 * ============================================================
 */
import java.awt.*;
import java.util.Random;

// ─────────────────────────────────────────────────────────────
//  Abstract base class
// ─────────────────────────────────────────────────────────────
abstract class SeaAnimal {

    protected float x, y;
    protected int   panelW, panelH;
    protected Random rng = new Random();

    public SeaAnimal(int panelW, int panelH) {
        this.panelW = panelW;
        this.panelH = panelH;
    }

    /** Update posisi / state animasi setiap frame. */
    public abstract void update();

    /** Gambar hewan ke Graphics2D. */
    public abstract void draw(Graphics2D g);
}

// ─────────────────────────────────────────────────────────────
//  Fish — Ikan yang berenang kiri-kanan
// ─────────────────────────────────────────────────────────────
class Fish extends SeaAnimal {

    private float   speed;
    private boolean facingRight;
    private Color   bodyColor;
    private Color   finColor;
    private int     size;        // Lebar badan
    private float   wobble;      // Gerakan naik-turun halus
    private float   wobbleSpeed;
    private float   wobbleAmp;
    private float   baseY;

    public Fish(int panelW, int panelH, Random sharedRng) {
        super(panelW, panelH);
        this.rng = sharedRng;
        respawn(true);
    }

    /** Posisikan ulang ikan di luar layar sisi berlawanan. */
    private void respawn(boolean initial) {
        facingRight = rng.nextBoolean();
        speed       = 0.8f + rng.nextFloat() * 1.5f;
        size        = 28 + rng.nextInt(30);
        baseY       = 70 + rng.nextFloat() * (panelH - 200);
        wobbleSpeed = 0.04f + rng.nextFloat() * 0.03f;
        wobbleAmp   = 5f + rng.nextFloat() * 8f;
        wobble      = rng.nextFloat() * 6.28f;

        // Palet warna tropis
        Color[] palette = {
            new Color(255, 120, 40),   // Oranye — Nemo
            new Color(80,  200, 255),  // Biru terang
            new Color(255, 220, 40),   // Kuning
            new Color(255, 80,  160),  // Pink
            new Color(50,  220, 120),  // Hijau toska
            new Color(180, 80,  255),  // Ungu
        };
        bodyColor = palette[rng.nextInt(palette.length)];
        finColor  = bodyColor.darker();

        if (initial) {
            x = rng.nextFloat() * panelW;
        } else {
            x = facingRight ? -size * 2 : panelW + size;
        }
        y = baseY;
    }

    @Override
    public void update() {
        x += facingRight ? speed : -speed;
        wobble += wobbleSpeed;
        y = baseY + (float) Math.sin(wobble) * wobbleAmp;

        // Keluar layar → spawn ulang dari sisi berlawanan
        if (facingRight && x > panelW + size * 2) respawn(false);
        if (!facingRight && x < -size * 2)        respawn(false);
    }

    @Override
    public void draw(Graphics2D g) {
        int bw = size;           // lebar badan
        int bh = (int)(size * 0.55f);
        int ix = (int) x;
        int iy = (int) y;

        Graphics2D g2 = (Graphics2D) g.create();
        // Balik gambar jika ikan menghadap kiri
        if (!facingRight) {
            g2.translate(ix + bw, iy);
            g2.scale(-1, 1);
            ix = 0; iy = 0;
        } else {
            g2.translate(ix, iy);
            ix = 0; iy = 0;
        }

        // Ekor (segitiga)
        Polygon tail = new Polygon();
        tail.addPoint(-bh, 0);
        tail.addPoint(-bh, bh);
        tail.addPoint(bh / 3, bh / 2);
        g2.setColor(finColor);
        g2.fillPolygon(tail);

        // Badan (oval)
        g2.setColor(bodyColor);
        g2.fillOval(0, 0, bw, bh);

        // Corak garis putih di badan (Nemo style jika oranye)
        if (bodyColor.getRed() > 200 && bodyColor.getGreen() < 150) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(bw / 3, 0, bw / 3, bh);
            g2.drawLine(bw * 2 / 3, 2, bw * 2 / 3, bh - 2);
            g2.setStroke(new BasicStroke(1f));
        }

        // Sirip punggung
        Polygon dorsalFin = new Polygon();
        dorsalFin.addPoint(bw / 3, 0);
        dorsalFin.addPoint(bw / 2, -bh / 2);
        dorsalFin.addPoint(bw * 2 / 3, 0);
        g2.setColor(finColor);
        g2.fillPolygon(dorsalFin);

        // Mata
        int eyeX = bw - bw / 5;
        int eyeY = bh / 4;
        g2.setColor(Color.WHITE);
        g2.fillOval(eyeX - 4, eyeY - 4, 9, 9);
        g2.setColor(Color.BLACK);
        g2.fillOval(eyeX - 2, eyeY - 2, 5, 5);
        // Kilap mata
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillOval(eyeX, eyeY - 2, 2, 2);

        g2.dispose();
    }
}

// ─────────────────────────────────────────────────────────────
//  Jellyfish — Ubur-ubur yang naik turun
// ─────────────────────────────────────────────────────────────
class Jellyfish extends SeaAnimal {

    private Color   bodyColor;
    private float   phase;       // Fase animasi naik-turun
    private float   phaseSpeed;
    private float   baseY;
    private int     size;
    private float   pulsePhase;  // Animasi berdenyut (seperti bernapas)
    private int[]   tentacleOffsets;  // Variasi posisi tentakel

    public Jellyfish(int panelW, int panelH, Random sharedRng) {
        super(panelW, panelH);
        this.rng = sharedRng;

        // Posisi acak dalam layar
        x = 40 + rng.nextFloat() * (panelW - 80);
        baseY = 80 + rng.nextFloat() * (panelH - 300);
        phase = rng.nextFloat() * 6.28f;
        phaseSpeed = 0.012f + rng.nextFloat() * 0.008f;
        pulsePhase = rng.nextFloat() * 6.28f;
        size = 22 + rng.nextInt(22);

        Color[] palette = {
            new Color(255, 120, 200),  // Pink
            new Color(180, 80,  255),  // Ungu
            new Color(80,  200, 255),  // Biru muda
            new Color(120, 255, 200),  // Mint
        };
        bodyColor = palette[rng.nextInt(palette.length)];

        // Tentakel: 5 tentakel dengan panjang acak
        tentacleOffsets = new int[5];
        for (int i = 0; i < tentacleOffsets.length; i++)
            tentacleOffsets[i] = rng.nextInt(15) - 7;
    }

    @Override
    public void update() {
        phase      += phaseSpeed;
        pulsePhase += 0.08f;
        y = baseY + (float) Math.sin(phase) * 28f;
    }

    @Override
    public void draw(Graphics2D g) {
        int ix = (int) x;
        int iy = (int) y;

        // Denyutan: ubah tinggi badan secara berkala
        float pulse = 1f + 0.12f * (float) Math.sin(pulsePhase);
        int bw = size * 2;
        int bh = (int)(size * pulse);

        // Bayangan transparan di bawah
        g.setColor(new Color(0, 0, 80, 30));
        g.fillOval(ix - bw / 2 + 5, iy + bh + 10, bw, 8);

        // Tentakel (digambar dulu, di bawah badan)
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 5; i++) {
            int tx = ix - bw / 2 + (i + 1) * (bw / 6);
            int tLen = 20 + tentacleOffsets[i] + (int)(Math.sin(pulsePhase + i) * 8);
            int tsway = (int)(Math.sin(phase + i * 1.2f) * 5);
            g.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 120));
            g.drawLine(tx, iy + bh, tx + tsway, iy + bh + tLen);
        }
        g.setStroke(new BasicStroke(1f));

        // Badan utama — kubah ubur-ubur (setengah lingkaran)
        Color fill = new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 150);
        g.setColor(fill);
        g.fillArc(ix - bw / 2, iy, bw, bh * 2, 0, 180);

        // Kilauan di kubah
        g.setColor(new Color(255, 255, 255, 80));
        g.fillArc(ix - bw / 4, iy + 4, bw / 2, bh - 4, 20, 140);

        // Lingkaran dalam ubur-ubur (organ)
        g.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 90));
        g.fillOval(ix - bw / 4, iy + bh / 3, bw / 2, bh / 3);
    }
}

// ─────────────────────────────────────────────────────────────
//  Turtle — Penyu yang berenang pelan ke kiri/kanan
// ─────────────────────────────────────────────────────────────
class Turtle extends SeaAnimal {

    private float   speed;
    private boolean facingRight;
    private float   wobble;
    private float   baseY;
    private float   flipperPhase;  // Animasi sirip mengepak

    public Turtle(int panelW, int panelH, Random sharedRng) {
        super(panelW, panelH);
        this.rng = sharedRng;
        respawn(true);
    }

    private void respawn(boolean initial) {
        facingRight = rng.nextBoolean();
        speed = 0.4f + rng.nextFloat() * 0.6f;
        baseY = 120 + rng.nextFloat() * (panelH - 300);
        wobble = 0;
        flipperPhase = 0;

        if (initial) x = rng.nextFloat() * panelW;
        else         x = facingRight ? -100 : panelW + 100;
        y = baseY;
    }

    @Override
    public void update() {
        x += facingRight ? speed : -speed;
        wobble += 0.02f;
        y = baseY + (float) Math.sin(wobble) * 12f;
        flipperPhase += 0.12f;

        if (facingRight && x > panelW + 100) respawn(false);
        if (!facingRight && x < -100)        respawn(false);
    }

    @Override
    public void draw(Graphics2D g) {
        int ix = (int) x;
        int iy = (int) y;
        int sw = 50; // Lebar tempurung

        Graphics2D g2 = (Graphics2D) g.create();
        if (!facingRight) {
            g2.translate(ix + sw, iy);
            g2.scale(-1, 1);
            ix = 0; iy = 0;
        } else {
            g2.translate(ix, iy);
            ix = 0; iy = 0;
        }

        // Flipper depan
        float fp = (float) Math.sin(flipperPhase);
        g2.setColor(new Color(40, 130, 70));
        g2.fillOval(ix + sw - 8, iy + 8 + (int)(fp * 5), 20, 10);
        // Flipper belakang
        g2.fillOval(ix - 8, iy + 10 - (int)(fp * 5), 16, 9);

        // Tempurung — lingkaran hijau tua
        GradientPaint shellGrad = new GradientPaint(
            ix, iy, new Color(50, 160, 60),
            ix, iy + 35, new Color(20, 80, 30)
        );
        g2.setPaint(shellGrad);
        g2.fillOval(ix, iy, sw, 35);
        g2.setPaint(null);

        // Pola hexagonal di tempurung
        g2.setColor(new Color(30, 100, 40, 150));
        g2.setStroke(new BasicStroke(1.5f));
        // Baris atas
        g2.drawLine(ix + 12, iy + 5,  ix + 24, iy + 5);
        g2.drawLine(ix + 24, iy + 5,  ix + 30, iy + 14);
        g2.drawLine(ix + 12, iy + 5,  ix + 6,  iy + 14);
        g2.drawLine(ix + 6,  iy + 14, ix + 12, iy + 22);
        g2.drawLine(ix + 30, iy + 14, ix + 24, iy + 22);
        g2.drawLine(ix + 12, iy + 22, ix + 24, iy + 22);
        g2.setStroke(new BasicStroke(1f));

        // Kepala kecil
        g2.setColor(new Color(60, 160, 80));
        g2.fillOval(ix + sw - 4, iy + 10, 18, 14);
        // Mata
        g2.setColor(Color.WHITE);
        g2.fillOval(ix + sw + 6, iy + 12, 5, 5);
        g2.setColor(Color.BLACK);
        g2.fillOval(ix + sw + 7, iy + 13, 3, 3);

        g2.dispose();
    }
}
