package com.mycompany.bullethmath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;

/**
 * ================================================================
 *  GameplayForm.java  v3.0  — Tembak Jawaban Matematika
 * ================================================================
 *  Perbaikan v3.0:
 *   1. TIDAK ada warna berbeda untuk jawaban benar sebelum ditembak
 *      — semua makhluk tampil identik, baru berubah setelah kena peluru
 *   2. Peluru benar-benar bergerak dari cannon menuju target
 *      — ada animasi traveling bullet dengan ekor cahaya
 *   3. Cannon di tengah-bawah layar (W/2)
 *   4. Makhluk laut kawaii dua mata, lebih lucu & colorful
 *   5. Bubble label ekspresi bulat, warna seragam
 *   6. Efek ledakan, feedback, float text yang lebih halus
 * ================================================================
 */
public class GameplayForm extends JFrame {

    // ── Dimensi & FPS ────────────────────────────────────────────────────────
    private static final int FPS        = 60;
    private static final int TIMER_SECS = 10;   // ← 10 detik per soal (fixed)

    // W dan H diambil dari ukuran layar saat runtime
    private final int W;
    private final int H;
    private final int HUD_H      = 72;           // HUD sedikit lebih tinggi
    private final int GUNZONE_Y;
    private final int CANNON_X;
    private final int CANNON_Y;
    private final int CANNON_TIP;

    private static final int TOTAL_QUESTIONS = 10;  // diganti oleh getTotalQuestions(level)

    // ── State ─────────────────────────────────────────────────────────────────
    private enum Phase { COUNTDOWN, PLAYING, DONE }
    private Phase phase = Phase.COUNTDOWN;

    private final String         materi;
    private final int            level;
    private final int            userId;
    private final DatabaseManager db;

    private int score        = 0;
    private int lives        = 3;
    private int waveIndex    = 0;
    private int correctCount = 0;
    private int bulletNum    = 0;
    private int timerTicks   = 0;
    private int timerMax     = 1;

    // ── Combo & streak system ─────────────────────────────────────────────────
    private int   comboStreak    = 0;   // jawaban benar beruntun
    private int   maxCombo       = 0;   // combo tertinggi di level ini
    private float comboMulti     = 1f;  // multiplier skor (1x, 2x, 3x, 4x)
    private int   comboDisplayT  = 0;   // timer tampil teks combo
    private String comboText     = "";

    // ── Screen shake ──────────────────────────────────────────────────────────
    private int   shakeT         = 0;   // durasi shake (ticks)
    private float shakeX         = 0;
    private float shakeY         = 0;

    // ── Warning flash (timer hampir habis) ────────────────────────────────────
    private int   warnFlashT     = 0;
    private boolean panicMode    = false;   // 3 detik terakhir = panic!
    private float panicPulse     = 0f;      // animasi denyut panic

    // Countdown — lebih cepat
    private int countdownVal  = 3;
    private int countdownTick = 0;
    private static final int CD_DUR = 18;  // ~0.3 detik per angka (lebih cepat lagi)

    // ── Entitas ───────────────────────────────────────────────────────────────
    private final List<SeaCreature> creatures   = new ArrayList<>();
    private final List<BubbleFx>    splashFx    = new ArrayList<>();
    private final List<FloatLabel>  floatLabels = new ArrayList<>();
    private final List<BgAnimal>    bgAnimals   = new ArrayList<>();
    private Bullet activeBullet = null;  // hanya 1 peluru aktif
    private boolean waitingSpawn = false; // sedang menunggu spawn berikutnya

    // ── Efek visual cannon ────────────────────────────────────────────────────
    private float gunRecoil  = 0;
    private int   gunRecoilT = 0;
    private int   muzzleT    = 0;        // durasi muzzle flash

    // ── Feedback teks tengah ──────────────────────────────────────────────────
    private String feedbackText  = "";
    private Color  feedbackColor = Color.WHITE;
    private int    feedbackAlpha = 0;

    // ── Background bubbles (tidak lagi dipakai, dihapus) ─────────────────────

    private float coralAnim = 0;    // animasi karang & rumput laut

    // ── Sparkle particles ─────────────────────────────────────────────────────
    private final List<SparkleParticle> sparkles = new ArrayList<>();

    // ── Tombol kembali ke menu ────────────────────────────────────────────────
    private boolean menuBtnHover = false;
    private static final int MENU_BTN_X = 8;
    private static final int MENU_BTN_Y = 8;
    private static final int MENU_BTN_W = 110;
    private static final int MENU_BTN_H = 38;

    private final Random         rng       = new Random();
    private javax.swing.Timer    gameTimer;
    private JPanel               canvas;

    // =========================================================================
    //  KONSTRUKTOR
    // =========================================================================
    public GameplayForm(String materi, int level) {
        super("Math Adventure - " +
              LevelGenerator.getMateriName(materi) + " Level " + level);
        this.materi  = materi;
        this.level   = level;
        this.userId  = SessionManager.getUserId();
        this.db      = SessionManager.getDb();

        // ── Ukuran layar penuh ────────────────────────────────────────────
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                               .getDefaultScreenDevice()
                               .getDefaultConfiguration()
        );
        W = screen.width  - insets.left - insets.right;
        H = screen.height - insets.top  - insets.bottom;

        // ── Zona layout dihitung dari ukuran layar ────────────────────────
        GUNZONE_Y  = (int)(H * 0.85);   // pasir di 15% bawah layar — lebih ke bawah
        CANNON_X   = W / 2;
        CANNON_Y   = H - 110;
        CANNON_TIP = CANNON_Y - 88;

        // Timer selalu 10 detik (TIMER_SECS), abaikan LevelGenerator
        this.timerMax = TIMER_SECS * FPS;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(W, H);
        setLocationRelativeTo(null);

        initBackground();
        buildCanvas();

        gameTimer = new javax.swing.Timer(1000 / FPS, e -> tick());
        gameTimer.start();
        setVisible(true);
        // Tidak pakai fadeIn di gameplay — form terlalu berat, langsung tampil
    }

    // ── Init background elements ──────────────────────────────────────────────
    private void initBackground() {
        int fishLimit = GUNZONE_Y - 40;  // batas bawah hewan air

        // ── Hewan di AIR (di atas garis pasir) — sedikit agar tidak ganggu soal
        for (int i = 0; i < 6; i++) {
            BgFish f = new BgFish(W, H, rng);
            f.maxY = fishLimit;
            f.spawn(true);
            bgAnimals.add(f);
        }
        for (int i = 0; i < 2; i++) {
            BgJellyfish jf = new BgJellyfish(W, H, rng);
            jf.maxY = fishLimit;
            if (jf.y > fishLimit) jf.y = 80 + rng.nextFloat() * Math.max(10, fishLimit - 80);
            bgAnimals.add(jf);
        }
        BgTurtle t = new BgTurtle(W, H, rng);
        t.maxY = fishLimit;
        if (t.y > fishLimit) t.y = 100 + rng.nextFloat() * Math.max(10, fishLimit - 100);
        bgAnimals.add(t);
        // Pari manta — 1 ekor, berenang anggun di zona air
        BgManta manta = new BgManta(W, H, rng);
        manta.maxY = fishLimit;
        bgAnimals.add(manta);

        // ── Hewan di PASIR (GUNZONE_Y ke H) — digambar setelah pasir ──────────
        // Kepiting berjalan — 3 ekor tersebar
        for (int i = 0; i < 3; i++) {
            BgCrabWalk c = new BgCrabWalk(W, H, rng);
            c.maxY = GUNZONE_Y;
            c.x    = (W / 4f) * (i + 0.5f) + rng.nextFloat() * (W / 8f);
            c.y    = GUNZONE_Y + 10 + rng.nextFloat() * ((H - GUNZONE_Y) * 0.4f);
            bgAnimals.add(c);
        }

        int sandH = H - GUNZONE_Y;

        // ── Rumput laut bergerak dalam CLUSTER — tidak acak ───────────────────
        spawnSeaweedCluster(8,  (int)(W*0.01), (int)(W*0.08), GUNZONE_Y, sandH);
        spawnSeaweedCluster(6,  (int)(W*0.10), (int)(W*0.18), GUNZONE_Y, sandH);
        spawnSeaweedCluster(6,  (int)(W*0.82), (int)(W*0.90), GUNZONE_Y, sandH);
        spawnSeaweedCluster(8,  (int)(W*0.92), (int)(W*0.99), GUNZONE_Y, sandH);
        spawnSeaweedCluster(4,  (int)(W*0.28), (int)(W*0.34), GUNZONE_Y, sandH / 3);
        spawnSeaweedCluster(4,  (int)(W*0.66), (int)(W*0.72), GUNZONE_Y, sandH / 3);

        // ── Biota kecil bergerak — bintang laut, kerang, bulu babi, kepiting ─
        // Tersebar merata di seluruh area pasir
        int[] pool = {
            0,0,0, 1,1,1, 2,2,2, 3,3,3, 4,4,4,
            0,1,2, 3,4,0, 1,2,3, 4,0,1, 2,3,4
        };
        for (int tp : pool) {
            bgAnimals.add(new BgSandCreature(W, H, rng, GUNZONE_Y, tp));
        }
    }

    private void buildCanvas() {
        canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                drawAll(g2);
                g2.dispose();
            }
        };
        canvas.setBackground(new Color(3, 22, 58));
        canvas.setFocusable(true);
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                // Cek klik tombol kembali ke menu
                if (e.getX() >= MENU_BTN_X && e.getX() <= MENU_BTN_X + MENU_BTN_W
                 && e.getY() >= MENU_BTN_Y && e.getY() <= MENU_BTN_Y + MENU_BTN_H) {
                    onMenuButtonClick();
                    return;
                }
                onCanvasClick(e.getX(), e.getY());
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                boolean hover = e.getX() >= MENU_BTN_X && e.getX() <= MENU_BTN_X + MENU_BTN_W
                             && e.getY() >= MENU_BTN_Y && e.getY() <= MENU_BTN_Y + MENU_BTN_H;
                if (hover != menuBtnHover) { menuBtnHover = hover; canvas.repaint(); }
            }
        });
        // ESC juga kembali ke menu
        canvas.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) onMenuButtonClick();
            }
        });
        setContentPane(canvas);
    }

    // =========================================================================
    //  GAME LOOP
    // =========================================================================
    private void tick() {
        coralAnim += 0.08f;

        for (BgAnimal a : bgAnimals) a.update();

        // Fase
        if (phase == Phase.COUNTDOWN) tickCountdown();
        else if (phase == Phase.PLAYING) tickPlaying();

        // Efek
        splashFx.removeIf(b -> !b.alive);
        for (BubbleFx b : splashFx) b.update();
        floatLabels.removeIf(fl -> !fl.alive);
        for (FloatLabel fl : floatLabels) fl.update();
        sparkles.removeIf(s -> !s.alive);
        for (SparkleParticle s : sparkles) s.update();

        if (feedbackAlpha > 0) feedbackAlpha -= 6;
        if (gunRecoilT > 0) { gunRecoil = 10f * (gunRecoilT / 10f); gunRecoilT--; }
        else gunRecoil = 0;
        if (muzzleT > 0) muzzleT--;

        // ── Screen shake ──────────────────────────────────────────────────────
        if (shakeT > 0) {
            shakeX = (rng.nextFloat() - 0.5f) * shakeT * 1.5f;
            shakeY = (rng.nextFloat() - 0.5f) * shakeT * 1.5f;
            shakeT--;
        } else { shakeX = 0; shakeY = 0; }

        // ── Combo display timer ───────────────────────────────────────────────
        if (comboDisplayT > 0) comboDisplayT--;

        // ── Panic mode: 3 detik terakhir ─────────────────────────────────────
        if (phase == Phase.PLAYING && timerMax > 0) {
            int panicThreshold = 3 * FPS;
            panicMode = (timerTicks > 0 && timerTicks <= panicThreshold);
            if (panicMode) {
                panicPulse += 0.18f;
                warnFlashT = (warnFlashT + 1) % 12;  // lebih cepat berkedip
            } else {
                panicPulse = 0f;
                warnFlashT = 0;
            }
        }

        canvas.repaint();
    }

    private void tickCountdown() {
        countdownTick++;
        if (countdownTick >= CD_DUR) {
            countdownTick = 0;
            countdownVal--;
            if (countdownVal >= 0) {
                SoundManager.getInstance().play("countdown");
            }
            if (countdownVal < 0) {
                phase = Phase.PLAYING;
                spawnWave();
            }
        }
    }

    private void tickPlaying() {
        // Update peluru aktif
        if (activeBullet != null) {
            activeBullet.update();
            if (activeBullet.hasReachedTarget()) {
                SeaCreature hit = activeBullet.target;
                activeBullet = null;
                processBulletHit(hit);
                return;
            }
        }

        // Update & cek musuh
        List<SeaCreature> toRemove = new ArrayList<>();
        boolean targetEscaped = false;

        for (SeaCreature cr : creatures) {
            cr.update();
            if (cr.y > GUNZONE_Y - 10 && cr.alive && !cr.exploding) {
                cr.alive = false;
                toRemove.add(cr);
                if (cr.isTarget) {
                    targetEscaped = true;
                }
            }
            if (cr.exploding && cr.expTimer > 32) {
                cr.alive = false;
                toRemove.add(cr);
            }
        }
        creatures.removeAll(toRemove);

        // ── Bug fix: jika musuh target lolos, kurangi nyawa lalu lanjut wave ──
        if (targetEscaped) {
            lives--;
            showFeedback("Lolos! -Nyawa", new Color(255, 90, 90));
            SoundManager.getInstance().play("wrong");
            spawnSplash(rng.nextInt(W - 100) + 50, GUNZONE_Y - 20,
                        new Color(255, 80, 80));
            // Hapus semua sisa creature wave ini
            creatures.forEach(c -> c.alive = false);
            creatures.clear();
            activeBullet = null;
            if (lives <= 0) { triggerFail(); return; }
            scheduleNextWave(150);
            return;
        }

        // Timer per wave (hanya saat tidak ada peluru di udara)
        if (!creatures.isEmpty() && activeBullet == null && !waitingSpawn) {
            timerTicks--;
            if (timerTicks <= 0) {
                lives--;
                showFeedback("Waktu Habis! -Nyawa", new Color(255, 140, 40));
                SoundManager.getInstance().play("wrong");
                creatures.forEach(c -> c.alive = false);
                creatures.clear();
                activeBullet = null;
                if (lives <= 0) { triggerFail(); return; }
                scheduleNextWave(150);
            }
        }
    }

    // =========================================================================
    //  SPAWN WAVE
    // =========================================================================
    private void spawnWave() {
        waitingSpawn = false;
        panicMode    = false;
        panicPulse   = 0f;
        if (waveIndex >= getTotalQuestions()) { finishLevel(); return; }
        waveIndex++;
        creatures.clear();

        int[] q = LevelGenerator.generateQuestion(materi, level);
        bulletNum = q[2];

        // ── Jumlah creature: selalu 3–5, makin banyak di wave akhir ──────────
        int count = 3;
        if (waveIndex >= 5) count = 4;
        if (waveIndex >= 8) count = 5;
        count = Math.min(count, 5);

        // ── Kecepatan musuh: naik bertahap per wave & level ───────────────────
        // Level 1 wave 1 = 1.2, naik ~0.1 per wave, ~0.12 per level
        float waveAccel = (waveIndex - 1) * 0.10f;
        float baseSpeed = 1.2f + level * 0.12f + waveAccel;
        baseSpeed = Math.min(baseSpeed, 5.5f);   // cap agar tidak terlalu gila

        // ── Timer disesuaikan dengan kecepatan musuh ──────────────────────────
        // Hitung berapa tick musuh butuh untuk turun dari atas ke GUNZONE_Y
        // Musuh spawn di y = HUD_H + 20, harus sampai GUNZONE_Y
        int travelPixels = GUNZONE_Y - HUD_H - 20;
        // Beri waktu 1.5x travel time agar pemain punya cukup waktu tembak
        int autoTime = (int)((travelPixels / baseSpeed) * 1.5f);
        // Clamp: minimal 5 detik, maksimal 15 detik
        timerMax = Math.max(5 * FPS, Math.min(15 * FPS, autoTime));

        int targetIdx = rng.nextInt(count);
        int[] xs = spreadXPositions(count);

        for (int i = 0; i < count; i++) {
            String expr;
            boolean isTarget = (i == targetIdx);
            if (isTarget) {
                expr = q[0] + " " + LevelGenerator.getOperator(materi) + " " + q[1];
            } else {
                expr = makeWrongExpr(bulletNum, level, materi);
            }
            float spd = baseSpeed + rng.nextFloat() * 0.3f;
            int type = rng.nextInt(7);
            SeaCreature sc = new SeaCreature(xs[i], expr, isTarget, spd, type);
            if (level >= 10) sc.wobbleAmp = 10f + level * 0.4f;
            creatures.add(sc);
        }
        timerTicks = timerMax;
        activeBullet = null;
    }

    private int[] spreadXPositions(int count) {
        int margin = 20, slot = (W - 2 * margin) / count;
        int[] pos = new int[count];
        for (int i = 0; i < count; i++) {
            int base = margin + i * slot;
            int jitter = rng.nextInt(Math.max(1, slot - SeaCreature.W));
            pos[i] = base + jitter;
            pos[i] = Math.max(margin, Math.min(W - SeaCreature.W - margin, pos[i]));
        }
        return pos;
    }

    private String makeWrongExpr(int correct, int level, String materi) {
        String op = LevelGenerator.getOperator(materi);

        for (int t = 0; t < 40; t++) {
            // ── Hasilkan angka salah sesuai skala level ───────────────────────
            int wrong;
            if (level >= 30) {
                int delta = 50 + rng.nextInt(500);
                wrong = correct + (rng.nextBoolean() ? delta : -delta);
            } else if (level >= 20) {
                int delta = 5 + rng.nextInt(50);
                wrong = correct + (rng.nextBoolean() ? delta : -delta);
            } else if (level >= 10) {
                int delta = 2 + rng.nextInt(15);
                wrong = correct + (rng.nextBoolean() ? delta : -delta);
            } else {
                do { wrong = 1 + rng.nextInt(Math.max(10, correct + 8)); }
                while (wrong == correct);
            }
            if (wrong == correct || wrong <= 0) continue;

            // ── Bentuk ekspresi SESUAI materi, tidak dicampur ─────────────────
            switch (materi.toLowerCase()) {

                case DatabaseManager.PENJUMLAHAN: {
                    // Hanya pakai +
                    // a + b = wrong  →  pilih a acak, b = wrong - a
                    int a = 1 + rng.nextInt(Math.max(1, wrong - 1));
                    int b = wrong - a;
                    if (b <= 0) continue;
                    return a + " + " + b;
                }

                case DatabaseManager.PENGURANGAN: {
                    // Hanya pakai -
                    // (wrong + b) - b = wrong  →  b acak kecil
                    int b = 1 + rng.nextInt(Math.max(1, Math.min(wrong, 50)));
                    return (wrong + b) + " - " + b;
                }

                case DatabaseManager.PERKALIAN: {
                    // Hanya pakai x
                    // Cari pasangan faktor dari wrong
                    java.util.List<int[]> facs = new java.util.ArrayList<>();
                    for (int f = 2; f <= Math.min(wrong, 25); f++)
                        if (wrong % f == 0) facs.add(new int[]{f, wrong / f});
                    if (!facs.isEmpty()) {
                        int[] p = facs.get(rng.nextInt(facs.size()));
                        return p[0] + " x " + p[1];
                    }
                    // Tidak ada faktor bulat — coba wrong ± 1 sampai ada faktor
                    for (int adj = 1; adj <= 10; adj++) {
                        int candidate = wrong + adj;
                        for (int f = 2; f <= Math.min(candidate, 25); f++) {
                            if (candidate % f == 0 && candidate / f <= 25) {
                                return f + " x " + (candidate / f);
                            }
                        }
                    }
                    // Fallback: perkalian sederhana yang hasilnya beda
                    int a = 2 + rng.nextInt(10);
                    int b = 2 + rng.nextInt(10);
                    if (a * b != correct) return a + " x " + b;
                    continue;
                }

                case DatabaseManager.PEMBAGIAN: {
                    // Hanya pakai :
                    // (wrong * bagi) : bagi = wrong
                    int bagi = 2 + rng.nextInt(Math.max(1, Math.min(wrong, 24)));
                    return (wrong * bagi) + " : " + bagi;
                }

                default: {
                    int a = 1 + rng.nextInt(Math.max(1, wrong - 1));
                    return a + " + " + (wrong - a);
                }
            }
        }
        // Fallback aman per materi
        switch (materi.toLowerCase()) {
            case DatabaseManager.PENJUMLAHAN: return (correct + 1) + " + 1";
            case DatabaseManager.PENGURANGAN: return (correct + 2) + " - 1";
            case DatabaseManager.PERKALIAN:   return "2 x " + (correct / 2 + 1);
            case DatabaseManager.PEMBAGIAN:   return (correct * 2 + 2) + " : 2";
            default:                          return (correct + 1) + " + 1";
        }
    }

    // =========================================================================
    //  KLIK & PELURU
    // =========================================================================
    private void onCanvasClick(int mx, int my) {
        if (phase != Phase.PLAYING || waitingSpawn) return;
        if (activeBullet != null) return;  // peluru masih di udara
        if (creatures.isEmpty()) return;

        // Cari creature yang diklik
        for (SeaCreature cr : creatures) {
            if (!cr.alive || cr.exploding) continue;
            int dy = (int)(cr.y + Math.sin(cr.wobblePhase) * cr.wobbleAmp);
            if (mx >= cr.x && mx <= cr.x + SeaCreature.W &&
                my >= dy   && my <= dy + SeaCreature.H) {
                // Spawn peluru menuju creature ini
                activeBullet = new Bullet(CANNON_X, CANNON_TIP, cr);
                gunRecoilT = 10;
                muzzleT    = 8;
                SoundManager.getInstance().play("shoot");
                break;
            }
        }
    }

    /** Dipanggil saat peluru tiba di target */
    private void processBulletHit(SeaCreature target) {
        target.explode();
        if (target.isTarget) {
            // ── BENAR ─────────────────────────────────────────────────────────
            comboStreak++;
            if (comboStreak > maxCombo) maxCombo = comboStreak;

            // Multiplier: 1x, 2x, 3x, 4x (max)
            comboMulti = Math.min(4f, 1f + (comboStreak - 1) * 0.5f);

            int timeBonus = (int)(timerTicks / (float)timerMax * 80);
            int basePts   = 100 + timeBonus;
            int pts       = (int)(basePts * comboMulti);
            score += pts;
            correctCount++;
            SoundManager.getInstance().play("correct");

            // Teks feedback berdasarkan combo — lebih ekspresif
            if (comboStreak >= 5) {
                showFeedback("COMBO x" + comboStreak + "!  +" + pts, new Color(255, 80, 30));
                triggerCombo("COMBO x" + comboStreak + "!", new Color(255, 100, 30));
            } else if (comboStreak >= 3) {
                showFeedback("STREAK x" + comboStreak + "!  +" + pts, new Color(255, 220, 30));
                triggerCombo("STREAK x" + comboStreak, new Color(255, 220, 30));
            } else if (comboStreak == 2) {
                showFeedback("NICE!  +" + pts, new Color(80, 255, 180));
            } else {
                showFeedback("TEPAT!  +" + pts, new Color(80, 255, 180));
            }

            // Bonus life setiap 5 combo beruntun
            if (comboStreak % 5 == 0 && lives < 5) {
                lives++;
                floatLabels.add(new FloatLabel("+1 NYAWA!",
                        W / 2, H / 2 - 60, new Color(255, 100, 150)));
                SoundManager.getInstance().play("levelup");
            }

            // Spawn banyak sparkle saat benar
            for (int i = 0; i < 18; i++)
                sparkles.add(new SparkleParticle(
                    (int)target.x + SeaCreature.W/2,
                    (int)target.y + SeaCreature.H/2,
                    new Color(255, 200 + rng.nextInt(55), 50 + rng.nextInt(100)), rng));

            spawnSplash((int)target.x + SeaCreature.W / 2,
                        (int)target.y + SeaCreature.H / 2,
                        new Color(80, 220, 255));
            floatLabels.add(new FloatLabel(
                    (comboMulti > 1f ? "x" + String.format("%.0f", comboMulti) + "  " : "") + "+" + pts,
                    (int)target.x + SeaCreature.W / 2,
                    (int)target.y - 10,
                    comboStreak >= 3 ? new Color(255, 220, 30) : new Color(60, 255, 180)));
            scheduleNextWave(comboStreak >= 3 ? 50 : 100);

        } else {
            // ── SALAH ─────────────────────────────────────────────────────────
            comboStreak = 0;
            comboMulti  = 1f;
            lives--;
            shakeT = 16;
            SoundManager.getInstance().play("wrong");
            showFeedback("Salah! -Nyawa", new Color(255, 80, 80));
            spawnSplash((int)target.x + SeaCreature.W / 2,
                        (int)target.y + SeaCreature.H / 2,
                        new Color(255, 120, 80));
            floatLabels.add(new FloatLabel("-Nyawa",
                    (int)target.x + SeaCreature.W / 2,
                    (int)target.y - 10,
                    new Color(255, 60, 60)));
            if (lives <= 0) {
                scheduleFailAfter(600);
                return;
            }
            creatures.forEach(c -> { if (c != target) c.alive = false; });
            scheduleNextWave(150);
        }
    }

    /** Tampilkan teks combo besar di tengah layar */
    private void triggerCombo(String text, Color col) {
        comboText    = text;
        comboDisplayT = 55;
        // Spawn banyak splash di tengah
        for (int i = 0; i < 3; i++)
            spawnSplash(W / 2 + rng.nextInt(200) - 100,
                        H / 2 + rng.nextInt(100) - 50,
                        col);
    }

    private void scheduleNextWave(int delayMs) {
        waitingSpawn = true;
        creatures.clear();
        javax.swing.Timer t = new javax.swing.Timer(delayMs, ev -> spawnWave());
        t.setRepeats(false);
        t.start();
    }

    private void scheduleFailAfter(int delayMs) {
        javax.swing.Timer t = new javax.swing.Timer(delayMs, ev -> triggerFail());
        t.setRepeats(false);
        t.start();
    }

    /** Tombol kembali ke menu — konfirmasi dulu */
    private void onMenuButtonClick() {
        if (gameTimer != null) gameTimer.stop();
        int choice = JOptionPane.showOptionDialog(
            this,
            "Kembali ke menu level?\nProgress soal ini akan hilang.",
            "Keluar Game",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new String[]{"Ya, Keluar", "Lanjut Main"},
            "Lanjut Main"
        );
        if (choice == JOptionPane.YES_OPTION) {
            TransitionManager.fadeOut(this, () -> {
                new LevelForm(materi).setVisible(true);
                dispose();
            });
        } else {
            if (gameTimer != null) gameTimer.start();
            canvas.requestFocusInWindow();
        }
    }

    // =========================================================================
    //  SELESAI / GAGAL
    // =========================================================================
    private void finishLevel() {
        gameTimer.stop();
        phase = Phase.DONE;
        int tq = getTotalQuestions();
        // Bintang: 3 = semua benar, 2 = >=80%, 1 = >=60%, 0 = <60%
        int stars = correctCount >= tq     ? 3
                  : correctCount >= (int)(tq * 0.8) ? 2
                  : correctCount >= (int)(tq * 0.6) ? 1 : 0;
        SoundManager.getInstance().play("levelup");
        if (stars > 0) {
            db.saveScore(userId, materi, level, score, stars);
            db.unlockNextLevel(userId, materi, level);
        }
        SwingUtilities.invokeLater(() -> showResultDialog(true, stars));
    }

    private void triggerFail() {
        gameTimer.stop();
        phase = Phase.DONE;
        SoundManager.getInstance().play("gameover");
        SwingUtilities.invokeLater(() -> showResultDialog(false, 0));
    }

    // =========================================================================
    //  EFEK HELPER
    // =========================================================================
    private void spawnSplash(int cx, int cy, Color col) {
        for (int i = 0; i < 22; i++) splashFx.add(new BubbleFx(cx, cy, col, rng));
    }

    /**
     * Spawn cluster rumput laut bergerak dalam zona x dan y tertentu.
     * Batang-batang dalam satu cluster berdekatan (jarak 12-28px) dan
     * tingginya bervariasi untuk tampilan natural.
     */
    private void spawnSeaweedCluster(int count, int xMin, int xMax, int yMin, int yRange) {
        int zoneW = Math.max(1, xMax - xMin);
        // Distribusi x merata dalam zona, dengan jitter kecil
        for (int i = 0; i < count; i++) {
            float xBase = xMin + (i + 0.2f + rng.nextFloat() * 0.6f) * (zoneW / (float)count);
            xBase = Math.max(xMin, Math.min(xMax, xBase));
            // y: lebih banyak di baris atas (dekat garis pasir), makin jarang ke bawah
            float yFrac = (float)Math.pow(rng.nextFloat(), 1.5f); // bias ke atas
            float yBase = yMin + 5 + yFrac * Math.max(10, yRange - 20);
            yBase = Math.min(yBase, yMin + yRange - 10);

            BgSandCreature sw = new BgSandCreature(W, H, rng, yMin, 5);
            sw.baseX = xBase;
            sw.baseY = yBase;
            sw.x = xBase;
            sw.y = yBase;
            // Variasi tinggi per batang: 45-120px
            sw.wobbleAmp = 2f + rng.nextFloat() * 4f;
            bgAnimals.add(sw);
        }
    }

    private void showFeedback(String text, Color color) {
        feedbackText = text; feedbackColor = color; feedbackAlpha = 255;
    }

    /** Jumlah soal per sesi berdasarkan level */
    private int getTotalQuestions() {
        if (level <= 10) return 5;
        if (level <= 20) return 6;
        if (level <= 30) return 7;
        if (level <= 40) return 8;
        return 9;   // level 41-50
    }

    // =========================================================================
    //  DRAW MASTER
    // =========================================================================
    private void drawAll(Graphics2D g) {
        // ── Screen shake ──────────────────────────────────────────────────────
        if (shakeT > 0) {
            g.translate((int)shakeX, (int)shakeY);
        }

        // ── Background laut ───────────────────────────────────────────────────
        UITheme.drawOceanBg(g, W, H);

        // ── Hewan AIR — digambar sebelum pasir, dengan alpha dikurangi ────────
        // Composite alpha 0.55 agar hewan background terlihat "jauh di belakang"
        Composite origComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        for (BgAnimal a : bgAnimals) {
            if (!(a instanceof BgSandCreature) && !(a instanceof BgCrabWalk)) a.draw(g);
        }
        g.setComposite(origComposite);

        // ── Overlay aquarium di area soal — membuat background mundur ─────────
        // Gradien biru gelap transparan dari HUD ke GUNZONE_Y
        g.setPaint(new java.awt.GradientPaint(
            0, HUD_H,      new Color(2, 18, 55, 0),
            0, GUNZONE_Y,  new Color(2, 18, 55, 80)
        ));
        g.fillRect(0, HUD_H, W, GUNZONE_Y - HUD_H);
        g.setPaint(null);

        // ── Vignette tepi kiri & kanan (efek aquarium glass) ─────────────────
        int vigW = (int)(W * 0.08);
        g.setPaint(new java.awt.GradientPaint(
            0, 0, new Color(0, 10, 40, 100), vigW, 0, new Color(0, 10, 40, 0)));
        g.fillRect(0, HUD_H, vigW, GUNZONE_Y - HUD_H);
        g.setPaint(new java.awt.GradientPaint(
            W - vigW, 0, new Color(0, 10, 40, 0), W, 0, new Color(0, 10, 40, 100)));
        g.fillRect(W - vigW, HUD_H, vigW, GUNZONE_Y - HUD_H);
        g.setPaint(null);

        drawCoralZone(g);

        // ── Hewan PASIR — alpha sedikit dikurangi agar tidak ganggu soal ──────
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        for (BgAnimal a : bgAnimals) {
            if (a instanceof BgSandCreature || a instanceof BgCrabWalk) a.draw(g);
        }
        g.setComposite(origComposite);

        drawGunZone(g);
        if (activeBullet != null) activeBullet.draw(g);
        // Soal & peluru digambar dengan opacity penuh — terang & jelas
        for (SeaCreature cr : creatures) cr.draw(g);
        for (BubbleFx b : splashFx)     b.draw(g);
        for (SparkleParticle s : sparkles) s.draw(g);
        for (FloatLabel fl : floatLabels) fl.draw(g);
        drawHUD(g);
        if (phase == Phase.PLAYING)     { drawTimerBar(g); drawProgressDots(g); }
        if (phase == Phase.PLAYING || phase == Phase.COUNTDOWN)  drawFeedback(g);
        if (phase == Phase.PLAYING)     drawComboDisplay(g);
        if (phase == Phase.COUNTDOWN)   drawCountdown(g);

        // ── Panic overlay ─────────────────────────────────────────────────────
        if (panicMode && warnFlashT < 6) {
            float pulse = (float)(Math.sin(panicPulse) * 0.5 + 0.5);
            g.setColor(new Color(255, 30, 30, (int)(pulse * 55)));
            g.fillRect(0, 0, W, H);
            g.setColor(new Color(255, 50, 50, (int)(pulse * 180)));
            g.setStroke(new BasicStroke(8f));
            g.drawRect(4, 4, W - 8, H - 8);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // ── Zona Karang & Hiasan Dasar ────────────────────────────────────────────
    private void drawCoralZone(Graphics2D g) {
        int sy = GUNZONE_Y;

        // ── Pasir: smooth gradient, tidak terlalu banyak gundukan ────────────
        // Layer dasar
        g.setPaint(new GradientPaint(0, sy, new Color(225, 192, 118),
                                     0, H,  new Color(168, 132, 68)));
        g.fillRect(0, sy, W, H - sy);

        // Gundukan pasir — 4 bukit halus, tidak terlalu tinggi
        g.setPaint(new GradientPaint(0, sy - 28, new Color(255, 242, 185),
                                     0, sy + 12, new Color(218, 185, 112)));
        g.fillOval((int)(-0.03*W), sy - 26, (int)(0.30*W), 52);
        g.fillOval((int)( 0.22*W), sy - 18, (int)(0.24*W), 40);
        g.fillOval((int)( 0.42*W), sy - 24, (int)(0.20*W), 48);
        g.fillOval((int)( 0.60*W), sy - 16, (int)(0.24*W), 38);
        g.fillOval((int)( 0.78*W), sy - 26, (int)(0.26*W), 52);
        g.fillRect(0, sy - 6, W, H - sy + 6);

        // Highlight tipis di puncak
        g.setPaint(new GradientPaint(0, sy - 28, new Color(255, 252, 225, 130),
                                     0, sy - 8,  new Color(255, 252, 225, 0)));
        g.fillOval((int)(-0.03*W), sy - 26, (int)(0.30*W), 28);
        g.fillOval((int)( 0.42*W), sy - 24, (int)(0.20*W), 26);
        g.fillOval((int)( 0.78*W), sy - 26, (int)(0.26*W), 28);
        g.setPaint(null);

        // Riak pasir halus — 3 baris saja
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[] riyY = {sy + 16, sy + 32, sy + 48};
        int[] riyA = {22, 14, 8};
        for (int i = 0; i < 3; i++) {
            if (riyY[i] > H - 5) break;
            g.setColor(new Color(175, 140, 65, riyA[i]));
            g.drawArc(-8,    riyY[i],     W/3+16, 9, 0, 180);
            g.drawArc(W/3,   riyY[i] + 3, W/3+16, 8, 0, 180);
            g.drawArc(W*2/3, riyY[i] + 1, W/3+16, 9, 0, 180);
        }
        g.setStroke(new BasicStroke(1f));

        // ── Helper: gambar satu cluster rumput laut rapi ──────────────────────
        // Pola tinggi: pendek-tinggi-sedang-tinggi-pendek (natural)
        int[] heights = {72, 105, 82, 118, 88, 100, 75, 112, 90, 78, 108, 85};
        Color[] greens = {
            new Color(38, 172, 30), new Color(52, 196, 42), new Color(44, 182, 35),
            new Color(58, 204, 46), new Color(46, 188, 37), new Color(54, 200, 44),
            new Color(40, 178, 32), new Color(56, 202, 45), new Color(48, 192, 38),
            new Color(42, 184, 34), new Color(60, 206, 48), new Color(50, 194, 40)
        };

        // ── CLUSTER KIRI — 12 batang, jarak 14px, rapi ───────────────────────
        int lx = (int)(W * 0.015);
        for (int i = 0; i < 12; i++) {
            drawSeaweedGreen(g, lx + i * 14, sy, heights[i], greens[i]);
        }
        // Karang di sebelah kanan cluster kiri
        drawBranchCoral(g, lx + 175, sy, new Color(200, 55, 90), 5);
        drawFluffyCoral(g, lx + 228, sy, new Color(255, 145, 185), 38);
        // Rumput laut kecil setelah karang (6 batang)
        for (int i = 0; i < 6; i++) {
            drawSeaweedGreen(g, lx + 268 + i * 14, sy,
                heights[(i + 3) % 12], greens[(i + 4) % 12]);
        }
        // Dekorasi
        drawDecorStarfish(g, lx + 155, sy + 5, new Color(255, 160, 40), 13);
        drawGameUrchin(g,   lx + 200, sy + 8, new Color(80, 30, 100));
        drawShell(g,        lx + 248, sy + 6, new Color(255, 220, 200));
        drawGameCrab(g,     lx + 350, sy + 4, new Color(240, 90, 60));

        // ── CLUSTER KANAN — 12 batang, jarak 14px, rapi ──────────────────────
        int rx = (int)(W * 0.985) - 12 * 14;  // mulai dari kanan
        for (int i = 0; i < 12; i++) {
            drawSeaweedGreen(g, rx + i * 14, sy,
                heights[(i + 6) % 12], greens[(i + 6) % 12]);
        }
        // Karang di sebelah kiri cluster kanan
        drawFluffyCoral(g, rx - 55,  sy, new Color(200, 120, 240), 38);
        drawFanCoral(g,   rx - 100, sy, new Color(175, 105, 225), 46, 40);
        // Rumput laut kecil sebelum karang (6 batang)
        for (int i = 0; i < 6; i++) {
            drawSeaweedGreen(g, rx - 145 + i * 14, sy,
                heights[(i + 2) % 12], greens[(i + 8) % 12]);
        }
        // Dekorasi
        drawDecorStarfish(g, rx + 12 * 14 - 20, sy + 5, new Color(255, 100, 130), 12);
        drawGameUrchin(g,   rx - 72, sy + 8, new Color(100, 40, 140));
        drawSpiralShell(g,  rx + 12 * 14 + 10, sy + 6, new Color(220, 240, 255));
        drawGameCrab(g,     rx - 118, sy + 4, new Color(220, 80, 55));

        // ── CLUSTER TENGAH-KIRI — 5 batang rapi (x: 28%~35%) ────────────────
        int mx1 = (int)(W * 0.285);
        for (int i = 0; i < 5; i++) {
            drawSeaweedGreen(g, mx1 + i * 14, sy,
                heights[(i + 1) % 12], greens[(i + 2) % 12]);
        }
        drawTubeCoral(g,  mx1 + 80, sy, new Color(210, 88, 28));
        drawShell(g,      mx1 + 110, sy + 5, new Color(240, 225, 205));

        // ── CLUSTER TENGAH-KANAN — 5 batang rapi (x: 63%~70%) ───────────────
        int mx2 = (int)(W * 0.635);
        for (int i = 0; i < 5; i++) {
            drawSeaweedGreen(g, mx2 + i * 14, sy,
                heights[(i + 4) % 12], greens[(i + 7) % 12]);
        }
        drawTubeCoral(g,  mx2 - 35, sy, new Color(175, 78, 215));
        drawAnemon(g,     mx2 + 85, sy, new Color(80, 170, 255), 20);
        drawSpiralShell(g, mx2 + 60, sy + 5, new Color(235, 220, 200));
    }

    /** Rumput laut lapisan belakang: tipis, gelap */
    private void drawSeaweedBack(Graphics2D g, int x, int y, int height,
                                  Color col, int dir) {
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int segs = 5;
        int px = x, py = y;
        for (int i = 0; i < segs; i++) {
            float sway = (float)(Math.sin(coralAnim * 1.1f + x * 0.06f + i * 0.7f) * 7 * dir);
            int nx = x + (int)sway;
            int ny = y - (i + 1) * (height / segs);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(),
                                 col.getAlpha() - i * 20));
            g.drawLine(px, py, nx, ny);
            px = nx; py = ny;
        }
        g.setStroke(new BasicStroke(1f));
    }

    /** Bulu babi untuk gameplay */
    private void drawGameUrchin(Graphics2D g, int cx, int cy, Color color) {
        g.setColor(color.darker());
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16;
            g.drawLine(cx + (int)(Math.cos(angle) * 6), cy + (int)(Math.sin(angle) * 6),
                       cx + (int)(Math.cos(angle) * 13), cy + (int)(Math.sin(angle) * 13));
        }
        g.setColor(color);
        g.fillOval(cx - 6, cy - 6, 12, 12);
        g.setColor(color.darker());
        g.setStroke(new BasicStroke(1f));
        g.drawOval(cx - 6, cy - 6, 12, 12);
        g.setColor(new Color(255, 255, 255, 55));
        g.fillOval(cx - 2, cy - 3, 4, 3);
        g.setStroke(new BasicStroke(1f));
    }

    /** Kepiting kecil untuk gameplay */
    private void drawGameCrab(Graphics2D g, int cx, int cy, Color color) {
        g.setColor(color.darker());
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx - 14, cy + 4, cx - 22, cy + 12);
        g.drawLine(cx + 14, cy + 4, cx + 22, cy + 12);
        g.fillOval(cx - 26, cy - 6, 14, 10);
        g.fillOval(cx + 12, cy - 6, 14, 10);
        g.setColor(color);
        g.fillOval(cx - 18, cy - 6, 36, 22);
        g.setColor(new Color(255, 255, 255, 70));
        g.fillOval(cx - 10, cy - 3, 12, 6);
        g.setColor(color.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(cx - 8, cy - 6, cx - 8, cy - 14);
        g.drawLine(cx + 8, cy - 6, cx + 8, cy - 14);
        g.setColor(Color.WHITE);
        g.fillOval(cx - 12, cy - 18, 8, 8);
        g.fillOval(cx + 4, cy - 18, 8, 8);
        g.setColor(new Color(40, 55, 70));
        g.fillOval(cx - 9, cy - 15, 3, 3);
        g.fillOval(cx + 7, cy - 15, 3, 3);
        g.setColor(color.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc(cx - 6, cy + 1, 12, 8, 210, 120);
        g.setStroke(new BasicStroke(1f));
    }

    /** Rumput laut hijau tinggi bergoyang — lebih realistis, batang tipis melengkung */
    private void drawSeaweedGreen(Graphics2D g, int x, int y, int height, Color col) {
        int segs = 12;  // lebih banyak segmen = lebih halus
        int px = x, py = y;

        for (int i = 0; i < segs; i++) {
            float t = i / (float) segs;
            // Sway makin besar ke atas, dengan frekuensi berbeda per batang
            float sway = (float)(Math.sin(coralAnim * 1.2f + x * 0.05f + i * 0.55f)
                               * (6 + i * 2.2f));
            // Batang sedikit melengkung ke satu arah (natural)
            float lean = (float)(Math.sin(x * 0.03f) * i * 1.5f);
            int nx = x + (int)(sway + lean);
            int ny = y - (i + 1) * (height / segs);

            // Ketebalan makin tipis ke atas
            float thick = Math.max(0.8f, 3.5f - t * 2.8f);
            g.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Warna: hijau tua di bawah, hijau muda cerah di atas
            int rv = Math.min(255, col.getRed()   + (int)(t * 20));
            int gv = Math.min(255, col.getGreen() + (int)(t * 45));
            int bv = Math.min(255, col.getBlue()  + (int)(t * 10));
            g.setColor(new Color(rv, gv, bv, 220 - (int)(t * 40)));
            g.drawLine(px, py, nx, ny);

            // Daun kecil memanjang di setiap 3 segmen — lebih realistis
            if (i % 3 == 2 && i < segs - 1) {
                int leafDir = (i % 6 < 3) ? 1 : -1;
                int lw = (int)(8 + t * 10);
                int lh = (int)(3 + t * 3);
                g.setStroke(new BasicStroke(1f));
                Color leafCol = new Color(
                    Math.min(255, rv + 15), Math.min(255, gv + 25), bv, 200);
                g.setColor(leafCol);
                java.awt.geom.AffineTransform old = g.getTransform();
                double leafAngle = Math.atan2(ny - py, nx - px)
                                 + Math.PI / 2.5 * leafDir
                                 + Math.sin(coralAnim + i) * 0.15;
                g.rotate(leafAngle, nx, ny);
                g.fillOval(nx - lw/2, ny - lh/2, lw, lh);
                // Urat tengah daun
                g.setColor(new Color(rv - 20, gv - 15, bv, 120));
                g.setStroke(new BasicStroke(0.6f));
                g.drawLine(nx - lw/2 + 1, ny, nx + lw/2 - 1, ny);
                g.setTransform(old);
                g.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            px = nx; py = ny;
        }
        // Ujung runcing kecil
        g.setColor(new Color(
            Math.min(255, col.getRed() + 50),
            Math.min(255, col.getGreen() + 60),
            Math.min(255, col.getBlue() + 15), 160));
        g.fillOval(px - 3, py - 4, 6, 8);
        g.setStroke(new BasicStroke(1f));
    }

    /** Karang bercabang merah/pink */
    private void drawBranchCoral(Graphics2D g, int x, int y, Color col, int branches) {
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(col.darker());
        g.drawLine(x, y, x, y - 38);  // batang utama
        for (int i = 0; i < branches; i++) {
            double angle = -Math.PI / 2 + (i - branches / 2.0) * 0.62
                         + Math.sin(coralAnim + i) * 0.05;
            int len = 24 + (branches - Math.abs(i - branches/2)) * 7;
            int bx2 = x + (int)(Math.cos(angle) * len);
            int by2 = y - 38 + (int)(Math.sin(angle) * len);
            g.setColor(col.darker());
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x, y - 38, bx2, by2);
            // Sub-cabang
            double subA = angle + (i % 2 == 0 ? 0.4 : -0.4);
            int sx = bx2 + (int)(Math.cos(subA) * 14);
            int sy2 = by2 + (int)(Math.sin(subA) * 14);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(col);
            g.drawLine(bx2, by2, sx, sy2);
            g.setColor(col.brighter());
            g.fillOval(sx - 4, sy2 - 4, 8, 8);
            g.fillOval(bx2 - 4, by2 - 4, 8, 8);
        }
        g.setStroke(new BasicStroke(1f));
    }

    /** Karang fluffy/brain coral bulat warna pink */
    private void drawFluffyCoral(Graphics2D g, int cx, int y, Color col, int r) {
        // Kumpulan lingkaran kecil membentuk gumpalan
        for (int i = 0; i < 14; i++) {
            double angle = Math.PI * 2 * i / 14;
            int ox = (int)(Math.cos(angle) * r * 0.55);
            int oy = (int)(Math.sin(angle) * r * 0.45);
            int br = 12 + (i % 3) * 4;
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 200));
            g.fillOval(cx + ox - br, y - r - oy - br, br*2, br*2);
        }
        // Tengah lebih cerah
        g.setColor(col.brighter());
        g.fillOval(cx - r/2, y - (int)(r*1.3), r, r);
        // Border seluruh gumpalan
        g.setColor(col.darker());
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(cx - r, y - (int)(r * 1.8), r * 2, (int)(r * 1.8));
        g.setStroke(new BasicStroke(1f));
    }

    /** Anemon kecil */
    private void drawAnemon(Graphics2D g, int cx, int y, Color col, int r) {
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 8; i++) {
            float sway = (float)(Math.sin(coralAnim * 1.8f + i * 0.9f) * 5);
            double angle = Math.PI * i / 4 - Math.PI / 2;
            int ex = cx + (int)(Math.cos(angle) * r) + (int)sway;
            int ey = y - r - (int)(Math.abs(Math.sin(angle)) * 10);
            g.setColor(col.darker());
            g.drawLine(cx, y, ex, ey);
            g.setColor(col.brighter());
            g.fillOval(ex - 5, ey - 5, 10, 10);
        }
        g.setStroke(new BasicStroke(1f));
        g.setColor(col.darker());
        g.fillOval(cx - 8, y - 6, 16, 14);
    }

    /** Karang kipas fan shape (ungu) */
    private void drawFanCoral(Graphics2D g, int cx, int y, Color col, int w, int h) {
        // Batang
        g.setColor(col.darker());
        g.setStroke(new BasicStroke(3.5f));
        g.drawLine(cx, y, cx, y - h / 3);
        g.setStroke(new BasicStroke(1f));
        // Kipas: banyak garis melengkung
        for (int i = -4; i <= 4; i++) {
            double angle = -Math.PI / 2 + i * 0.18;
            int fx = cx + (int)(Math.cos(angle) * w / 2);
            int fy = y - h/3 + (int)(Math.sin(angle) * h * 0.72);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 170));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx, y - h/3, fx, fy);
        }
        // Grid horizontal
        for (int level = 1; level <= 3; level++) {
            float frac = level / 4f;
            int lx1 = cx - (int)(w/2 * frac), ly = y - h/3 - (int)(h * 0.6f * frac);
            int lx2 = cx + (int)(w/2 * frac);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 100));
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(lx1, ly, lx2, ly);
        }
        g.setStroke(new BasicStroke(1f));
    }

    /** Terumbu tabung silindris (coklat/oranye) */
    private void drawTubeCoral(Graphics2D g, int x, int y, Color col) {
        int[][] tubes = {{x, y, 18, 50}, {x + 24, y, 15, 40}, {x + 44, y, 13, 35}};
        for (int[] t : tubes) {
            int tx = t[0], ty = t[1], tw = t[2], th = t[3];
            // Badan tabung
            g.setPaint(new GradientPaint(tx, ty, col.brighter(), tx + tw, ty, col.darker()));
            g.fillRoundRect(tx - tw/2, ty - th, tw, th, tw/2, tw/2);
            g.setPaint(null);
            g.setColor(col.darker());
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(tx - tw/2, ty - th, tw, th, tw/2, tw/2);
            g.setStroke(new BasicStroke(1f));
            // Mulut tabung (oval gelap)
            g.setColor(new Color(60, 30, 10, 180));
            g.fillOval(tx - tw/2 + 1, ty - th, tw - 2, tw/2);
        }
    }

    /** Siput spiral shell */
    private void drawSpiralShell(Graphics2D g, int cx, int cy, Color col) {
        // Badan bulat
        g.setColor(col);
        g.fillOval(cx - 12, cy - 10, 22, 18);
        g.setColor(col.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(cx - 12, cy - 10, 22, 18);
        // Spiral dalam
        g.setStroke(new BasicStroke(1.2f));
        g.drawArc(cx - 8, cy - 7, 14, 12, 10, 280);
        g.drawArc(cx - 5, cy - 5, 8, 7, 20, 250);
        // Ujung/bibir kerang
        g.setColor(col.brighter());
        g.fillArc(cx - 14, cy - 2, 10, 12, 0, 180);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawCoral(Graphics2D g, int x, int y, Color col, int branches) {
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < branches; i++) {
            double angle = -Math.PI / 2 + (i - branches / 2.0) * 0.55
                         + Math.sin(coralAnim + i * 0.8f) * 0.06;
            int len = 22 + i * 9;
            int ex = x + (int)(Math.cos(angle) * len);
            int ey = y + (int)(Math.sin(angle) * len);
            g.setColor(col.darker());
            g.drawLine(x, y, ex, ey);
            g.setColor(col.brighter());
            g.fillOval(ex - 6, ey - 6, 12, 12);
            g.setColor(col);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(ex - 6, ey - 6, 12, 12);
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawDecorStarfish(Graphics2D g, int cx, int cy, Color col, int r) {
        int[] sx = new int[10], sy = new int[10];
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * i / 5 - Math.PI / 2;
            int rad = (i % 2 == 0) ? r : r / 2;
            sx[i] = cx + (int)(Math.cos(a) * rad);
            sy[i] = cy + (int)(Math.sin(a) * rad);
        }
        g.setColor(col);
        g.fillPolygon(sx, sy, 10);
        g.setColor(col.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(sx, sy, 10);
        g.setStroke(new BasicStroke(1f));
        // Dua mata
        g.setColor(new Color(60, 30, 10));
        g.fillOval(cx - 4, cy - 3, 4, 4);
        g.fillOval(cx + 1, cy - 3, 4, 4);
    }

    private void drawShell(Graphics2D g, int cx, int cy, Color col) {
        g.setColor(col);
        g.fillArc(cx - 14, cy - 10, 28, 18, 0, 180);
        g.setColor(col.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc(cx - 14, cy - 10, 28, 18, 0, 180);
        g.setStroke(new BasicStroke(1f));
        for (int i = -2; i <= 2; i++) {
            g.setColor(col.darker());
            g.drawLine(cx, cy - 2, cx + i * 6, cy - 10);
        }
    }

    // ── HUD (Atas) ────────────────────────────────────────────────────────────
    private void drawHUD(Graphics2D g) {
        // Background HUD — glossy dark blue pill
        g.setPaint(new GradientPaint(0, 0, new Color(5, 25, 70, 230),
                                     0, HUD_H, new Color(2, 12, 45, 245)));
        g.fillRect(0, 0, W, HUD_H);
        // Garis bawah HUD — glow biru
        g.setPaint(new GradientPaint(0, HUD_H, new Color(0, 180, 255, 0),
                                     W / 2, HUD_H, new Color(0, 200, 255, 160)));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(0, HUD_H, W, HUD_H);
        g.setPaint(null);
        g.setStroke(new BasicStroke(1f));

        // ── Tombol kembali ke menu (pojok kiri atas) ──────────────────────────
        drawMenuButton(g);

        // ── Hati (kiri, setelah tombol menu) ─────────────────────────────────
        int heartStartX = MENU_BTN_X + MENU_BTN_W + 10;
        int maxHearts = Math.max(3, lives);
        for (int i = 0; i < maxHearts; i++) {
            Color hc = (i < lives)
                ? new Color(255, 70, 110)
                : new Color(55, 55, 80, 160);
            drawHeart(g, heartStartX + i * 34, 12, 28, hc);
        }

        // ── Judul tengah — glossy pill ────────────────────────────────────────
        String title = LevelGenerator.getMateriName(materi) + "  ·  Level " + level;
        g.setFont(new Font("Arial Black", Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        int titleW = fm.stringWidth(title) + 32;
        int titleX = (W - titleW) / 2;
        g.setPaint(new GradientPaint(titleX, 6, new Color(0, 100, 200, 200),
                                     titleX, HUD_H - 6, new Color(0, 60, 140, 200)));
        g.fillRoundRect(titleX, 6, titleW, HUD_H - 12, 22, 22);
        g.setColor(new Color(100, 200, 255, 100));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(titleX, 6, titleW, HUD_H - 12, 22, 22);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 255, 255, 30));
        g.fillRoundRect(titleX + 2, 7, titleW - 4, (HUD_H - 12) / 2, 20, 20);
        g.setColor(new Color(130, 230, 255));
        g.setFont(new Font("Arial Black", Font.BOLD, 15));
        g.drawString(title, titleX + 16, 28);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(160, 220, 255, 200));
        String prog = "Soal " + waveIndex + " / " + getTotalQuestions() + "  |  " + SessionManager.getUsername();
        fm = g.getFontMetrics();
        g.drawString(prog, (W - fm.stringWidth(prog)) / 2, 50);

        // ── Skor (kanan) — glossy pill ────────────────────────────────────────
        String scoreStr = "⭐ " + score;
        g.setFont(new Font("Arial Black", Font.BOLD, 18));
        fm = g.getFontMetrics();
        int scorePillW = fm.stringWidth(scoreStr) + 28;
        int scorePillX = W - scorePillW - 8;
        g.setPaint(new GradientPaint(scorePillX, 8, new Color(180, 120, 0, 220),
                                     scorePillX, HUD_H - 8, new Color(100, 60, 0, 220)));
        g.fillRoundRect(scorePillX, 8, scorePillW, HUD_H - 16, 22, 22);
        g.setColor(new Color(255, 200, 50, 120));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(scorePillX, 8, scorePillW, HUD_H - 16, 22, 22);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 255, 255, 30));
        g.fillRoundRect(scorePillX + 2, 9, scorePillW - 4, (HUD_H - 16) / 2, 20, 20);
        g.setFont(new Font("Arial Black", Font.BOLD, 18));
        g.setColor(new Color(0, 0, 0, 80));
        g.drawString(scoreStr, scorePillX + 15, 38);
        g.setColor(new Color(255, 230, 50));
        g.drawString(scoreStr, scorePillX + 14, 37);

        // ── Combo multiplier pill ─────────────────────────────────────────────
        if (comboStreak >= 2) {
            String comboLabel = "🔥 x" + String.format("%.0f", comboMulti);
            g.setFont(new Font("Arial Black", Font.BOLD, 12));
            fm = g.getFontMetrics();
            int pw = fm.stringWidth(comboLabel) + 18;
            int px = W - pw - 8;
            int py = HUD_H - 22;
            Color pillCol = comboStreak >= 5
                ? new Color(255, 80, 30, 220)
                : comboStreak >= 3
                    ? new Color(255, 200, 20, 220)
                    : new Color(80, 200, 255, 220);
            g.setColor(pillCol);
            g.fillRoundRect(px, py, pw, 18, 9, 9);
            g.setColor(new Color(255, 255, 255, 50));
            g.fillRoundRect(px + 1, py + 1, pw - 2, 8, 8, 8);
            g.setColor(Color.WHITE);
            g.drawString(comboLabel, px + 9, py + 13);
        }
    }

    /** Tombol kembali ke menu — glossy pill dengan ikon panah */
    private void drawMenuButton(Graphics2D g) {
        int bx = MENU_BTN_X, by = MENU_BTN_Y, bw = MENU_BTN_W, bh = MENU_BTN_H;

        // Shadow
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(bx + 2, by + 3, bw, bh, 14, 14);

        // Background — merah/oranye saat hover, biru gelap normal
        Color bgTop = menuBtnHover ? new Color(220, 60, 60, 240) : new Color(30, 80, 180, 230);
        Color bgBot = menuBtnHover ? new Color(160, 30, 30, 240) : new Color(10, 40, 120, 230);
        g.setPaint(new GradientPaint(bx, by, bgTop, bx, by + bh, bgBot));
        g.fillRoundRect(bx, by, bw, bh, 14, 14);
        g.setPaint(null);

        // Kilap atas
        g.setColor(new Color(255, 255, 255, menuBtnHover ? 50 : 35));
        g.fillRoundRect(bx + 2, by + 2, bw - 4, bh / 2, 12, 12);

        // Border
        Color borderCol = menuBtnHover ? new Color(255, 120, 120, 200) : new Color(100, 180, 255, 160);
        g.setColor(borderCol);
        g.setStroke(new BasicStroke(1.8f));
        g.drawRoundRect(bx, by, bw, bh, 14, 14);
        g.setStroke(new BasicStroke(1f));

        // Ikon panah kiri + teks
        g.setFont(new Font("Arial Black", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        String label = "◀ Menu";
        int tx = bx + (bw - fm.stringWidth(label)) / 2;
        int ty = by + (bh + fm.getAscent() - fm.getDescent()) / 2 - 1;
        // Shadow teks
        g.setColor(new Color(0, 0, 0, 100));
        g.drawString(label, tx + 1, ty + 1);
        // Teks
        g.setColor(Color.WHITE);
        g.drawString(label, tx, ty);
    }

    private void drawHeart(Graphics2D g, int x, int y, int size, Color col) {
        g.setColor(col);
        Path2D h = new Path2D.Float();
        float cx = x + size / 2f, cy = y + size / 2f, r = size / 4f;
        h.moveTo(cx, cy + r * 1.6f);
        h.curveTo(cx - r * 2.6f, cy + r * 0.5f, cx - r * 2.6f, cy - r * 1.1f, cx, cy - r * 0.4f);
        h.curveTo(cx + r * 2.6f, cy - r * 1.1f, cx + r * 2.6f, cy + r * 0.5f, cx, cy + r * 1.6f);
        g.fill(h);
        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(h);
        g.setStroke(new BasicStroke(1f));
    }

    // ── Zona Cannon (Bawah) ───────────────────────────────────────────────────
    private void drawGunZone(Graphics2D g) {
        // Panel angka peluru (kiri)
        drawBulletPanel(g);

        // Cannon digambar DULU (di bawah banner)
        drawCannon(g, CANNON_X, CANNON_Y);

        // Banner instruksi digambar SETELAH cannon — tampil di atas bintang
        drawInstructionBanner(g);
    }

    private void drawInstructionBanner(Graphics2D g) {
        // Banner tepat di bawah ujung bawah bintang
        // Badan bintang center = (CANNON_X, CANNON_Y+28), outerR=65
        // Ujung bawah bintang = CANNON_Y + 28 + 65 = CANNON_Y + 93
        int bw = 520, bh = 28;
        int bx = (W - bw) / 2;
        int by = CANNON_Y + 93 + 5;
        if (by + bh > H - 2) by = H - bh - 2;

        // Background pill gelap transparan
        g.setColor(new Color(0, 20, 60, 200));
        g.fillRoundRect(bx, by, bw, bh, 14, 14);
        // Border glow biru
        g.setColor(new Color(80, 180, 255, 150));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(bx, by, bw, bh, 14, 14);
        g.setStroke(new BasicStroke(1f));
        // Kilap atas
        g.setColor(new Color(255, 255, 255, 25));
        g.fillRoundRect(bx + 2, by + 2, bw - 4, bh / 2, 12, 12);
        // Teks
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        String hint = "Klik makhluk yang ekspresinya sama dengan ANGKA PELURU!";
        int tx = bx + (bw - fm.stringWidth(hint)) / 2;
        g.setColor(new Color(0, 0, 0, 100));
        g.drawString(hint, tx + 1, by + 19);
        g.setColor(new Color(200, 240, 255));
        g.drawString(hint, tx, by + 18);
    }

    private void drawBulletPanel(Graphics2D g) {
        int px = 24, py = GUNZONE_Y + 28, pw = 150, ph = 90;
        // Shadow
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(px + 3, py + 4, pw, ph, 22, 22);
        // Background panel glossy
        g.setPaint(new GradientPaint(px, py, new Color(20, 60, 140, 230),
                                     px, py + ph, new Color(5, 25, 80, 240)));
        g.fillRoundRect(px, py, pw, ph, 22, 22);
        g.setPaint(null);
        // Kilap atas
        g.setColor(new Color(255, 255, 255, 35));
        g.fillRoundRect(px + 2, py + 2, pw - 4, ph / 2, 20, 20);
        // Border glow
        g.setColor(new Color(255, 220, 60, 150));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(px, py, pw, ph, 22, 22);
        g.setStroke(new BasicStroke(1f));

        // Label
        g.setFont(new Font("Arial Black", Font.BOLD, 10));
        g.setColor(new Color(255, 210, 80, 230));
        g.drawString("ANGKA PELURU", px + 12, py + 18);

        // Angka besar glossy
        g.setFont(new Font("Arial Black", Font.BOLD, 52));
        FontMetrics fm = g.getFontMetrics();
        String ns = String.valueOf(bulletNum);
        int nx = px + (pw - fm.stringWidth(ns)) / 2;
        // Shadow
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(ns, nx + 3, py + 74);
        // Gradien angka
        g.setPaint(new GradientPaint(nx, py + 30, new Color(255, 245, 80),
                                     nx + fm.stringWidth(ns), py + 72, new Color(255, 140, 10)));
        g.drawString(ns, nx, py + 72);
        g.setPaint(null);
        // Kilap angka
        g.setColor(new Color(255, 255, 255, 80));
        g.setFont(new Font("Arial Black", Font.BOLD, 52));
        g.drawString(ns, nx, py + 70);
    }

    private void drawCannon(Graphics2D g, int cx, int cy) {
        int rx = (int) gunRecoil;

        // ── Glow aura besar di bawah ──────────────────────────────────────────
        g.setColor(new Color(255, 150, 200, 60));
        g.fillOval(cx - 80 + rx, cy + 8, 160, 55);

        // ── Badan bintang laut launcher — LEBIH BESAR ─────────────────────────
        int outerR = 65, innerR = 28;
        int[] sx = new int[10], sy2 = new int[10];
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * i / 5 - Math.PI / 2;
            int r = (i % 2 == 0) ? outerR : innerR;
            sx[i] = cx + rx + (int)(Math.cos(a) * r);
            sy2[i] = cy + 28 + (int)(Math.sin(a) * r);
        }
        // Shadow
        g.setColor(new Color(0, 0, 0, 70));
        int[] ssx = new int[10], ssy = new int[10];
        for (int i = 0; i < 10; i++) { ssx[i] = sx[i] + 5; ssy[i] = sy2[i] + 7; }
        g.fillPolygon(ssx, ssy, 10);

        // Badan utama — gradient pink glossy
        g.setPaint(new GradientPaint(cx - outerR + rx, cy - 30, new Color(255, 150, 210),
                                     cx + outerR + rx, cy + 60, new Color(220, 50, 130)));
        g.fillPolygon(sx, sy2, 10);
        g.setPaint(null);

        // Border
        g.setColor(new Color(180, 30, 100));
        g.setStroke(new BasicStroke(3f));
        g.drawPolygon(sx, sy2, 10);
        g.setStroke(new BasicStroke(1f));

        // Kilap atas
        g.setColor(new Color(255, 255, 255, 80));
        g.fillOval(cx - 24 + rx, cy + 4, 32, 18);

        // Titik-titik tekstur lebih besar
        g.setColor(new Color(255, 220, 240, 170));
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5 - Math.PI / 2;
            int bx = cx + rx + (int)(Math.cos(a) * 40);
            int by = cy + 28 + (int)(Math.sin(a) * 40);
            g.fillOval(bx - 6, by - 6, 12, 12);
        }

        // Mata kawaii BESAR di badan
        g.setColor(Color.WHITE);
        g.fillOval(cx - 16 + rx, cy + 18, 16, 16);
        g.fillOval(cx + 2 + rx,  cy + 18, 16, 16);
        g.setColor(new Color(40, 20, 50));
        g.fillOval(cx - 11 + rx, cy + 23, 8, 8);
        g.fillOval(cx + 7 + rx,  cy + 23, 8, 8);
        g.setColor(new Color(255, 255, 255, 230));
        g.fillOval(cx - 9 + rx,  cy + 22, 3, 3);
        g.fillOval(cx + 9 + rx,  cy + 22, 3, 3);

        // Senyum lebar
        g.setColor(new Color(40, 20, 50));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 10 + rx, cy + 32, 20, 12, 210, 120);
        g.setStroke(new BasicStroke(1f));

        // Pipi merah
        g.setColor(new Color(255, 160, 180, 150));
        g.fillOval(cx - 28 + rx, cy + 26, 14, 9);
        g.fillOval(cx + 14 + rx, cy + 26, 14, 9);

        // ── Laras LEBIH BESAR (lubang tembak di atas) ─────────────────────────
        g.setPaint(new GradientPaint(cx - 16 + rx, cy - 80, new Color(255, 190, 230),
                                     cx + 16 + rx, cy - 20, new Color(200, 70, 150)));
        g.fillRoundRect(cx - 16 + rx, cy - 80, 32, 58, 14, 14);
        g.setPaint(null);
        g.setColor(new Color(180, 40, 110));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - 16 + rx, cy - 80, 32, 58, 14, 14);
        g.setStroke(new BasicStroke(1f));
        // Kilap laras
        g.setColor(new Color(255, 255, 255, 50));
        g.fillRoundRect(cx - 12 + rx, cy - 78, 10, 50, 8, 8);
        // Lubang laras
        g.setColor(new Color(80, 10, 50, 220));
        g.fillOval(cx - 13 + rx, cy - 82, 26, 18);

        // Muzzle flash
        if (muzzleT > 0) {
            float al = muzzleT / 8f;
            int flashR = 36;
            g.setColor(new Color(255, 200, 240, (int)(al * 200)));
            g.fillOval(cx - flashR + rx, cy - 84 - flashR, flashR * 2, flashR * 2);
            g.setColor(new Color(255, 255, 255, (int)(al * 240)));
            g.fillOval(cx - flashR/2 + rx, cy - 84 - flashR/2, flashR, flashR);
            g.setColor(new Color(255, 180, 220, (int)(al * 200)));
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                int ex = cx + rx + (int)(Math.cos(a) * flashR * 1.6f);
                int ey = cy - 84 + (int)(Math.sin(a) * flashR * 1.6f);
                g.fillOval(ex - 5, ey - 5, 10, 10);
            }
        }
    }

    // ── Countdown ─────────────────────────────────────────────────────────────
    private void drawCountdown(Graphics2D g) {
        // Overlay gelap pada area musuh
        g.setColor(new Color(0, 10, 40, 140));
        g.fillRect(0, HUD_H + 12, W, GUNZONE_Y - HUD_H - 12);

        String text = (countdownVal > 0) ? String.valueOf(countdownVal) : "GO!";
        Color col;
        switch (countdownVal) {
            case 3:  col = new Color(255, 80,  80);  break;
            case 2:  col = new Color(255, 185, 30);  break;
            case 1:  col = new Color(80,  255, 130); break;
            default: col = new Color(80,  230, 255); break;
        }

        float prog  = countdownTick / (float) CD_DUR;
        float scale = 1.2f + (1f - prog) * 0.6f;   // lebih dramatis
        float alpha = (prog < 0.6f) ? 1f : (1f - prog) / 0.4f;

        int fontSize = (int)(110 * scale);
        g.setFont(new Font("Arial Black", Font.BOLD, fontSize));
        FontMetrics fm = g.getFontMetrics();
        int tx = (W - fm.stringWidth(text)) / 2;
        int ty = (GUNZONE_Y + HUD_H) / 2 + fontSize / 3;

        // Glow luar
        g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(alpha * 60)));
        for (int d = 8; d >= 1; d--)
            g.drawString(text, tx - d, ty + d);

        // Shadow
        g.setColor(new Color(0, 0, 0, (int)(alpha * 130)));
        g.drawString(text, tx + 6, ty + 6);
        // Teks
        g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(alpha * 255)));
        g.drawString(text, tx, ty);

        // Subtitle
        if (countdownVal <= 0) {
            g.setFont(new Font("Arial Black", Font.BOLD, 20));
            g.setColor(new Color(200, 240, 255, (int)(alpha * 220)));
            String sub = "🎯 Tembak ekspresi yang hasilnya = angka peluru!";
            fm = g.getFontMetrics();
            g.drawString(sub, (W - fm.stringWidth(sub)) / 2, ty + 60);
        }

        // Lingkaran hiasan
        g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(alpha * 70)));
        int circR = (int)(130 * scale);
        g.setStroke(new BasicStroke(4f));
        g.drawOval(W/2 - circR, ty - fontSize - circR/3, circR*2, circR*2);
        g.setStroke(new BasicStroke(1f));
    }

    // ── Timer bar ──────────────────────────────────────────────────────────────
    private void drawTimerBar(Graphics2D g) {
        int bx = 20, by = HUD_H + 5, bw = W - 40, bh = 12;
        float ratio = (timerMax > 0) ? Math.max(0, timerTicks / (float)timerMax) : 1f;

        // Background track
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(bx, by, bw, bh, 8, 8);

        // Warna bar
        Color bc;
        if (panicMode) {
            // Berkedip antara merah terang dan oranye saat panic
            float pulse = (float)(Math.sin(panicPulse * 2) * 0.5 + 0.5);
            bc = new Color(255, (int)(30 + pulse * 80), 30);
        } else {
            bc = (ratio > 0.5f) ? new Color(50, 220, 130)
               : (ratio > 0.3f) ? new Color(255, 210, 30)
               : new Color(255, 80, 50);
        }

        int fw = (int)(bw * ratio);
        if (fw > 0) {
            // Glow di belakang bar
            g.setColor(new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 40));
            g.fillRoundRect(bx, by - 2, fw, bh + 4, 8, 8);
            // Bar utama
            g.setPaint(new GradientPaint(bx, by, bc.brighter(), bx, by + bh, bc));
            g.fillRoundRect(bx, by, fw, bh, 8, 8);
            g.setPaint(null);
            // Kilap atas
            g.setColor(new Color(255, 255, 255, 60));
            g.fillRoundRect(bx, by, fw, bh / 2, 8, 8);
        }
        g.setColor(new Color(255, 255, 255, 50));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(bx, by, bw, bh, 8, 8);
        g.setStroke(new BasicStroke(1f));

        // Detik tersisa — besar & merah saat panic
        int sec = (int)Math.ceil(timerTicks / (float)FPS);
        if (panicMode) {
            float pulse = (float)(Math.sin(panicPulse) * 0.5 + 0.5);
            int fontSize = (int)(28 + pulse * 10);
            g.setFont(new Font("Arial Black", Font.BOLD, fontSize));
            g.setColor(new Color(255, 50, 50, 220));
            FontMetrics fm = g.getFontMetrics();
            String secStr = sec + "s";
            g.drawString(secStr, (W - fm.stringWidth(secStr)) / 2, by + bh + fontSize + 2);
        } else {
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(bc);
            if (fw > 14)
                g.drawString(sec + "s", bx + fw + 5, by + 10);
        }
    }

    // ── Titik progress soal ───────────────────────────────────────────────────
    private void drawProgressDots(Graphics2D g) {
        int tq = getTotalQuestions();
        int r = 6, gap = 16;
        int totalW = tq * (r * 2) + (tq - 1) * gap;
        int startX = (W - totalW) / 2;
        int dy = GUNZONE_Y - 18;
        for (int i = 0; i < tq; i++) {
            int cx = startX + i * (r * 2 + gap) + r;
            Color c = (i < waveIndex - 1)  ? new Color(40, 220, 130)
                    : (i == waveIndex - 1) ? new Color(255, 210, 30)
                    : new Color(48, 68, 100, 170);
            g.setColor(c);
            g.fillOval(cx - r, dy - r, r * 2, r * 2);
            g.setColor(new Color(255, 255, 255, 45));
            g.drawOval(cx - r, dy - r, r * 2, r * 2);
        }
    }

    // ── Feedback teks tengah ──────────────────────────────────────────────────
    private void drawFeedback(Graphics2D g) {
        if (feedbackAlpha <= 0 || feedbackText.isEmpty()) return;
        int al = Math.min(feedbackAlpha, 255);
        int fontSize = 26;
        g.setFont(new Font("Arial Black", Font.BOLD, fontSize));
        FontMetrics fm = g.getFontMetrics();
        int fx = (W - fm.stringWidth(feedbackText)) / 2;
        int fy = GUNZONE_Y - 55;

        // Glow
        g.setColor(new Color(feedbackColor.getRed(), feedbackColor.getGreen(),
                             feedbackColor.getBlue(), al / 4));
        for (int d = 4; d >= 1; d--)
            g.drawString(feedbackText, fx - d, fy + d);

        // Shadow
        g.setColor(new Color(0, 0, 0, al / 3));
        g.drawString(feedbackText, fx + 2, fy + 2);
        // Teks
        g.setColor(new Color(feedbackColor.getRed(), feedbackColor.getGreen(),
                             feedbackColor.getBlue(), al));
        g.drawString(feedbackText, fx, fy);
    }

    // ── Combo display besar di tengah layar ───────────────────────────────────
    private void drawComboDisplay(Graphics2D g) {
        if (comboDisplayT <= 0 || comboText.isEmpty()) return;
        float prog  = comboDisplayT / 55f;
        float scale = 0.6f + prog * 1.0f;
        int   al    = (int)(Math.min(prog * 2.5f, 1f) * 255);
        int   fontSize = (int)(60 * scale);

        g.setFont(new Font("Arial Black", Font.BOLD, fontSize));
        FontMetrics fm = g.getFontMetrics();
        int cx = (W - fm.stringWidth(comboText)) / 2;
        int cy = (int)(H * 0.36f);

        // Glow luar besar
        g.setColor(new Color(255, 180, 0, al / 5));
        for (int d = 6; d >= 1; d--) {
            g.drawString(comboText, cx - d, cy + d);
            g.drawString(comboText, cx + d, cy - d);
        }
        // Shadow
        g.setColor(new Color(0, 0, 0, al / 2));
        g.drawString(comboText, cx + 4, cy + 4);
        // Teks utama
        Color cc = comboStreak >= 5
            ? new Color(255, 80, 30, al)
            : new Color(255, 220, 30, al);
        g.setColor(cc);
        g.drawString(comboText, cx, cy);
        // Kilap putih di atas teks
        g.setColor(new Color(255, 255, 255, al / 3));
        g.setFont(new Font("Arial Black", Font.BOLD, fontSize));
        g.drawString(comboText, cx, cy - 2);
    }

    // =========================================================================
    //  DIALOG HASIL
    // =========================================================================
    private void showResultDialog(boolean success, int stars) {
        JDialog dlg = new JDialog(this, success ? "Level Selesai!" : "Game Over", true);
        dlg.setUndecorated(true);
        dlg.setSize(480, success ? 420 : 330);
        dlg.setLocationRelativeTo(this);

        Color bgTop  = success ? new Color(5, 65, 128)  : new Color(80, 10, 30);
        Color bgBot  = success ? new Color(2, 22, 56)   : new Color(20, 5, 42);
        Color bdrCol = success ? new Color(100, 220, 255, 130) : new Color(255, 80, 80, 140);
        int finalScore  = score;
        int sc = stars, cc = correctCount;
        int mc = maxCombo;   // combo tertinggi

        JPanel panel = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Background
                g2.setPaint(new GradientPaint(0, 0, bgTop, 0, getHeight(), bgBot));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);
                g2.setColor(bdrCol);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 32, 32);
                g2.setStroke(new BasicStroke(1f));

                int cx = getWidth() / 2;
                FontMetrics fm2;

                if (success) {
                    // ── Judul ─────────────────────────────────────────────────
                    String t = "Level " + level + " Selesai!";
                    g2.setFont(new Font("Arial Black", Font.BOLD, 24));
                    fm2 = g2.getFontMetrics();
                    g2.setColor(new Color(0,0,0,80));
                    g2.drawString(t, cx - fm2.stringWidth(t)/2 + 2, 50);
                    g2.setColor(new Color(255, 215, 30));
                    g2.drawString(t, cx - fm2.stringWidth(t)/2, 48);

                    // ── Bintang ───────────────────────────────────────────────
                    drawDialogStars(g2, sc, cx, 96, 30);

                    // ── Skor besar ────────────────────────────────────────────
                    g2.setFont(new Font("Arial Black", Font.BOLD, 36));
                    fm2 = g2.getFontMetrics();
                    String scoreStr = String.format("%,d", finalScore);
                    g2.setColor(new Color(0,0,0,60));
                    g2.drawString(scoreStr, cx - fm2.stringWidth(scoreStr)/2 + 2, 162);
                    g2.setColor(new Color(255, 230, 60));
                    g2.drawString(scoreStr, cx - fm2.stringWidth(scoreStr)/2, 160);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g2.setColor(new Color(180, 210, 255, 180));
                    g2.drawString("SKOR TOTAL", cx - 30, 176);

                    // ── Stats row ─────────────────────────────────────────────
                    drawStatRow(g2, cx, 205,
                        "Benar", cc + " / " + getTotalQuestions(), new Color(80, 220, 150));
                    drawStatRow(g2, cx, 230,
                        "Combo Terbaik", "x" + mc, new Color(255, 180, 30));
                    drawStatRow(g2, cx, 255,
                        "Bintang", sc + " / 3", new Color(255, 215, 30));

                } else {
                    // ── Game Over ─────────────────────────────────────────────
                    g2.setFont(new Font("Arial Black", Font.BOLD, 28));
                    String t = "Nyawa Habis!";
                    fm2 = g2.getFontMetrics();
                    g2.setColor(new Color(255, 80, 80));
                    g2.drawString(t, cx - fm2.stringWidth(t)/2, 58);

                    g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    g2.setColor(new Color(160, 200, 255));
                    String sub = "Semangat! Kamu pasti bisa lebih baik!";
                    fm2 = g2.getFontMetrics();
                    g2.drawString(sub, cx - fm2.stringWidth(sub)/2, 90);

                    // Skor
                    g2.setFont(new Font("Arial Black", Font.BOLD, 32));
                    fm2 = g2.getFontMetrics();
                    String scoreStr = String.format("%,d", finalScore);
                    g2.setColor(new Color(255, 215, 30));
                    g2.drawString(scoreStr, cx - fm2.stringWidth(scoreStr)/2, 138);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g2.setColor(new Color(180, 210, 255, 180));
                    g2.drawString("SKOR", cx - 16, 154);

                    drawStatRow(g2, cx, 180,
                        "Benar", cc + " / " + getTotalQuestions(), new Color(80, 220, 150));
                    drawStatRow(g2, cx, 205,
                        "Combo Terbaik", "x" + mc, new Color(255, 180, 30));
                }
                g2.dispose();
            }

            // Baris statistik kecil
            private void drawStatRow(Graphics2D g2, int cx, int y,
                                      String label, String val, Color valCol) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int totalW = fm.stringWidth(label + "  " + val);
                int lx = cx - totalW / 2;
                g2.setColor(new Color(180, 210, 255, 200));
                g2.drawString(label + "  ", lx, y);
                g2.setColor(valCol);
                g2.drawString(val, lx + fm.stringWidth(label + "  "), y);
            }
        };
        panel.setOpaque(false);
        dlg.setContentPane(panel);

        if (success) {
            JButton btnNext = makePopupBtn(">> Level " + (level+1),
                                           new Color(40, 185, 108));
            btnNext.setBounds(20, 280, 200, 52);
            btnNext.setEnabled(level < 50 && stars > 0);
            panel.add(btnNext);

            JButton btnRetry = makePopupBtn("Ulangi", new Color(218, 148, 20));
            btnRetry.setBounds(248, 280, 200, 52);
            panel.add(btnRetry);

            JButton btnMenu = makePopupBtn("Menu Level", new Color(0, 98, 158));
            btnMenu.setBounds(120, 346, 220, 48);
            panel.add(btnMenu);

            btnNext.addActionListener(e -> {
                dlg.dispose();
                TransitionManager.fadeOut(GameplayForm.this, () -> {
                    new GameplayForm(materi, level + 1).setVisible(true);
                    dispose();
                });
            });
            btnRetry.addActionListener(e -> {
                dlg.dispose();
                TransitionManager.fadeOut(GameplayForm.this, () -> {
                    new GameplayForm(materi, level).setVisible(true);
                    dispose();
                });
            });
            btnMenu.addActionListener(e -> {
                dlg.dispose();
                TransitionManager.fadeOut(GameplayForm.this, () -> {
                    new LevelForm(materi).setVisible(true);
                    dispose();
                });
            });
        } else {
            JButton btnRetry = makePopupBtn("Coba Lagi", new Color(218, 58, 58));
            btnRetry.setBounds(20, 228, 200, 52);
            panel.add(btnRetry);

            JButton btnMenu = makePopupBtn("Menu", new Color(0, 98, 158));
            btnMenu.setBounds(248, 228, 200, 52);
            panel.add(btnMenu);

            btnRetry.addActionListener(e -> {
                dlg.dispose();
                TransitionManager.fadeOut(GameplayForm.this, () -> {
                    new GameplayForm(materi, level).setVisible(true);
                    dispose();
                });
            });
            btnMenu.addActionListener(e -> {
                dlg.dispose();
                TransitionManager.fadeOut(GameplayForm.this, () -> {
                    new LevelForm(materi).setVisible(true);
                    dispose();
                });
            });
        }

        dlg.setVisible(true);
    }

    private void drawDialogStars(Graphics2D g, int stars, int cx, int cy, int size) {
        int totalW = 3 * size * 2 + 2 * 10;
        int sx = cx - totalW / 2;
        for (int i = 0; i < 3; i++) {
            drawDialogStar(g, sx + i * (size * 2 + 10) + size, cy, size,
                           i < stars ? new Color(255, 215, 0) : new Color(65, 65, 95));
        }
    }

    private void drawDialogStar(Graphics2D g, int cx, int cy, int r, Color col) {
        int[] xs = new int[10], ys = new int[10];
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * i / 5 - Math.PI / 2;
            int rad = (i % 2 == 0) ? r : r / 2;
            xs[i] = cx + (int)(Math.cos(a) * rad);
            ys[i] = cy + (int)(Math.sin(a) * rad);
        }
        g.setColor(col);
        g.fillPolygon(xs, ys, 10);
        g.setColor(col.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(xs, ys, 10);
        g.setStroke(new BasicStroke(1f));
    }

    private JButton makePopupBtn(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isRollover() ? bg.brighter() : bg;
                g2.setColor(new Color(0,0,0,55));
                g2.fillRoundRect(3, 5, getWidth()-4, getHeight()-4, 22, 22);
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-4, 22, 22);
                g2.setColor(new Color(255,255,255,45));
                g2.fillRoundRect(0, 0, getWidth()-2, (getHeight()-4)/2, 22, 22);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()-2 - fm.stringWidth(getText())) / 2;
                int ty = (getHeight()-4 + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(new Color(0,0,0,75));
                g2.drawString(getText(), tx+1, ty+1);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    @Override
    public void dispose() {
        if (gameTimer != null) gameTimer.stop();
        super.dispose();
    }

    // =========================================================================
    //  INNER CLASS: SeaCreature — Musuh Kawaii
    // =========================================================================
    static class SeaCreature {
        static final int W = 210, H = 155;   // lebih besar lagi

        float x, y;
        String  expr;
        boolean isTarget;
        float   speed;
        int     type;
        Color   bodyColor;
        boolean alive = true, exploding = false;
        int     expTimer = 0;
        float   wobblePhase;
        float   wobbleAmp = 7f;          // goyang lebih aktif
        private float idlePhase;
        private float blinkTimer = 0f;   // animasi kedip mata
        private boolean blinking = false;
        private final Random rng = new Random();

        // Palet: semua warna cerah & ceria
        private static final Color[] PALETTE = {
            new Color(255, 110, 145),  // Hot pink
            new Color(255, 168,  44),  // Oranye mango
            new Color(110, 210, 255),  // Biru langit
            new Color(72,  220, 148),  // Hijau mint
            new Color(210,  88, 255),  // Ungu neon
            new Color(255, 232,  55),  // Kuning cerah
            new Color(255, 130, 200),  // Pink muda
            new Color(60,  195, 210),  // Toska
        };

        SeaCreature(int x, String expr, boolean isTarget, float speed, int type) {
            this.x           = x;
            this.y           = -H - 20f - rng.nextInt(60);
            this.expr        = expr;
            this.isTarget    = isTarget;
            this.speed       = speed;
            this.type        = type;
            this.wobblePhase = rng.nextFloat() * 6.28f;
            this.idlePhase   = rng.nextFloat() * 6.28f;
            this.blinkTimer  = 60f + rng.nextFloat() * 120f;
            this.bodyColor   = PALETTE[rng.nextInt(PALETTE.length)];
        }

        void update() {
            if (exploding) { expTimer++; return; }
            y += speed;
            wobblePhase += 0.065f;
            idlePhase   += 0.10f;
            x += (float)(Math.sin(wobblePhase * 0.42f) * 0.7f);

            // Animasi kedip mata
            blinkTimer--;
            if (blinkTimer <= 0) {
                blinking = !blinking;
                blinkTimer = blinking ? 6f : (60f + rng.nextFloat() * 120f);
            }
        }

        void explode() { exploding = true; expTimer = 0; }

        void draw(Graphics2D g) {
            if (!alive) return;
            int dx = (int)x;
            int dy = (int)y + (int)(Math.sin(wobblePhase) * wobbleAmp);
            if (exploding) { drawPop(g, dx + W/2, dy + H/2); return; }

            switch (type % 7) {
                case 0: drawJellyfish(g, dx, dy);   break;
                case 1: drawPufferFish(g, dx, dy);  break;
                case 2: drawOctopus(g, dx, dy);     break;
                case 3: drawDolphin(g, dx, dy);     break;
                case 4: drawClownfish(g, dx, dy);   break;
                case 5: drawSeahorse(g, dx, dy);    break;
                case 6: drawMantaRay(g, dx, dy);    break;
                default: drawJellyfish(g, dx, dy);  break;
            }
            drawExprBubble(g, dx, dy);
        }

        // ── Ubur-ubur ─────────────────────────────────────────────────────────
        private void drawJellyfish(Graphics2D g, int dx, int dy) {
            int cx = dx + W/2, cy = dy + 50;
            int r = 46;  // lebih besar

            // Glow aura luar
            g.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 50));
            g.fillOval(cx - r - 12, cy - r - 8, (r+12)*2, (r+8)*2);

            // Dome glossy
            g.setPaint(new GradientPaint(cx, cy - r, bodyColor.brighter(),
                                         cx, cy + 10, new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 180)));
            g.fillArc(cx - r, cy - r, r*2, (int)(r*1.5), 0, 180);
            g.setPaint(null);
            // Kilap dome
            g.setColor(new Color(255, 255, 255, 90));
            g.fillArc(cx - r/2, cy - r + 4, r, r/2, 30, 120);
            // Border dome
            g.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 200));
            g.setStroke(new BasicStroke(2f));
            g.drawArc(cx - r, cy - r, r*2, (int)(r*1.5), 0, 180);
            g.setStroke(new BasicStroke(1f));

            // Tentakel bergoyang — lebih banyak & panjang
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = -4; i <= 4; i++) {
                float sway = (float)(Math.sin(idlePhase + i * 0.8f) * 10);
                g.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 200));
                g.drawLine(cx + i * 9, cy + 8, cx + i * 11 + (int)sway, cy + 44);
                // Ujung bulat
                g.setColor(bodyColor.brighter());
                g.fillOval(cx + i*11 + (int)sway - 4, cy + 41, 8, 8);
            }
            g.setStroke(new BasicStroke(1f));

            // Mata kawaii BESAR
            drawKawaiiEyes(g, cx - 14, cy - 16, cx + 6, cy - 16, 12);
            // Pipi merah
            g.setColor(new Color(255, 160, 180, 130));
            g.fillOval(cx - 26, cy - 4, 14, 9);
            g.fillOval(cx + 12, cy - 4, 14, 9);
            // Senyum lebar
            g.setColor(new Color(40, 20, 30));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx - 12, cy - 6, 24, 14, 200, 140);
            g.setStroke(new BasicStroke(1f));
        }

        // ── Ikan Buntal ───────────────────────────────────────────────────────
        private void drawPufferFish(Graphics2D g, int dx, int dy) {
            int cx = dx + W/2, cy = dy + 55;
            int r = 40;  // lebih besar

            // Duri bergerak
            g.setColor(bodyColor.darker());
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * 2 * i / 14;
                float r1 = r - 4f, r2 = r + 10f + (float)(Math.sin(idlePhase * 2 + i) * 3);
                g.drawLine(cx + (int)(Math.cos(angle)*r1), cy + (int)(Math.sin(angle)*r1),
                           cx + (int)(Math.cos(angle)*r2), cy + (int)(Math.sin(angle)*r2));
            }
            g.setStroke(new BasicStroke(1f));

            // Shadow
            g.setColor(new Color(0, 0, 0, 40));
            g.fillOval(cx - r + 4, cy - r + 6, r*2, r*2);
            // Badan bulat glossy
            g.setPaint(new GradientPaint(cx - r, cy - r, bodyColor.brighter(), cx + r, cy + r, bodyColor.darker()));
            g.fillOval(cx - r, cy - r, r*2, r*2);
            g.setPaint(null);
            // Kilap
            g.setColor(new Color(255, 255, 255, 80));
            g.fillOval(cx - r/2, cy - r + 6, r/2, r/3);
            // Pola bintik
            g.setColor(new Color(255, 255, 255, 50));
            g.fillOval(cx - 20, cy - 24, 14, 10);
            g.fillOval(cx + 8, cy - 18, 10, 8);
            g.fillOval(cx - 10, cy + 10, 12, 9);

            // Mata kawaii BESAR
            drawKawaiiEyes(g, cx - 16, cy - 10, cx + 6, cy - 10, 13);
            // Pipi
            g.setColor(new Color(255, 160, 160, 130));
            g.fillOval(cx - 30, cy + 2, 14, 9);
            g.fillOval(cx + 16, cy + 2, 14, 9);
            // Mulut O lucu
            g.setColor(new Color(40, 20, 10));
            g.fillOval(cx - 6, cy + 14, 12, 10);
            g.setColor(new Color(255, 200, 200, 180));
            g.fillOval(cx - 3, cy + 16, 6, 5);
        }

        // ── Gurita ────────────────────────────────────────────────────────────
        private void drawOctopus(Graphics2D g, int dx, int dy) {
            int cx = dx + W/2, cy = dy + 44;

            // Tentakel bergoyang — lebih tebal & panjang
            g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = -3; i <= 3; i++) {
                float off = (float)(Math.sin(idlePhase + i * 1.1f) * 12);
                g.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), 210));
                g.drawLine(cx + i * 9, cy + 22, cx + i * 14 + (int)off, cy + 56);
                // Bulatan ujung tentakel
                g.setColor(bodyColor.brighter());
                g.fillOval(cx + i*14 + (int)off - 7, cy + 52, 14, 14);
                // Sucker kecil
                g.setColor(new Color(255, 255, 255, 100));
                g.fillOval(cx + i*14 + (int)off - 3, cy + 56, 6, 6);
            }
            g.setStroke(new BasicStroke(1f));

            // Shadow kepala
            g.setColor(new Color(0, 0, 0, 40));
            g.fillOval(cx - 34, cy - 30 + 5, 68, 56);
            // Kepala besar glossy
            g.setPaint(new GradientPaint(cx, cy - 30, bodyColor.brighter(), cx + 34, cy + 26, bodyColor.darker()));
            g.fillOval(cx - 34, cy - 30, 68, 56);
            g.setPaint(null);
            // Kilap
            g.setColor(new Color(255, 255, 255, 80));
            g.fillOval(cx - 20, cy - 24, 18, 12);

            // Mata kawaii BESAR
            drawKawaiiEyes(g, cx - 16, cy - 8, cx + 6, cy - 8, 13);
            // Pipi merah
            g.setColor(new Color(255, 180, 180, 140));
            g.fillOval(cx - 28, cy + 6, 14, 9);
            g.fillOval(cx + 14, cy + 6, 14, 9);
            // Senyum lebar
            g.setColor(new Color(40, 20, 30));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx - 12, cy + 2, 24, 14, 210, 120);
            g.setStroke(new BasicStroke(1f));
        }

        // ── Bintang Laut kawaii ───────────────────────────────────────────────
        private void drawStarfish(Graphics2D g, int dx, int dy) {
            int cx = dx + W/2, cy = dy + 55;
            int outerR = 46, innerR = 20;

            // Shadow
            g.setColor(new Color(0, 0, 0, 40));
            int[] ssx = new int[10], ssy = new int[10];
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * i / 5 - Math.PI / 2;
                int r = (i % 2 == 0) ? outerR : innerR;
                ssx[i] = cx + (int)(Math.cos(a) * r) + 4;
                ssy[i] = cy + (int)(Math.sin(a) * r) + 5;
            }
            g.fillPolygon(ssx, ssy, 10);

            // 5 lengan bintang laut
            int[] sx = new int[10], sy = new int[10];
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI * i / 5 - Math.PI / 2
                             + (float)(Math.sin(idlePhase + i) * 0.05);
                int r = (i % 2 == 0) ? outerR : innerR;
                sx[i] = cx + (int)(Math.cos(angle) * r);
                sy[i] = cy + (int)(Math.sin(angle) * r);
            }
            g.setPaint(new GradientPaint(cx - outerR, cy - outerR, bodyColor.brighter(),
                                         cx + outerR, cy + outerR, bodyColor.darker()));
            g.fillPolygon(sx, sy, 10);
            g.setPaint(null);
            g.setColor(bodyColor.darker());
            g.setStroke(new BasicStroke(2.5f));
            g.drawPolygon(sx, sy, 10);
            g.setStroke(new BasicStroke(1f));
            // Kilap
            g.setColor(new Color(255, 255, 255, 80));
            g.fillOval(cx - 14, cy - 14, 16, 10);
            // Titik-titik tekstur
            g.setColor(new Color(255, 255, 255, 70));
            for (int i = 0; i < 5; i++) {
                double angle = Math.PI * 2 * i / 5 - Math.PI / 2;
                int rx = cx + (int)(Math.cos(angle) * 28);
                int ry = cy + (int)(Math.sin(angle) * 28);
                g.fillOval(rx - 4, ry - 4, 8, 8);
            }

            // Mata kawaii BESAR
            drawKawaiiEyes(g, cx - 14, cy - 10, cx + 4, cy - 10, 12);
            // Senyum lebar
            g.setColor(new Color(60, 30, 10));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx - 10, cy - 2, 20, 12, 210, 120);
            g.setStroke(new BasicStroke(1f));
            // Pipi
            g.setColor(new Color(255, 160, 160, 120));
            g.fillOval(cx - 26, cy + 2, 12, 8);
            g.fillOval(cx + 14, cy + 2, 12, 8);
        }

        // ── Kepiting ─────────────────────────────────────────────────────────
        private void drawCrab(Graphics2D g, int dx, int dy) {
            int cx = dx + W/2, cy = dy + 60;

            // Kaki bergerak
            g.setColor(bodyColor.darker());
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int[][] legs = {{-38,-5,-56,-18},{-32,-2,-48,-22},{28,-5,46,-18},{24,-2,40,-22}};
            for (int[] leg : legs) {
                float sway = (float)(Math.sin(idlePhase + leg[0]) * 4);
                g.drawLine(cx+leg[0], cy+leg[1], cx+leg[2]+(int)sway, cy+leg[3]);
                g.fillOval(cx+leg[2]+(int)sway-4, cy+leg[3]-4, 8, 8);
            }
            g.setStroke(new BasicStroke(1f));

            // Capit besar
            g.setColor(bodyColor.darker());
            g.fillOval(cx - 60, cy - 26, 28, 20);
            g.fillOval(cx + 32, cy - 26, 28, 20);
            g.setColor(bodyColor);
            g.fillOval(cx - 58, cy - 24, 20, 15);
            g.fillOval(cx + 38, cy - 24, 20, 15);

            // Shadow badan
            g.setColor(new Color(0, 0, 0, 40));
            g.fillOval(cx - 38, cy - 22 + 5, 76, 44);
            // Badan utama glossy
            g.setPaint(new GradientPaint(cx - 38, cy - 22, bodyColor.brighter(),
                                         cx + 38, cy + 22, bodyColor.darker()));
            g.fillOval(cx - 38, cy - 22, 76, 44);
            g.setPaint(null);
            // Kilap
            g.setColor(new Color(255, 255, 255, 80));
            g.fillOval(cx - 22, cy - 16, 18, 10);

            // Tangkai mata
            g.setColor(bodyColor.darker());
            g.setStroke(new BasicStroke(2.5f));
            g.drawLine(cx - 12, cy - 22, cx - 12, cy - 32);
            g.drawLine(cx + 12, cy - 22, cx + 12, cy - 32);
            g.setStroke(new BasicStroke(1f));

            // Mata kawaii BESAR di ujung tangkai
            drawKawaiiEyes(g, cx - 18, cy - 36, cx + 6, cy - 36, 12);
            // Pipi
            g.setColor(new Color(255, 160, 140, 130));
            g.fillOval(cx - 28, cy - 4, 14, 9);
            g.fillOval(cx + 14, cy - 4, 14, 9);
            // Senyum
            g.setColor(new Color(40, 20, 10));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx - 12, cy - 4, 24, 14, 210, 120);
            g.setStroke(new BasicStroke(1f));
        }

        // ── Lumba-lumba kawaii ────────────────────────────────────────────────
        private void drawDolphin(Graphics2D g, int dx, int dy) {
            int cx = dx + W/2, cy = dy + 38;
            // Ekor bergoyang
            float tailSway = (float)(Math.sin(idlePhase * 1.5f) * 8);
            g.setColor(bodyColor.darker());
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx - 28, cy + 8, cx - 42 + (int)tailSway, cy + 18);
            g.drawLine(cx - 28, cy + 8, cx - 42 + (int)tailSway, cy);
            g.setStroke(new BasicStroke(1f));
            // Sirip punggung
            g.setColor(bodyColor.darker());
            g.fillPolygon(new int[]{cx, cx + 8, cx + 14},
                          new int[]{cy - 28, cy - 42, cy - 20}, 3);
            // Sirip samping
            g.setColor(bodyColor);
            g.fillPolygon(new int[]{cx - 5, cx + 12, cx + 5},
                          new int[]{cy + 2, cy + 14, cy + 18}, 3);
            // Badan utama
            g.setPaint(new GradientPaint(cx - 30, cy - 20, bodyColor.brighter(),
                                         cx + 30, cy + 14, bodyColor.darker()));
            g.fillOval(cx - 30, cy - 20, 60, 34);
            g.setPaint(null);
            // Perut putih
            g.setColor(new Color(240, 248, 255, 200));
            g.fillOval(cx - 16, cy - 8, 32, 20);
            // Moncong
            g.setColor(bodyColor);
            g.fillOval(cx + 22, cy - 8, 20, 14);
            g.setColor(new Color(240, 248, 255, 200));
            g.fillOval(cx + 24, cy - 5, 14, 8);
            // Mata
            drawKawaiiEyes(g, cx - 4, cy - 8, cx + 10, cy - 8, 8);
            // Senyum
            g.setColor(new Color(40, 20, 60));
            g.setStroke(new BasicStroke(1.8f));
            g.drawArc(cx + 22, cy - 2, 14, 8, 210, 120);
            g.setStroke(new BasicStroke(1f));
            // Kilap
            g.setColor(new Color(255, 255, 255, 70));
            g.fillOval(cx - 18, cy - 16, 12, 8);
        }

        // ── Ikan Badut (Clownfish) kawaii ─────────────────────────────────────
        private void drawClownfish(Graphics2D g, int dx, int dy) {
            int cx = dx + W/2, cy = dy + 55;
            int bw = 80, bh = 58;   // badan lebih besar

            // Ekor bergoyang
            float tailSway = (float)(Math.sin(idlePhase * 1.8f) * 8);
            g.setColor(new Color(255, 100, 20, 220));
            g.fillPolygon(
                new int[]{cx - bw/2, cx - bw/2 - 30, cx - bw/2 - 30, cx - bw/2},
                new int[]{cy - bh/4, cy - bh/3 + (int)tailSway,
                          cy + bh/3 + (int)tailSway, cy + bh/4},
                4
            );

            // Sirip atas bergoyang
            float finSway = (float)(Math.sin(idlePhase * 1.2f) * 4);
            g.setColor(new Color(255, 130, 30, 210));
            g.fillPolygon(
                new int[]{cx - bw/5, cx - bw/8, cx + bw/5},
                new int[]{cy - bh/2, cy - bh/2 - 28 + (int)finSway, cy - bh/2 + 4},
                3
            );

            // Sirip bawah
            g.setColor(new Color(255, 110, 20, 180));
            g.fillPolygon(
                new int[]{cx - bw/6, cx - bw/8, cx + bw/6},
                new int[]{cy + bh/2, cy + bh/2 + 18, cy + bh/2 + 2},
                3
            );

            // Shadow badan
            g.setColor(new Color(0, 0, 0, 40));
            g.fillOval(cx - bw/2 + 4, cy - bh/2 + 5, bw, bh);

            // Badan oranye glossy
            g.setPaint(new GradientPaint(cx - bw/2, cy - bh/2, new Color(255, 160, 40),
                                         cx + bw/2, cy + bh/2, new Color(220, 70, 10)));
            g.fillOval(cx - bw/2, cy - bh/2, bw, bh);
            g.setPaint(null);

            // Garis putih khas ikan badut (3 garis tebal)
            g.setColor(new Color(255, 255, 255, 230));
            g.setStroke(new BasicStroke(6f));
            g.drawLine(cx + bw/4,  cy - bh/2, cx + bw/4,  cy + bh/2);
            g.drawLine(cx - bw/8,  cy - bh/2, cx - bw/8,  cy + bh/2);
            g.drawLine(cx - bw/3,  cy - bh/3, cx - bw/3,  cy + bh/3);
            g.setStroke(new BasicStroke(1f));

            // Border hitam tipis
            g.setColor(new Color(40, 20, 10, 180));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(cx - bw/2, cy - bh/2, bw, bh);
            g.setStroke(new BasicStroke(1f));

            // Kilap badan
            g.setColor(new Color(255, 255, 255, 70));
            g.fillOval(cx - bw/3, cy - bh/2 + 6, bw/3, bh/4);

            // Mata kawaii BESAR
            drawKawaiiEyes(g, cx + bw/6, cy - bh/6, cx + bw/3, cy - bh/6, 13);

            // Pipi
            g.setColor(new Color(255, 180, 100, 130));
            g.fillOval(cx + bw/8, cy + bh/8, 14, 9);

            // Senyum
            g.setColor(new Color(40, 20, 10));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx + bw/8 - 4, cy + bh/8 - 4, 18, 12, 210, 120);
            g.setStroke(new BasicStroke(1f));
        }

        // ── Kuda Laut kawaii ──────────────────────────────────────────────────
        private void drawSeahorse(Graphics2D g, int dx, int dy) {
            int cx = dx + W/2, cy = dy + 20;
            // Ekor melingkar
            g.setColor(bodyColor.darker());
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float tailCurl = (float)(Math.sin(idlePhase) * 4);
            g.drawArc(cx - 14, cy + 28, 28, 28, 0, 270);
            g.setStroke(new BasicStroke(1f));
            // Badan bersegmen
            for (int i = 0; i < 5; i++) {
                float sway = (float)(Math.sin(idlePhase + i * 0.4f) * 2);
                int bx = cx + (int)sway, by = cy + 10 + i * 10;
                int bw = 22 - i * 2, bh = 12;
                g.setPaint(new GradientPaint(bx - bw/2, by, bodyColor.brighter(),
                                             bx + bw/2, by + bh, bodyColor.darker()));
                g.fillRoundRect(bx - bw/2, by, bw, bh, 6, 6);
                g.setPaint(null);
            }
            // Kepala
            g.setPaint(new GradientPaint(cx - 14, cy - 10, bodyColor.brighter(),
                                         cx + 14, cy + 10, bodyColor.darker()));
            g.fillOval(cx - 14, cy - 10, 28, 24);
            g.setPaint(null);
            // Moncong panjang
            g.setColor(bodyColor);
            g.fillRoundRect(cx + 10, cy - 4, 18, 8, 4, 4);
            // Sirip punggung bergoyang
            g.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(),
                                 bodyColor.getBlue(), 180));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 4; i++) {
                float sf = (float)(Math.sin(idlePhase * 3 + i) * 5);
                g.drawLine(cx - 14, cy + 12 + i * 8, cx - 22 + (int)sf, cy + 8 + i * 8);
            }
            g.setStroke(new BasicStroke(1f));
            // Mata
            drawKawaiiEyes(g, cx - 4, cy - 2, cx + 8, cy - 2, 7);
            // Kilap
            g.setColor(new Color(255, 255, 255, 70));
            g.fillOval(cx - 8, cy - 6, 8, 6);
        }

        // ── Pari Manta kawaii ─────────────────────────────────────────────────
        private void drawMantaRay(Graphics2D g, int dx, int dy) {
            // Diganti jadi kepiting kawaii besar
            int cx = dx + W/2, cy = dy + 48;

            float legSway = (float)(Math.sin(idlePhase * 2.5f) * 6);
            float clawSnap = (float)(Math.sin(idlePhase * 1.8f) * 5);
            Color dark = bodyColor.darker();

            // ── Shadow ────────────────────────────────────────────────────────
            g.setColor(new Color(0, 0, 0, 35));
            g.fillOval(cx - 38, cy + 22, 76, 18);

            // ── Kaki (6 kaki bergerak) ────────────────────────────────────────
            g.setColor(dark);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = -2; i <= 2; i++) {
                if (i == 0) continue;
                float ls = (float)(Math.sin(idlePhase * 2.5f + i * 0.8f) * 7);
                int kx1 = cx + i * 12, ky1 = cy + 14;
                int kx2 = cx + i * 24, ky2 = cy + 28 + (int)ls;
                g.drawLine(kx1, ky1, kx2, ky2);
                g.fillOval(kx2 - 3, ky2 - 3, 6, 6);
            }
            g.setStroke(new BasicStroke(1f));

            // ── Capit kiri ────────────────────────────────────────────────────
            // Lengan
            g.setColor(dark);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx - 22, cy, cx - 40, cy - 10 + (int)clawSnap);
            g.setStroke(new BasicStroke(1f));
            // Capit atas
            g.setPaint(new GradientPaint(cx-55, cy-20, bodyColor.brighter(), cx-38, cy, dark));
            g.fillOval(cx - 55, cy - 22 + (int)clawSnap, 20, 14);
            g.setPaint(null);
            // Capit bawah
            g.setPaint(new GradientPaint(cx-52, cy-8, bodyColor, cx-38, cy+4, dark));
            g.fillOval(cx - 52, cy - 8 - (int)clawSnap, 18, 12);
            g.setPaint(null);
            // Kilap capit
            g.setColor(new Color(255,255,255,60));
            g.fillOval(cx-52, cy-20+(int)clawSnap, 8, 5);

            // ── Capit kanan ───────────────────────────────────────────────────
            g.setColor(dark);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx + 22, cy, cx + 40, cy - 10 - (int)clawSnap);
            g.setStroke(new BasicStroke(1f));
            g.setPaint(new GradientPaint(cx+35, cy-20, bodyColor.brighter(), cx+55, cy, dark));
            g.fillOval(cx + 35, cy - 22 - (int)clawSnap, 20, 14);
            g.setPaint(null);
            g.setPaint(new GradientPaint(cx+34, cy-8, bodyColor, cx+52, cy+4, dark));
            g.fillOval(cx + 34, cy - 8 + (int)clawSnap, 18, 12);
            g.setPaint(null);
            g.setColor(new Color(255,255,255,60));
            g.fillOval(cx+36, cy-20-(int)clawSnap, 8, 5);

            // ── Badan utama glossy ────────────────────────────────────────────
            g.setColor(new Color(0,0,0,40));
            g.fillOval(cx - 32, cy - 18 + 5, 64, 36);
            g.setPaint(new GradientPaint(cx-32, cy-18, bodyColor.brighter(), cx+32, cy+18, dark));
            g.fillOval(cx - 32, cy - 18, 64, 36);
            g.setPaint(null);
            // Pola cangkang
            g.setColor(new Color(255,255,255,45));
            g.fillOval(cx - 18, cy - 14, 28, 14);
            g.setColor(new Color(0,0,0,20));
            g.setStroke(new BasicStroke(1.2f));
            g.drawArc(cx - 28, cy - 14, 56, 28, 20, 140);
            g.drawArc(cx - 20, cy - 10, 40, 20, 25, 130);
            g.setStroke(new BasicStroke(1f));

            // ── Tangkai mata ──────────────────────────────────────────────────
            g.setColor(dark);
            g.setStroke(new BasicStroke(2.5f));
            g.drawLine(cx - 12, cy - 18, cx - 12, cy - 30);
            g.drawLine(cx + 12, cy - 18, cx + 12, cy - 30);
            g.setStroke(new BasicStroke(1f));

            // ── Mata kawaii BESAR ─────────────────────────────────────────────
            drawKawaiiEyes(g, cx - 12, cy - 30, cx + 12, cy - 30, 12);

            // ── Pipi merah ────────────────────────────────────────────────────
            g.setColor(new Color(255, 130, 150, 130));
            g.fillOval(cx - 28, cy - 6, 14, 9);
            g.fillOval(cx + 14, cy - 6, 14, 9);

            // ── Senyum ────────────────────────────────────────────────────────
            g.setColor(new Color(60, 20, 20, 200));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx - 10, cy + 4, 20, 12, 210, 120);
            g.setStroke(new BasicStroke(1f));
        }

        // ── Mata Kawaii BESAR (2 mata bulat dengan kilap + kedip) ────────────
        private void drawKawaiiEyes(Graphics2D g, int lx, int ly,
                                     int rx, int ry, int r) {
            if (blinking) {
                g.setColor(new Color(30, 20, 50));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(lx - r/2, ly - r/4, r, r/2, 0, 180);
                g.drawArc(rx - r/2, ry - r/4, r, r/2, 0, 180);
                g.setStroke(new BasicStroke(1f));
                return;
            }
            // Mata kiri — putih besar
            g.setColor(Color.WHITE);
            g.fillOval(lx - r/2, ly - r/2, r, r);
            // Pupil
            g.setColor(new Color(30, 20, 50));
            g.fillOval(lx - r/4, ly - r/4, r/2 + 2, r/2 + 2);
            // Kilap besar
            g.setColor(new Color(255, 255, 255, 240));
            g.fillOval(lx - r/5, ly - r/3, r/3, r/3);
            // Kilap kecil
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval(lx + r/6, ly + r/8, r/5, r/5);

            // Mata kanan
            g.setColor(Color.WHITE);
            g.fillOval(rx - r/2, ry - r/2, r, r);
            g.setColor(new Color(30, 20, 50));
            g.fillOval(rx - r/4, ry - r/4, r/2 + 2, r/2 + 2);
            g.setColor(new Color(255, 255, 255, 240));
            g.fillOval(rx - r/5, ry - r/3, r/3, r/3);
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval(rx + r/6, ry + r/8, r/5, r/5);
        }

        // ── Label ekspresi matematika: glossy bubble besar ────────────────────
        private void drawExprBubble(Graphics2D g, int dx, int dy) {
            g.setFont(new Font("Arial Black", Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(expr);
            int bw = tw + 28, bh = 32;
            int bx = dx + (W - bw) / 2;
            int by = dy + H - 36;

            // Shadow
            g.setColor(new Color(0, 0, 0, 50));
            g.fillRoundRect(bx + 2, by + 3, bw, bh, 16, 16);

            // Bubble putih glossy
            g.setPaint(new GradientPaint(bx, by, new Color(255, 255, 255, 240),
                                          bx, by + bh, new Color(220, 240, 255, 220)));
            g.fillRoundRect(bx, by, bw, bh, 16, 16);
            g.setPaint(null);

            // Kilap atas
            g.setColor(new Color(255, 255, 255, 180));
            g.fillRoundRect(bx + 3, by + 3, bw - 6, bh / 2, 12, 12);

            // Border biru glow
            g.setColor(new Color(80, 160, 255, 180));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(bx, by, bw, bh, 16, 16);
            g.setStroke(new BasicStroke(1f));

            // Teks
            g.setColor(new Color(10, 50, 130));
            g.setFont(new Font("Arial Black", Font.BOLD, 16));
            g.drawString(expr, bx + 14, by + 21);
        }

        // ── Animasi pop saat ditembak — lebih spektakuler ────────────────────
        private void drawPop(Graphics2D g, int cx, int cy) {
            float prog = Math.min(1f, expTimer / 32f);
            // Ring gelombang kejut
            int ringR = (int)(60 * prog);
            int ringAl = Math.max(0, (int)(200 * (1 - prog)));
            g.setColor(new Color(255, 240, 100, ringAl));
            g.setStroke(new BasicStroke(4f * (1 - prog) + 1f));
            g.drawOval(cx - ringR, cy - ringR, ringR * 2, ringR * 2);
            g.setStroke(new BasicStroke(1f));

            // Gelembung meledak
            int nBubbles = 18;
            for (int i = 0; i < nBubbles; i++) {
                double angle = Math.PI * 2 * i / nBubbles;
                int bx = cx + (int)(Math.cos(angle) * 55 * prog);
                int by = cy + (int)(Math.sin(angle) * 55 * prog);
                int br = Math.max(1, (int)(13 * (1 - prog)));
                int al = Math.max(0, (int)(220 * (1 - prog)));
                // Warna-warni
                Color[] cols = {new Color(130, 228, 255, al), new Color(255, 200, 80, al),
                                new Color(255, 130, 200, al), new Color(100, 255, 180, al)};
                g.setColor(cols[i % cols.length]);
                g.fillOval(bx - br, by - br, br * 2, br * 2);
                g.setColor(new Color(255, 255, 255, al / 2));
                g.drawOval(bx - br, by - br, br * 2, br * 2);
            }
            // Kilat tengah
            int cr = (int)(40 * (1 - prog));
            int al = Math.max(0, (int)(240 * (1 - prog)));
            g.setColor(new Color(255, 255, 200, al));
            g.fillOval(cx - cr, cy - cr, cr * 2, cr * 2);
            // Bintang percikan
            if (prog < 0.7f) {
                g.setColor(new Color(255, 220, 60, al));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    int sx = cx + (int)(Math.cos(a) * 28 * prog);
                    int sy = cy + (int)(Math.sin(a) * 28 * prog);
                    g.fillOval(sx - 5, sy - 5, 10, 10);
                }
            }
        }
    }

    // =========================================================================
    //  INNER CLASS: Bullet — Bintang laut mini bergerak dari cannon ke target
    // =========================================================================
    static class Bullet {
        float x, y;
        float vx, vy;
        static final float SPEED = 22f;   // lebih cepat dari 15f
        boolean alive = true;
        SeaCreature target;
        // Trail
        private final float[] trailX = new float[16];
        private final float[] trailY = new float[16];
        private int trailHead = 0;

        Bullet(float startX, float startY, SeaCreature target) {
            this.x = startX;
            this.y = startY;
            this.target = target;
            Arrays.fill(trailX, startX);
            Arrays.fill(trailY, startY);
            updateDirection();
        }

        private void updateDirection() {
            float tx = target.x + SeaCreature.W / 2f;
            float ty = target.y + SeaCreature.H / 2f;
            float dx = tx - x, dy = ty - y;
            float dist = (float)Math.sqrt(dx * dx + dy * dy);
            if (dist > 0) { vx = dx / dist * SPEED; vy = dy / dist * SPEED; }
        }

        void update() {
            // Homing lebih responsif
            if (target.alive && !target.exploding) {
                float tx = target.x + SeaCreature.W / 2f;
                float ty = target.y + SeaCreature.H / 2f;
                float dx = tx - x, dy = ty - y;
                float dist = (float)Math.sqrt(dx * dx + dy * dy);
                if (dist > 0) {
                    float nx = dx / dist * SPEED;
                    float ny = dy / dist * SPEED;
                    vx = vx * 0.65f + nx * 0.35f;
                    vy = vy * 0.65f + ny * 0.35f;
                }
            }
            trailX[trailHead] = x;
            trailY[trailHead] = y;
            trailHead = (trailHead + 1) % trailX.length;
            x += vx; y += vy;
        }

        boolean hasReachedTarget() {
            if (!target.alive || target.exploding) return true;
            float tx = target.x + SeaCreature.W / 2f;
            float ty = target.y + SeaCreature.H / 2f;
            float dx = x - tx, dy = y - ty;
            return Math.sqrt(dx * dx + dy * dy) < 38;
        }

        void draw(Graphics2D g) {
            if (!alive) return;
            // Trail (ekor cahaya berwarna pink/magenta — warna bintang laut)
            for (int i = 0; i < trailX.length; i++) {
                int idx = (trailHead + i) % trailX.length;
                float ratio = i / (float)trailX.length;
                int al = (int)(ratio * 160);
                int r  = (int)(4 + ratio * 10);   // trail lebih tebal
                g.setColor(new Color(255, 140, 200, al));
                g.fillOval((int)trailX[idx] - r, (int)trailY[idx] - r, r*2, r*2);
            }
            // Bintang laut berputar sebagai peluru
            float spin = (System.currentTimeMillis() % 1000) / 1000f * (float)(Math.PI * 2);
            drawBulletStarfish(g, (int)x, (int)y, spin);
        }

        /** Gambar bintang laut kawaii sebagai peluru */
        private void drawBulletStarfish(Graphics2D g, int cx, int cy, float spin) {
            java.awt.geom.AffineTransform old = g.getTransform();
            g.rotate(spin, cx, cy);

            int outerR = 24, innerR = 10;   // sedikit lebih kecil
            int[] sx = new int[10], sy = new int[10];
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * i / 5 - Math.PI / 2;
                int r = (i % 2 == 0) ? outerR : innerR;
                sx[i] = cx + (int)(Math.cos(a) * r);
                sy[i] = cy + (int)(Math.sin(a) * r);
            }
            // Glow luar
            g.setColor(new Color(255, 180, 220, 80));
            int[] gsx = new int[10], gsy = new int[10];
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * i / 5 - Math.PI / 2;
                int r = (i % 2 == 0) ? outerR + 5 : innerR + 3;
                gsx[i] = cx + (int)(Math.cos(a) * r);
                gsy[i] = cy + (int)(Math.sin(a) * r);
            }
            g.fillPolygon(gsx, gsy, 10);

            // Badan bintang laut — gradient pink cerah
            g.setPaint(new GradientPaint(cx - outerR, cy - outerR,
                                          new Color(255, 120, 180),
                                          cx + outerR, cy + outerR,
                                          new Color(255, 60, 130)));
            g.fillPolygon(sx, sy, 10);
            g.setPaint(null);

            // Border
            g.setColor(new Color(200, 40, 100));
            g.setStroke(new BasicStroke(1.5f));
            g.drawPolygon(sx, sy, 10);
            g.setStroke(new BasicStroke(1f));

            // Titik-titik tekstur
            g.setColor(new Color(255, 220, 240, 180));
            for (int i = 0; i < 5; i++) {
                double a = Math.PI * 2 * i / 5 - Math.PI / 2;
                int rx = cx + (int)(Math.cos(a) * 8);
                int ry = cy + (int)(Math.sin(a) * 8);
                g.fillOval(rx - 2, ry - 2, 4, 4);
            }
            // Mata kecil di tengah
            g.setColor(Color.WHITE);
            g.fillOval(cx - 4, cy - 3, 4, 4);
            g.fillOval(cx + 1, cy - 3, 4, 4);
            g.setColor(new Color(40, 20, 50));
            g.fillOval(cx - 3, cy - 2, 2, 2);
            g.fillOval(cx + 2, cy - 2, 2, 2);

            g.setTransform(old);
        }
    }

    // =========================================================================
    //  INNER CLASS: BubbleFx — Efek percikan gelembung
    // =========================================================================
    static class BubbleFx {
        float x, y, vx, vy, r;
        Color col;
        boolean alive = true;
        int life, maxLife;

        BubbleFx(int cx, int cy, Color col, Random rng) {
            x = cx; y = cy; this.col = col;
            double angle = rng.nextDouble() * Math.PI * 2;
            float spd = 1f + rng.nextFloat() * 3.8f;
            vx = (float)(Math.cos(angle) * spd);
            vy = (float)(Math.sin(angle) * spd) - 0.8f;
            r = 4f + rng.nextFloat() * 9f;
            maxLife = 22 + rng.nextInt(22);
            life = maxLife;
        }

        void update() {
            x += vx; y += vy; vy -= 0.09f; vx *= 0.97f;
            if (--life <= 0) alive = false;
        }

        void draw(Graphics2D g) {
            if (!alive) return;
            float al = life / (float)maxLife;
            int   a  = (int)(al * 200);
            int   s  = Math.max(2, (int)(r * al));
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), a));
            g.fillOval((int)(x - s), (int)(y - s), s*2, s*2);
            g.setColor(new Color(255, 255, 255, a / 2));
            g.drawOval((int)(x - s), (int)(y - s), s*2, s*2);
        }
    }

    // =========================================================================
    //  INNER CLASS: FloatLabel — Teks +pts / -❤ mengambang
    // =========================================================================
    static class FloatLabel {
        float x, y; String text; Color col;
        boolean alive = true; int life = 80;   // lebih lama dari 64
        FloatLabel(String t, int x, int y, Color c) { text=t; this.x=x; this.y=y; col=c; }
        void update() { y -= 1.6f; if (--life <= 0) alive = false; }
        void draw(Graphics2D g) {
            if (!alive) return;
            float al = life / 80f;
            g.setFont(new Font("Arial Black", Font.BOLD, 22));  // lebih besar
            FontMetrics fm = g.getFontMetrics();
            int tx = (int)x - fm.stringWidth(text) / 2;
            // Shadow
            g.setColor(new Color(0, 0, 0, (int)(al * 100)));
            g.drawString(text, tx + 2, (int)y + 2);
            // Teks utama
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(al * 255)));
            g.drawString(text, tx, (int)y);
            // Glow
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(al * 60)));
            g.setFont(new Font("Arial Black", Font.BOLD, 26));
            g.drawString(text, tx - 2, (int)y + 2);
        }
    }

    // =========================================================================
    //  INNER CLASS: BgAnimal — Hewan laut dekoratif latar belakang
    // =========================================================================
    abstract static class BgAnimal {
        float x, y; int panelW, panelH; Random rng;
        int maxY = Integer.MAX_VALUE;  // batas bawah — hewan tidak masuk area pasir
        BgAnimal(int pw, int ph, Random r) { panelW=pw; panelH=ph; rng=r; }
        abstract void update(); abstract void draw(Graphics2D g);
    }

    // ── Ikan kecil warna-warni KAWAII ─────────────────────────────────────────
    static class BgFish extends BgAnimal {
        float speed; boolean right; Color col, colFin, colBelly; int sz;
        float wobble, wSpd, baseY, finPhase, blinkT;
        boolean blinking;
        private static final Color[] PAL = {
            new Color(255, 100, 60),   // oranye coral
            new Color(80,  200, 255),  // biru langit
            new Color(255, 210, 30),   // kuning cerah
            new Color(255, 80,  160),  // pink hot
            new Color(60,  220, 130),  // hijau mint
            new Color(200, 80,  255),  // ungu neon
            new Color(255, 160, 60),   // oranye mango
            new Color(60,  210, 220),  // toska
        };
        BgFish(int pw, int ph, Random r) { super(pw, ph, r); spawn(true); }
        void spawn(boolean init) {
            right   = rng.nextBoolean();
            speed   = 0.8f + rng.nextFloat() * 1.4f;
            sz      = 22 + rng.nextInt(26);
            int fishMax = (maxY == Integer.MAX_VALUE) ? panelH - 240 : maxY - sz - 20;
            baseY   = 80 + rng.nextFloat() * Math.max(10, fishMax - 80);
            wSpd    = 0.04f + rng.nextFloat() * 0.03f;
            wobble  = rng.nextFloat() * 6.28f;
            finPhase = rng.nextFloat() * 6.28f;
            blinkT  = 80f + rng.nextFloat() * 140f;
            blinking = false;
            col     = PAL[rng.nextInt(PAL.length)];
            // Warna sirip lebih gelap, perut lebih terang
            colFin  = col.darker();
            colBelly = new Color(
                Math.min(255, col.getRed() + 80),
                Math.min(255, col.getGreen() + 80),
                Math.min(255, col.getBlue() + 80), 200);
            x = init ? rng.nextFloat() * panelW : (right ? -sz * 3 : panelW + sz);
        }
        @Override void update() {
            wobble   += wSpd;
            finPhase += 0.15f;
            y = baseY + (float)(Math.sin(wobble) * 12);
            x += right ? speed : -speed;
            // Kedip mata
            blinkT--;
            if (blinkT <= 0) {
                blinking = !blinking;
                blinkT = blinking ? 5f : (80f + rng.nextFloat() * 140f);
            }
            if (right && x > panelW + sz * 3) spawn(false);
            if (!right && x < -sz * 3) spawn(false);
        }
        @Override void draw(Graphics2D g) {
            int cx = (int)x, cy = (int)y;
            int d = right ? 1 : -1;
            int alpha = col.getAlpha();

            // ── Ekor bergoyang ────────────────────────────────────────────────
            float tailSway = (float)(Math.sin(finPhase * 1.8f) * sz * 0.18f);
            g.setColor(new Color(colFin.getRed(), colFin.getGreen(), colFin.getBlue(), alpha));
            g.fillPolygon(
                new int[]{cx - d*sz/2, cx - d*(sz/2 + sz/3), cx - d*(sz/2 + sz/3), cx - d*sz/2},
                new int[]{cy - sz/5, cy - sz/3 + (int)tailSway, cy + sz/3 + (int)tailSway, cy + sz/5},
                4
            );

            // ── Sirip atas bergoyang ──────────────────────────────────────────
            float finSway = (float)(Math.sin(finPhase) * 3);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha - 30));
            g.fillPolygon(
                new int[]{cx - d*sz/5, cx - d*sz/5, cx + d*sz/5},
                new int[]{cy - sz/4, cy - sz/4 - sz/3 + (int)finSway, cy - sz/4},
                3
            );

            // ── Badan utama — oval glossy ─────────────────────────────────────
            // Shadow
            g.setColor(new Color(0, 0, 0, 30));
            g.fillOval(cx - sz/2 + 2, cy - sz/4 + 3, sz, sz/2);
            // Badan
            g.setPaint(new GradientPaint(
                cx - sz/2, cy - sz/4, col,
                cx + sz/2, cy + sz/4, colFin));
            g.fillOval(cx - sz/2, cy - sz/4, sz, sz/2);
            g.setPaint(null);
            // Perut putih/terang
            g.setColor(new Color(colBelly.getRed(), colBelly.getGreen(), colBelly.getBlue(), 160));
            g.fillOval(cx - sz/4, cy - sz/8, sz/2, sz/4);
            // Kilap atas
            g.setColor(new Color(255, 255, 255, 90));
            g.fillOval(cx - sz/4, cy - sz/4 + 2, sz/3, sz/6);

            // ── Sirip dada kecil ──────────────────────────────────────────────
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha - 40));
            g.fillOval(cx + d*2, cy, sz/5, sz/6);

            // ── Mata kawaii ───────────────────────────────────────────────────
            int ex = cx + d * (sz/4 - 2), ey = cy - sz/8;
            int er = Math.max(3, sz / 7);
            g.setColor(Color.WHITE);
            g.fillOval(ex - er, ey - er, er*2, er*2);
            if (blinking) {
                // Mata tertutup — garis lengkung
                g.setColor(new Color(30, 20, 50, alpha));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(ex - er, ey - er/2, er*2, er, 0, 180);
                g.setStroke(new BasicStroke(1f));
            } else {
                // Pupil
                g.setColor(new Color(20, 15, 40, alpha));
                g.fillOval(ex - er/2, ey - er/2, er, er);
                // Kilap mata
                g.setColor(new Color(255, 255, 255, 220));
                g.fillOval(ex - er/4, ey - er/2, er/3, er/3);
            }

            // ── Pipi merah kawaii ─────────────────────────────────────────────
            g.setColor(new Color(255, 160, 160, 80));
            g.fillOval(ex - er - 4, ey + er/2, er + 2, er/2 + 1);

            // ── Mulut kecil ───────────────────────────────────────────────────
            g.setColor(new Color(40, 20, 30, alpha));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx + d*(sz/2 - sz/5), cy - 2, sz/6, sz/8, 200, 120);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // ── Ubur-ubur latar KAWAII ─────────────────────────────────────────────────
    static class BgJellyfish extends BgAnimal {
        float wobble, phase, pulsePhase; Color col; int sz;
        BgJellyfish(int pw, int ph, Random r) {
            super(pw, ph, r);
            x = rng.nextFloat() * pw;
            y = 80 + rng.nextFloat() * (ph - 260);
            phase = rng.nextFloat() * 6.28f;
            pulsePhase = rng.nextFloat() * 6.28f;
            sz = 20 + rng.nextInt(18);
            Color[] cs = {
                new Color(220, 80,  255, 160),
                new Color(100, 180, 255, 160),
                new Color(255, 120, 200, 160),
                new Color(80,  230, 210, 160),
                new Color(255, 180, 80,  160),
            };
            col = cs[rng.nextInt(cs.length)];
        }
        @Override void update() {
            phase      += 0.022f;
            wobble     += 0.05f;
            pulsePhase += 0.08f;
            y -= 0.18f;
            x += (float)(Math.sin(phase) * 0.35f);
            if (y < -80) { y = panelH + 40; x = rng.nextFloat() * panelW; }
        }
        @Override void draw(Graphics2D g) {
            int cx = (int)x, cy = (int)y;
            float pulse = (float)(Math.sin(pulsePhase) * 0.08 + 1.0);
            int pw = (int)(sz * 2 * pulse), ph2 = (int)(sz * 1.4 * pulse);

            // Glow aura
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 40));
            g.fillOval(cx - pw/2 - 6, cy - ph2/2 - 6, pw + 12, ph2 + 12);

            // Dome — gradient glossy
            g.setPaint(new GradientPaint(cx, cy - ph2/2, col.brighter(),
                                          cx, cy + ph2/2, new Color(col.getRed(), col.getGreen(), col.getBlue(), 100)));
            g.fillArc(cx - pw/2, cy - ph2/2, pw, ph2, 0, 180);
            g.setPaint(null);

            // Kilap dome
            g.setColor(new Color(255, 255, 255, 80));
            g.fillArc(cx - pw/4, cy - ph2/2 + 2, pw/3, ph2/3, 30, 120);

            // Border dome
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 180));
            g.setStroke(new BasicStroke(1.5f));
            g.drawArc(cx - pw/2, cy - ph2/2, pw, ph2, 0, 180);
            g.setStroke(new BasicStroke(1f));

            // Tentakel bergoyang
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int nTent = 5;
            for (int i = 0; i < nTent; i++) {
                float tx = cx - pw/2 + (float)(i + 0.5f) * pw / nTent;
                float sw = (float)(Math.sin(wobble + i * 1.1f) * 7);
                g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 180));
                g.drawLine((int)tx, cy, (int)(tx + sw), cy + sz);
                // Ujung tentakel bulat
                g.setColor(col.brighter());
                g.fillOval((int)(tx + sw) - 3, cy + sz - 3, 6, 6);
            }
            g.setStroke(new BasicStroke(1f));

            // Mata kawaii
            int eyeY = cy - ph2/6;
            int er = Math.max(3, sz/5);
            // Mata kiri
            g.setColor(Color.WHITE);
            g.fillOval(cx - er*2, eyeY - er, er*2, er*2);
            g.setColor(new Color(30, 20, 50));
            g.fillOval(cx - er - er/2, eyeY - er/2, er, er);
            g.setColor(new Color(255, 255, 255, 220));
            g.fillOval(cx - er - er/4, eyeY - er/2, er/3, er/3);
            // Mata kanan
            g.setColor(Color.WHITE);
            g.fillOval(cx + 2, eyeY - er, er*2, er*2);
            g.setColor(new Color(30, 20, 50));
            g.fillOval(cx + er/2, eyeY - er/2, er, er);
            g.setColor(new Color(255, 255, 255, 220));
            g.fillOval(cx + er/2 + 2, eyeY - er/2, er/3, er/3);

            // Senyum kecil
            g.setColor(new Color(40, 20, 50, 200));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx - er, eyeY + er/2, er*2, er, 200, 140);
            g.setStroke(new BasicStroke(1f));

            // Pipi
            g.setColor(new Color(255, 160, 180, 100));
            g.fillOval(cx - er*2 - 2, eyeY + er/2, er + 2, er/2 + 1);
            g.fillOval(cx + er + 2, eyeY + er/2, er + 2, er/2 + 1);
        }
    }

    // ── Kura-kura latar KAWAII ─────────────────────────────────────────────────
    static class BgTurtle extends BgAnimal {
        float angle, speed, flipperPhase; Color shellCol, skinCol;
        BgTurtle(int pw, int ph, Random r) {
            super(pw, ph, r);
            x = -80; y = 100 + rng.nextFloat() * (ph - 310);
            speed = 0.5f + rng.nextFloat() * 0.5f;
            flipperPhase = rng.nextFloat() * 6.28f;
            // Warna cangkang random
            Color[] shells = {
                new Color(50, 160, 70, 180),
                new Color(80, 180, 50, 180),
                new Color(40, 140, 100, 180),
            };
            shellCol = shells[rng.nextInt(shells.length)];
            skinCol  = new Color(70, 180, 90, 180);
        }
        @Override void update() {
            x += speed;
            y += (float)(Math.sin(angle) * 0.35f);
            angle += 0.03f;
            flipperPhase += 0.08f;
            if (x > panelW + 100) { x = -80; y = 100 + rng.nextFloat() * (panelH - 310); }
        }
        @Override void draw(Graphics2D g) {
            int cx = (int)x, cy = (int)y;
            float fp = (float)(Math.sin(flipperPhase) * 8);

            // ── Sirip/kaki bergerak ───────────────────────────────────────────
            g.setColor(skinCol);
            // Kaki depan kiri
            g.fillOval(cx - 28, cy - 8 + (int)fp, 14, 9);
            // Kaki depan kanan
            g.fillOval(cx + 14, cy - 8 - (int)fp, 14, 9);
            // Kaki belakang kiri
            g.fillOval(cx - 24, cy + 6 - (int)fp, 12, 8);
            // Kaki belakang kanan
            g.fillOval(cx + 12, cy + 6 + (int)fp, 12, 8);

            // ── Cangkang — glossy dome ────────────────────────────────────────
            // Shadow
            g.setColor(new Color(0, 0, 0, 40));
            g.fillOval(cx - 22, cy - 12 + 4, 44, 28);
            // Cangkang utama
            g.setPaint(new GradientPaint(cx - 22, cy - 12, shellCol.brighter(),
                                          cx + 22, cy + 16, shellCol.darker()));
            g.fillOval(cx - 22, cy - 12, 44, 28);
            g.setPaint(null);
            // Kilap cangkang
            g.setColor(new Color(255, 255, 255, 70));
            g.fillOval(cx - 10, cy - 10, 14, 8);
            // Pola cangkang hexagonal
            g.setColor(new Color(0, 80, 30, 100));
            g.setStroke(new BasicStroke(1.2f));
            g.drawOval(cx - 12, cy - 7, 24, 16);
            g.drawLine(cx, cy - 12, cx, cy + 16);
            g.drawLine(cx - 14, cy - 4, cx + 14, cy + 8);
            g.drawLine(cx - 14, cy + 8, cx + 14, cy - 4);
            g.setStroke(new BasicStroke(1f));

            // ── Kepala kawaii ─────────────────────────────────────────────────
            g.setColor(skinCol);
            g.fillOval(cx + 20, cy - 7, 18, 14);
            // Kilap kepala
            g.setColor(new Color(255, 255, 255, 60));
            g.fillOval(cx + 22, cy - 5, 7, 5);
            // Mata kawaii
            g.setColor(Color.WHITE);
            g.fillOval(cx + 28, cy - 5, 7, 7);
            g.setColor(new Color(20, 20, 40));
            g.fillOval(cx + 30, cy - 3, 4, 4);
            g.setColor(new Color(255, 255, 255, 220));
            g.fillOval(cx + 31, cy - 4, 2, 2);
            // Senyum
            g.setColor(new Color(30, 80, 30, 200));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx + 24, cy + 2, 8, 5, 210, 120);
            g.setStroke(new BasicStroke(1f));
            // Pipi
            g.setColor(new Color(255, 160, 160, 100));
            g.fillOval(cx + 26, cy + 2, 5, 3);
        }
    }

    // =========================================================================
    //  INNER CLASS: BgDolphin — Lumba-lumba latar KAWAII
    // =========================================================================
    static class BgDolphin extends BgAnimal {
        float speed; boolean right; float phase; Color col, colDark;
        BgDolphin(int pw, int ph, Random r) {
            super(pw, ph, r);
            right = rng.nextBoolean();
            speed = 1.4f + rng.nextFloat() * 1.0f;
            x = right ? -100 : pw + 100;
            y = 80 + rng.nextFloat() * (ph * 0.32f);
            phase = rng.nextFloat() * 6.28f;
            col     = new Color(120, 200, 240, 200);
            colDark = new Color(70, 140, 200, 180);
        }
        @Override void update() {
            phase += 0.045f;
            x += right ? speed : -speed;
            y += (float)(Math.sin(phase) * 0.7f);
            if (right && x > panelW + 120) { x = -100; y = 80 + rng.nextFloat() * (panelH * 0.32f); }
            if (!right && x < -120) { x = panelW + 100; y = 80 + rng.nextFloat() * (panelH * 0.32f); }
        }
        @Override void draw(Graphics2D g) {
            int cx = (int)x, cy = (int)y;
            int d = right ? 1 : -1;
            float tailSway = (float)(Math.sin(phase * 2.2f) * 8);
            // Ekor
            g.setColor(colDark);
            g.fillPolygon(
                new int[]{cx-d*26, cx-d*44, cx-d*38, cx-d*26},
                new int[]{cy-8, cy-14+(int)tailSway, cy+14+(int)tailSway, cy+8}, 4);
            // Sirip punggung
            g.fillPolygon(new int[]{cx+d*2, cx+d*10, cx+d*18},
                          new int[]{cy-20, cy-36, cy-16}, 3);
            // Sirip dada
            g.fillPolygon(new int[]{cx+d*4, cx+d*16, cx+d*8},
                          new int[]{cy+4, cy+16, cy+18}, 3);
            // Shadow badan
            g.setColor(new Color(0,0,0,35));
            g.fillOval(cx-28, cy-14+4, 56, 28);
            // Badan glossy
            g.setPaint(new GradientPaint(cx-28, cy-14, col.brighter(), cx+28, cy+14, colDark));
            g.fillOval(cx-28, cy-14, 56, 28);
            g.setPaint(null);
            // Perut putih
            g.setColor(new Color(230, 248, 255, 180));
            g.fillOval(cx-14, cy-5, 28, 16);
            // Kilap
            g.setColor(new Color(255,255,255,80));
            g.fillOval(cx-16, cy-12, 18, 8);
            // Moncong
            g.setColor(col);
            g.fillOval(cx+d*24, cy-7, 16, 12);
            g.setColor(new Color(230,248,255,160));
            g.fillOval(cx+d*26, cy-5, 10, 7);
            // Mata kawaii
            int ex = cx+d*16, ey = cy-4;
            g.setColor(Color.WHITE);
            g.fillOval(ex-5, ey-5, 10, 10);
            g.setColor(new Color(20,20,40));
            g.fillOval(ex-3, ey-3, 6, 6);
            g.setColor(new Color(255,255,255,220));
            g.fillOval(ex-1, ey-3, 3, 3);
            // Pipi
            g.setColor(new Color(255,160,200,100));
            g.fillOval(ex+d*2, ey+3, 7, 4);
            // Senyum
            g.setColor(new Color(30,60,100,200));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx+d*22, cy+1, 10, 6, 210, 120);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // =========================================================================
    //  INNER CLASS: BgCrabWalk — Kepiting berjalan di dasar laut KAWAII
    // =========================================================================
    static class BgCrabWalk extends BgAnimal {
        float speed; boolean right; Color col, colDark; float legPhase;
        BgCrabWalk(int pw, int ph, Random r) {
            super(pw, ph, r);
            right = rng.nextBoolean();
            speed = 0.5f + rng.nextFloat() * 0.6f;
            x = right ? -50 : pw + 50;
            y = ph * 0.72f + rng.nextFloat() * (ph * 0.06f);
            legPhase = rng.nextFloat() * 6.28f;
            Color[] cols = {new Color(230,80,50,220), new Color(255,120,40,220),
                            new Color(200,60,100,220), new Color(240,100,60,220)};
            col = cols[rng.nextInt(cols.length)];
            colDark = col.darker();
        }
        @Override void update() {
            legPhase += 0.14f;
            x += right ? speed : -speed;
            if (right && x > panelW+60) { x=-50; y=panelH*0.72f+rng.nextFloat()*(panelH*0.06f); }
            if (!right && x < -60) { x=panelW+50; y=panelH*0.72f+rng.nextFloat()*(panelH*0.06f); }
        }
        @Override void draw(Graphics2D g) {
            int cx = (int)x, cy = (int)y;
            // Kaki bergerak
            g.setColor(colDark);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = -2; i <= 2; i++) {
                if (i == 0) continue;
                float ls = (float)(Math.sin(legPhase + i*0.9f) * 5);
                g.drawLine(cx+i*8, cy+5, cx+i*16, cy+14+(int)ls);
                g.fillOval(cx+i*16-2, cy+12+(int)ls, 4, 4);
            }
            g.setStroke(new BasicStroke(1f));
            // Capit
            float cs = (float)(Math.sin(legPhase*0.5f)*3);
            g.setColor(colDark);
            g.fillOval(cx-30, cy-12+(int)cs, 16, 11);
            g.setColor(col);
            g.fillOval(cx-28, cy-10+(int)cs, 11, 8);
            g.setColor(colDark);
            g.fillOval(cx+14, cy-12-(int)cs, 16, 11);
            g.setColor(col);
            g.fillOval(cx+16, cy-10-(int)cs, 11, 8);
            // Shadow badan
            g.setColor(new Color(0,0,0,40));
            g.fillOval(cx-20, cy-12+4, 40, 24);
            // Badan glossy
            g.setPaint(new GradientPaint(cx-20, cy-12, col.brighter(), cx+20, cy+12, colDark));
            g.fillOval(cx-20, cy-12, 40, 24);
            g.setPaint(null);
            g.setColor(new Color(255,255,255,80));
            g.fillOval(cx-10, cy-9, 14, 7);
            // Tangkai mata
            g.setColor(colDark);
            g.setStroke(new BasicStroke(2f));
            g.drawLine(cx-8, cy-12, cx-8, cy-20);
            g.drawLine(cx+8, cy-12, cx+8, cy-20);
            g.setStroke(new BasicStroke(1f));
            // Mata kawaii
            g.setColor(Color.WHITE);
            g.fillOval(cx-12, cy-26, 8, 8);
            g.setColor(new Color(20,20,40));
            g.fillOval(cx-10, cy-24, 5, 5);
            g.setColor(new Color(255,255,255,220));
            g.fillOval(cx-9, cy-24, 2, 2);
            g.setColor(Color.WHITE);
            g.fillOval(cx+4, cy-26, 8, 8);
            g.setColor(new Color(20,20,40));
            g.fillOval(cx+6, cy-24, 5, 5);
            g.setColor(new Color(255,255,255,220));
            g.fillOval(cx+7, cy-24, 2, 2);
            // Senyum + pipi
            g.setColor(new Color(80,20,10,200));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx-6, cy-2, 12, 8, 210, 120);
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(255,160,140,100));
            g.fillOval(cx-14, cy+2, 8, 5);
            g.fillOval(cx+6, cy+2, 8, 5);
        }
    }

    // =========================================================================
    //  INNER CLASS: BgSeahorse — Kuda laut latar KAWAII
    // =========================================================================
    static class BgSeahorse extends BgAnimal {
        float phase, driftX, finPhase; Color col, colDark;
        BgSeahorse(int pw, int ph, Random r) {
            super(pw, ph, r);
            x = 40 + rng.nextFloat() * (pw - 80);
            y = 100 + rng.nextFloat() * (ph * 0.45f);
            phase = rng.nextFloat() * 6.28f;
            finPhase = rng.nextFloat() * 6.28f;
            driftX = (rng.nextBoolean() ? 1 : -1) * (0.12f + rng.nextFloat() * 0.18f);
            Color[] cs = {new Color(255,160,60,200), new Color(200,80,240,200),
                          new Color(60,210,190,200), new Color(255,100,160,200),
                          new Color(100,200,255,200)};
            col = cs[rng.nextInt(cs.length)];
            colDark = col.darker();
        }
        @Override void update() {
            phase += 0.028f; finPhase += 0.18f;
            x += driftX + (float)(Math.sin(phase*0.7f)*0.25f);
            y += (float)(Math.sin(phase)*0.35f);
            if (x < -40 || x > panelW+40) driftX = -driftX;
            if (y < 60) y = 60;
            if (y > panelH*0.65f) y = panelH*0.65f;
        }
        @Override void draw(Graphics2D g) {
            int cx = (int)x, cy = (int)y;
            // Ekor melingkar
            g.setColor(colDark);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx-10, cy+18, 20, 20, 0, 270);
            g.setStroke(new BasicStroke(1f));
            // Badan bersegmen glossy
            for (int i = 0; i < 5; i++) {
                float sw = (float)(Math.sin(phase+i*0.4f)*2);
                int bw = 18-i*2, bh = 10;
                int bx = cx+(int)sw-bw/2, by = cy+8+i*10;
                g.setColor(new Color(0,0,0,25));
                g.fillRoundRect(bx+1, by+2, bw, bh, 5, 5);
                g.setPaint(new GradientPaint(bx, by, col, bx+bw, by+bh, colDark));
                g.fillRoundRect(bx, by, bw, bh, 5, 5);
                g.setPaint(null);
                g.setColor(new Color(255,255,255,60));
                g.fillRoundRect(bx+2, by+1, bw/2, bh/3, 3, 3);
            }
            // Sirip punggung bergerak
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 160));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 4; i++) {
                float sf = (float)(Math.sin(finPhase+i*0.8f)*7);
                g.drawLine(cx-9, cy+10+i*9, cx-18+(int)sf, cy+6+i*9);
            }
            g.setStroke(new BasicStroke(1f));
            // Kepala glossy
            g.setColor(new Color(0,0,0,30));
            g.fillOval(cx-11, cy-8+3, 22, 18);
            g.setPaint(new GradientPaint(cx-11, cy-8, col.brighter(), cx+11, cy+10, col));
            g.fillOval(cx-11, cy-8, 22, 18);
            g.setPaint(null);
            g.setColor(new Color(255,255,255,80));
            g.fillOval(cx-6, cy-6, 8, 5);
            // Moncong
            g.setColor(col);
            g.fillRoundRect(cx+9, cy-3, 14, 7, 4, 4);
            // Mata kawaii
            g.setColor(Color.WHITE);
            g.fillOval(cx-3, cy-4, 8, 8);
            g.setColor(new Color(20,20,40));
            g.fillOval(cx-1, cy-2, 5, 5);
            g.setColor(new Color(255,255,255,220));
            g.fillOval(cx, cy-2, 2, 2);
            // Pipi + senyum
            g.setColor(new Color(255,160,180,120));
            g.fillOval(cx+4, cy+2, 6, 4);
            g.setColor(new Color(60,20,40,200));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx+2, cy+4, 8, 5, 210, 120);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // =========================================================================
    //  INNER CLASS: BgManta — Pari manta latar KAWAII
    // =========================================================================
    static class BgManta extends BgAnimal {
        float speed; boolean right; float phase, blinkT; Color col, colBelly;
        boolean blinking;
        BgManta(int pw, int ph, Random r) {
            super(pw, ph, r);
            right = rng.nextBoolean();
            speed = 0.5f + rng.nextFloat() * 0.4f;
            x = right ? -150 : pw + 150;
            y = 100 + rng.nextFloat() * (ph * 0.30f);
            phase = rng.nextFloat() * 6.28f;
            blinkT = 80f + rng.nextFloat() * 120f;
            // Warna cerah kawaii
            Color[] cs = {
                new Color(100, 140, 255, 210),  // biru cerah
                new Color(140,  80, 220, 210),  // ungu
                new Color( 60, 180, 220, 210),  // toska
                new Color(180,  80, 200, 210),  // pink-ungu
            };
            col = cs[rng.nextInt(cs.length)];
            colBelly = new Color(220, 240, 255, 180);
        }
        @Override void update() {
            phase += 0.032f;
            x += right ? speed : -speed;
            y += (float)(Math.sin(phase) * 0.55f);
            blinkT--;
            if (blinkT <= 0) { blinking = !blinking; blinkT = blinking ? 6 : 80 + (float)(Math.random()*120); }
            if (right && x > panelW + 160) { x = -150; y = 100 + (float)(Math.random() * panelH * 0.30f); }
            if (!right && x < -160)        { x = panelW + 150; y = 100 + (float)(Math.random() * panelH * 0.30f); }
        }
        @Override void draw(Graphics2D g) {
            int cx = (int)x, cy = (int)y;
            int d = right ? 1 : -1;

            // Animasi kepak sayap
            float wf = (float)(Math.sin(phase * 1.5f) * 18);

            // ── Shadow ────────────────────────────────────────────────────────
            g.setColor(new Color(0, 0, 0, 25));
            int[] shx = {cx - d*70+4, cx - d*12+4, cx+4, cx + d*12+4, cx + d*70+4};
            int[] shy = {cy+(int)wf+6, cy-16+6, cy+8+6, cy-16+6, cy+(int)wf+6};
            g.fillPolygon(shx, shy, 5);

            // ── Sayap — bentuk lebih bulat & kawaii ───────────────────────────
            // Sayap kiri
            int[] lwx = {cx - d*70, cx - d*12, cx, cx - d*8};
            int[] lwy = {cy+(int)wf, cy-16, cy+8, cy+20};
            g.setPaint(new GradientPaint(cx-d*70, cy, col, cx, cy, col.darker()));
            g.fillPolygon(lwx, lwy, 4);
            // Sayap kanan
            int[] rwx = {cx + d*70, cx + d*12, cx, cx + d*8};
            int[] rwy = {cy+(int)wf, cy-16, cy+8, cy+20};
            g.setPaint(new GradientPaint(cx+d*70, cy, col, cx, cy, col.darker()));
            g.fillPolygon(rwx, rwy, 4);
            g.setPaint(null);

            // Kilap sayap
            g.setColor(new Color(255, 255, 255, 50));
            g.fillPolygon(
                new int[]{cx-d*40, cx-d*12, cx-d*28},
                new int[]{cy+(int)(wf*0.4f), cy-10, cy-4}, 3);

            // Pola bintik di sayap (kawaii)
            g.setColor(new Color(255, 255, 255, 55));
            g.fillOval(cx-d*45, cy+(int)(wf*0.6f)-4, 10, 7);
            g.fillOval(cx-d*28, cy+(int)(wf*0.3f)-6, 8, 6);
            g.fillOval(cx+d*30, cy+(int)(wf*0.5f)-4, 9, 6);

            // ── Perut putih (belly) ───────────────────────────────────────────
            g.setPaint(new GradientPaint(cx-18, cy-4, colBelly, cx+18, cy+14, new Color(200,230,255,120)));
            g.fillOval(cx-18, cy-4, 36, 22);
            g.setPaint(null);

            // ── Badan utama glossy ────────────────────────────────────────────
            g.setPaint(new GradientPaint(cx-22, cy-12, col.brighter(), cx+22, cy+12, col.darker()));
            g.fillOval(cx-22, cy-12, 44, 24);
            g.setPaint(null);
            // Kilap badan
            g.setColor(new Color(255, 255, 255, 80));
            g.fillOval(cx-12, cy-10, 16, 8);

            // ── Ekor ──────────────────────────────────────────────────────────
            g.setColor(col.darker());
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx, cy+10, cx-d*4, cy+28);
            g.drawLine(cx-d*4, cy+28, cx-d*12, cy+36);
            g.drawLine(cx-d*4, cy+28, cx+d*4, cy+36);
            g.setStroke(new BasicStroke(1f));

            // ── Tanduk kepala (cephalic fins) kawaii ──────────────────────────
            g.setColor(col.brighter());
            // Tanduk kiri
            g.fillPolygon(
                new int[]{cx+d*16, cx+d*24, cx+d*18},
                new int[]{cy-10, cy-22, cy-4}, 3);
            // Tanduk kanan
            g.fillPolygon(
                new int[]{cx+d*8, cx+d*16, cx+d*10},
                new int[]{cy-10, cy-20, cy-4}, 3);

            // ── Mata kawaii BESAR ─────────────────────────────────────────────
            int ex = cx + d*10, ey = cy - 3;
            // Putih mata
            g.setColor(Color.WHITE);
            g.fillOval(ex-7, ey-7, 14, 14);
            // Pupil
            g.setColor(new Color(20, 20, 50));
            g.fillOval(ex-4, ey-4, 9, 9);
            // Kilap mata
            g.setColor(new Color(255, 255, 255, 240));
            g.fillOval(ex-2, ey-3, 4, 4);
            g.fillOval(ex+2, ey+1, 2, 2);
            // Kedip
            if (blinking) {
                g.setColor(col.darker());
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(ex-7, ey-7, 14, 14, 0, 180);
                g.setStroke(new BasicStroke(1f));
            }

            // ── Pipi merah kawaii ─────────────────────────────────────────────
            g.setColor(new Color(255, 140, 170, 120));
            g.fillOval(ex+d*4, ey+3, 10, 6);

            // ── Senyum lebar ──────────────────────────────────────────────────
            g.setColor(new Color(30, 20, 60, 200));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx+d*6, cy+5, 12, 8, 200, 140);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // =========================================================================
    //  INNER CLASS: BgSandCreature — Biota kecil bergerak di area pasir
    //  Posisi acak, bergerak goyang/melayang/bergeser, tersebar merata
    // =========================================================================
    static class BgSandCreature extends BgAnimal {
        // type: 0=bintang laut, 1=kerang, 2=spiral shell, 3=bulu babi, 4=kepiting kecil
        int type;
        Color col;
        float phase, wobbleAmp, driftSpd;
        float baseX, baseY;

        private static final Color[][] PALETTES = {
            { new Color(255,160,40), new Color(255,100,130), new Color(255,200,50), new Color(255,140,35) },   // 0 starfish
            { new Color(255,220,200), new Color(240,225,205), new Color(220,200,255), new Color(255,235,200) }, // 1 shell
            { new Color(235,220,200), new Color(215,230,250), new Color(220,240,255), new Color(255,235,200) }, // 2 spiral
            { new Color(60,20,20),   new Color(80,30,80),    new Color(40,20,60),    new Color(70,30,30) },    // 3 urchin
            { new Color(230,80,50),  new Color(200,60,100),  new Color(240,100,60),  new Color(210,70,80) },   // 4 crab
            { new Color(40,160,30),  new Color(55,185,42),   new Color(35,145,25),   new Color(65,200,50) },   // 5 seaweed
        };
        BgSandCreature(int pw, int ph, Random r, int sandY, int type) {
            super(pw, ph, r);
            this.type  = type;
            baseX = 20 + rng.nextFloat() * (pw - 40);
            baseY = sandY + 15 + rng.nextFloat() * (ph - sandY - 30);
            x = baseX; y = baseY;
            phase     = rng.nextFloat() * 6.28f;
            wobbleAmp = 1.5f + rng.nextFloat() * 3f;
            driftSpd  = (rng.nextBoolean() ? 1 : -1) * (0.06f + rng.nextFloat() * 0.14f);
            col = PALETTES[type][rng.nextInt(PALETTES[type].length)];
        }

        @Override void update() {
            phase += 0.04f;
            x = baseX + (float)(Math.sin(phase) * wobbleAmp * 2);
            y = baseY + (float)(Math.sin(phase * 0.7f) * wobbleAmp);
            baseX += driftSpd;
            if (baseX < 15 || baseX > panelW - 15) driftSpd = -driftSpd;
        }

        @Override void draw(Graphics2D g) {
            int cx = (int)x, cy = (int)y;
            switch (type) {
                case 0: drawStarfish(g, cx, cy); break;
                case 1: drawShell(g, cx, cy);    break;
                case 2: drawSpiral(g, cx, cy);   break;
                case 3: drawUrchin(g, cx, cy);   break;
                case 4: drawCrab(g, cx, cy);     break;
                case 5: drawSeaweed(g, cx, cy);  break;
            }
        }

        private void drawStarfish(Graphics2D g, int cx, int cy) {
            int sz = 12 + (int)(wobbleAmp * 1.5f);
            int[] xp = new int[10], yp = new int[10];
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI/2 + i * Math.PI/5;
                int r = (i % 2 == 0) ? sz : sz/2;
                xp[i] = cx + (int)(Math.cos(a)*r);
                yp[i] = cy + (int)(Math.sin(a)*r);
            }
            g.setColor(new Color(0,0,0,22)); g.fillPolygon(xp, yp, 10);
            g.setColor(col);
            int[] sx = new int[10], sy = new int[10];
            for (int i=0;i<10;i++){sx[i]=xp[i]-1;sy[i]=yp[i]-1;}
            g.fillPolygon(sx, sy, 10);
            g.setColor(col.darker()); g.setStroke(new BasicStroke(1.2f));
            g.drawPolygon(sx, sy, 10); g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(255,255,255,60));
            for (int i=0;i<5;i++){
                double a=Math.PI*2*i/5-Math.PI/2;
                g.fillOval(cx+(int)(Math.cos(a)*sz*0.55)-2,cy+(int)(Math.sin(a)*sz*0.55)-2,4,4);
            }
        }

        private void drawShell(Graphics2D g, int cx, int cy) {
            g.setColor(new Color(0,0,0,20)); g.fillOval(cx-14, cy+2, 28, 8);
            g.setColor(col); g.fillArc(cx-14, cy-10, 28, 20, 0, 180);
            g.setColor(col.darker()); g.setStroke(new BasicStroke(1.2f));
            g.drawArc(cx-14, cy-10, 28, 20, 0, 180);
            for (int i=-2;i<=2;i++){
                double a=Math.PI/2+i*0.38;
                g.setColor(col.darker()); g.setStroke(new BasicStroke(0.8f));
                g.drawLine(cx, cy-1, cx+(int)(Math.cos(a)*12), cy-10+10+(int)(Math.sin(a)*10));
            }
            g.setColor(new Color(255,255,255,70)); g.fillOval(cx-6, cy-8, 7, 4);
            g.setStroke(new BasicStroke(1f));
        }

        private void drawSpiral(Graphics2D g, int cx, int cy) {
            g.setColor(new Color(0,0,0,20)); g.fillOval(cx-12, cy+3, 24, 8);
            g.setColor(col); g.fillOval(cx-11, cy-8, 22, 18);
            g.setColor(col.darker()); g.setStroke(new BasicStroke(1.1f));
            g.drawOval(cx-11, cy-8, 22, 18);
            g.drawArc(cx-7, cy-5, 14, 11, 15, 300);
            g.drawArc(cx-4, cy-3, 8, 7, 20, 260);
            g.setColor(new Color(255,255,255,80)); g.fillOval(cx-5, cy-6, 6, 4);
            g.setStroke(new BasicStroke(1f));
        }

        private void drawUrchin(Graphics2D g, int cx, int cy) {
            g.setColor(new Color(0,0,0,25)); g.fillOval(cx-8, cy+5, 16, 7);
            g.setColor(col.darker()); g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i=0;i<14;i++){
                double a=Math.PI*2*i/14;
                g.drawLine(cx+(int)(Math.cos(a)*5),cy+(int)(Math.sin(a)*5),
                           cx+(int)(Math.cos(a)*12),cy+(int)(Math.sin(a)*12));
            }
            g.setStroke(new BasicStroke(1f));
            g.setColor(col); g.fillOval(cx-6, cy-6, 12, 12);
            g.setColor(col.darker()); g.drawOval(cx-6, cy-6, 12, 12);
            g.setColor(new Color(255,255,255,55)); g.fillOval(cx-2, cy-3, 4, 3);
        }

        private void drawCrab(Graphics2D g, int cx, int cy) {
            float ls = (float)(Math.sin(phase * 3) * 3);
            Color dark = col.darker();
            g.setColor(new Color(0,0,0,25)); g.fillOval(cx-16, cy+7, 32, 9);
            g.setColor(dark); g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i=-2;i<=2;i++){
                if(i==0) continue;
                float lsi = (float)(Math.sin(phase*3+i*0.9f)*4);
                g.drawLine(cx+i*6, cy+3, cx+i*12, cy+12+(int)lsi);
            }
            g.setStroke(new BasicStroke(1f));
            g.setColor(dark); g.fillOval(cx-24, cy-6+(int)ls, 12, 9);
            g.setColor(col);  g.fillOval(cx-22, cy-4+(int)ls, 8, 6);
            g.setColor(dark); g.fillOval(cx+12, cy-6-(int)ls, 12, 9);
            g.setColor(col);  g.fillOval(cx+14, cy-4-(int)ls, 8, 6);
            g.setColor(new Color(0,0,0,22)); g.fillOval(cx-15, cy-8+3, 30, 18);
            g.setPaint(new GradientPaint(cx-15,cy-8,col.brighter(),cx+15,cy+10,dark));
            g.fillOval(cx-15, cy-8, 30, 18); g.setPaint(null);
            g.setColor(new Color(255,255,255,65)); g.fillOval(cx-7, cy-6, 10, 5);
            g.setColor(dark); g.setStroke(new BasicStroke(1.6f));
            g.drawLine(cx-6, cy-8, cx-6, cy-14); g.drawLine(cx+6, cy-8, cx+6, cy-14);
            g.setStroke(new BasicStroke(1f));
            g.setColor(Color.WHITE); g.fillOval(cx-9, cy-19, 7, 7); g.fillOval(cx+2, cy-19, 7, 7);
            g.setColor(new Color(20,20,40)); g.fillOval(cx-7, cy-17, 3, 3); g.fillOval(cx+4, cy-17, 3, 3);
            g.setColor(new Color(255,255,255,200)); g.fillOval(cx-6, cy-17, 2, 2); g.fillOval(cx+5, cy-17, 2, 2);
        }

        private void drawSeaweed(Graphics2D g, int cx, int cy) {
            // Tinggi bervariasi berdasarkan wobbleAmp
            int height = (int)(40 + wobbleAmp * 12);
            int segs = 10;
            int px = cx, py = cy;
            for (int i = 0; i < segs; i++) {
                float t = i / (float) segs;
                float sway = (float)(Math.sin(phase * 1.1f + cx * 0.05f + i * 0.6f)
                                   * (5 + i * 2.0f));
                float lean = (float)(Math.sin(cx * 0.03f) * i * 1.2f);
                int nx = cx + (int)(sway + lean);
                int ny = cy - (i + 1) * (height / segs);
                float thick = Math.max(0.7f, 3.0f - t * 2.4f);
                g.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int rv = Math.min(255, col.getRed()   + (int)(t * 20));
                int gv = Math.min(255, col.getGreen() + (int)(t * 50));
                int bv = Math.min(255, col.getBlue()  + (int)(t * 10));
                g.setColor(new Color(rv, gv, bv, 215 - (int)(t * 35)));
                g.drawLine(px, py, nx, ny);
                // Daun kecil
                if (i % 3 == 2 && i < segs - 1) {
                    int ld = (i % 6 < 3) ? 1 : -1;
                    int lw = (int)(7 + t * 9), lh = (int)(3 + t * 3);
                    g.setStroke(new BasicStroke(1f));
                    g.setColor(new Color(Math.min(255,rv+18), Math.min(255,gv+28), bv, 200));
                    java.awt.geom.AffineTransform old = g.getTransform();
                    double la = Math.atan2(ny-py, nx-px) + Math.PI/2.5*ld + Math.sin(phase+i)*0.15;
                    g.rotate(la, nx, ny);
                    g.fillOval(nx-lw/2, ny-lh/2, lw, lh);
                    g.setColor(new Color(rv-20, gv-15, bv, 110));
                    g.setStroke(new BasicStroke(0.5f));
                    g.drawLine(nx-lw/2+1, ny, nx+lw/2-1, ny);
                    g.setTransform(old);
                    g.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                }
                px = nx; py = ny;
            }
            g.setColor(new Color(Math.min(255,col.getRed()+50),
                                 Math.min(255,col.getGreen()+65),
                                 Math.min(255,col.getBlue()+15), 155));
            g.fillOval(px-3, py-4, 6, 7);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // =========================================================================
    //  INNER CLASS: SparkleParticle — Efek kilap bintang saat jawaban benar
    // =========================================================================
    static class SparkleParticle {
        float x, y, vx, vy, r;
        Color col;
        boolean alive = true;
        int life, maxLife;

        SparkleParticle(int cx, int cy, Color col, Random rng) {
            x = cx; y = cy; this.col = col;
            double angle = rng.nextDouble() * Math.PI * 2;
            float spd = 1.5f + rng.nextFloat() * 4f;
            vx = (float)(Math.cos(angle) * spd);
            vy = (float)(Math.sin(angle) * spd) - 1f;
            r = 3f + rng.nextFloat() * 6f;
            maxLife = 20 + rng.nextInt(20);
            life = maxLife;
        }

        void update() {
            x += vx; y += vy; vy += 0.05f; vx *= 0.96f;
            if (--life <= 0) alive = false;
        }

        void draw(Graphics2D g) {
            if (!alive) return;
            float al = life / (float)maxLife;
            int   a  = (int)(al * 220);
            int   s  = Math.max(2, (int)(r * al));
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), a));
            // Gambar bintang kecil (silang)
            g.setStroke(new BasicStroke(2f));
            g.drawLine((int)x - s, (int)y, (int)x + s, (int)y);
            g.drawLine((int)x, (int)y - s, (int)x, (int)y + s);
            g.setStroke(new BasicStroke(1f));
        }
    }
}
