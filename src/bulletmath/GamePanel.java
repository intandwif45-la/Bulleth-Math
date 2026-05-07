package bulletmath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * GamePanel - Panel utama yang menangani semua logika dan rendering game.
 */
public class GamePanel extends JPanel implements ActionListener, MouseListener {

    // ── Konstanta ─────────────────────────────────────────────────────────────
    public static final int W = 600, H = 700;
    private static final int FPS = 60;

    // ── State game ───────────────────────────────────────────────────────────
    public enum State { MENU, PLAYING, GAME_OVER }
    private State state = State.MENU;

    // ── Data game ────────────────────────────────────────────────────────────
    private int score     = 0;
    private int lives     = 3;
    private int level     = 1;
    private int bulletNum = 0;   // Angka yang ada di peluru saat ini
    private int correctThisLevel = 0;

    private List<Enemy>   enemies   = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private List<FloatText> floatTexts = new ArrayList<>();

    private javax.swing.Timer gameTimer;
    private Random rng = new Random();

    // ── Spawn timing ─────────────────────────────────────────────────────────
    private int spawnCooldown = 0;
    private int spawnInterval = 120;  // frame
    private int waveSize      = 3;

    // ── Gun animasi ──────────────────────────────────────────────────────────
    private int   gunAngle   = 0;
    private float gunRecoil  = 0;
    private int   gunRecoilT = 0;

    // ── Stars latar ──────────────────────────────────────────────────────────
    private int[][] stars;

    // ── Pesan feedback ───────────────────────────────────────────────────────
    private String feedback     = "";
    private int    feedbackTimer = 0;
    private Color  feedbackColor = Color.WHITE;

    // ── Scroll background ────────────────────────────────────────────────────
    private float bgScroll = 0;

    // ─────────────────────────────────────────────────────────────────────────
    public GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(10, 10, 30));
        setFocusable(true);
        addMouseListener(this);

        // Bintang acak di latar
        stars = new int[80][3];
        for (int[] s : stars) {
            s[0] = rng.nextInt(W);
            s[1] = rng.nextInt(H);
            s[2] = rng.nextInt(3) + 1;
        }

        gameTimer = new javax.swing.Timer(1000 / FPS, this);
        gameTimer.start();
    }

    // ── Loop utama ────────────────────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == State.PLAYING) {
            updateGame();
        }
        bgScroll = (bgScroll + 0.3f) % H;
        repaint();
    }

    private void updateGame() {
        // Spawn musuh
        spawnCooldown--;
        if (spawnCooldown <= 0) {
            spawnWave();
            spawnCooldown = Math.max(60, spawnInterval - level * 8);
        }

        // Update semua musuh
        List<Enemy> toRemove = new ArrayList<>();
        for (Enemy en : enemies) {
            en.update();

            // Musuh melewati batas bawah
            if (en.getY() > H + 20 && en.isAlive() && !en.isExploding()) {
                en.setAlive(false);
                toRemove.add(en);
                if (en.isTarget()) {
                    lives--;
                    showFeedback("Musuh Lolos! -1 Nyawa", new Color(255, 80, 80));
                    spawnShake();
                }
            }

            // Selesai animasi ledakan
            if (en.isExploding() && en.getExpTimer() > 20) {
                en.setAlive(false);
                toRemove.add(en);
            }
        }
        enemies.removeAll(toRemove);

        // Update partikel
        particles.removeIf(p -> !p.alive);
        for (Particle p : particles) p.update();

        // Update float texts
        floatTexts.removeIf(ft -> !ft.alive);
        for (FloatText ft : floatTexts) ft.update();

        // Feedback timer
        if (feedbackTimer > 0) feedbackTimer--;

        // Gun recoil animasi
        if (gunRecoilT > 0) {
            gunRecoil = 8f * (gunRecoilT / 10f);
            gunRecoilT--;
        } else {
            gunRecoil = 0;
        }

        // Cek game over
        if (lives <= 0) {
            lives = 0;
            state = State.GAME_OVER;
        }

        // Level naik setiap 5 tembakan benar
        if (correctThisLevel >= 5) {
            level++;
            correctThisLevel = 0;
            waveSize = Math.min(5, waveSize + 1);
            showFeedback("Level " + level + "! Semakin Sulit!", new Color(100, 255, 150));
        }
    }

    // ── Spawn gelombang musuh ─────────────────────────────────────────────────
    private void spawnWave() {
        if (!enemies.isEmpty()) return;   // Tunggu layar bersih

        // Angka baru untuk peluru
        bulletNum = 2 + rng.nextInt(9 + level);   // Makin tinggi level, angka makin besar

        // Tentukan berapa musuh
        int count = 2 + rng.nextInt(2) + (level > 3 ? 1 : 0);
        count = Math.min(count, 5);

        // Tentukan index musuh yang BENAR
        int targetIdx = rng.nextInt(count);

        // Susun posisi X agar tidak tumpang tindih
        int gap    = W / count;
        int margin = 20;

        for (int i = 0; i < count; i++) {
            int xPos = margin + i * gap + rng.nextInt(gap - Enemy.W - margin);
            xPos = Math.max(margin, Math.min(W - Enemy.W - margin, xPos));

            String expr;
            int    val;
            if (i == targetIdx) {
                // Ekspresi yang BENAR (hasilnya = bulletNum)
                expr = generateCorrectExpr(bulletNum);
                val  = bulletNum;
            } else {
                // Ekspresi yang SALAH (hasilnya ≠ bulletNum)
                int wrongVal;
                do { wrongVal = 1 + rng.nextInt(20); } while (wrongVal == bulletNum);
                expr = generateCorrectExpr(wrongVal);
                val  = wrongVal;
            }

            float speed = 0.8f + level * 0.2f + rng.nextFloat() * 0.4f;
            enemies.add(new Enemy(xPos, expr, val, i == targetIdx, speed));
        }
    }

    // ── Buat ekspresi yang menghasilkan target ────────────────────────────────
    private String generateCorrectExpr(int target) {
        int type = rng.nextInt(level < 2 ? 2 : level < 4 ? 3 : 4);
        switch (type) {
            case 0: {  // Penjumlahan: a + b = target
                int a = rng.nextInt(target + 1);
                return a + " + " + (target - a);
            }
            case 1: {  // Pengurangan: (target+b) - b = target
                int b = 1 + rng.nextInt(10);
                return (target + b) + " - " + b;
            }
            case 2: {  // Perkalian: jika target punya faktor
                List<int[]> factors = new ArrayList<>();
                for (int f = 1; f <= target; f++)
                    if (target % f == 0 && f > 1 && target/f > 1)
                        factors.add(new int[]{f, target/f});
                if (!factors.isEmpty()) {
                    int[] pair = factors.get(rng.nextInt(factors.size()));
                    return pair[0] + " x " + pair[1];
                }
                // Fallback ke penjumlahan
                int a = rng.nextInt(target + 1);
                return a + " + " + (target - a);
            }
            case 3: {  // Pembagian: (target*b) / b = target
                int b = 2 + rng.nextInt(5);
                return (target * b) + " / " + b;
            }
            default: {
                int a = rng.nextInt(target + 1);
                return a + " + " + (target - a);
            }
        }
    }

    // ── Tembak musuh ──────────────────────────────────────────────────────────
    private void shoot(int mx, int my) {
        if (state != State.PLAYING) return;
        if (enemies.isEmpty()) return;

        gunRecoilT = 10;

        for (Enemy en : enemies) {
            if (!en.isAlive() || en.isExploding()) continue;
            if (en.contains(mx, my)) {
                if (en.isTarget()) {
                    // BENAR!
                    en.explode();
                    int pts = 10 + level * 5;
                    score += pts;
                    correctThisLevel++;
                    showFeedback("TEPAT! +" + pts, new Color(80, 255, 120));
                    spawnParticles((int)en.getX() + Enemy.W/2, (int)en.getY() + Enemy.H/2, new Color(100, 255, 150));
                    addFloatText("+" + pts, (int)en.getX() + Enemy.W/2, (int)en.getY(), new Color(80,255,80));
                } else {
                    // SALAH!
                    lives--;
                    score = Math.max(0, score - 5);
                    showFeedback("SALAH! -1 Nyawa", new Color(255, 60, 60));
                    spawnParticles((int)en.getX() + Enemy.W/2, (int)en.getY() + Enemy.H/2, new Color(255, 80, 80));
                    addFloatText("-5", (int)en.getX() + Enemy.W/2, (int)en.getY(), new Color(255,60,60));
                    en.explode();
                }
                break;
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g2);

        if (state == State.MENU)      drawMenu(g2);
        else if (state == State.PLAYING)   drawGame(g2);
        else if (state == State.GAME_OVER) drawGameOver(g2);
    }

    // ── Latar belakang bintang ────────────────────────────────────────────────
    private void drawBackground(Graphics2D g) {
        // Gradien langit gelap
        GradientPaint bg = new GradientPaint(0,0,new Color(8,8,28),0,H,new Color(20,10,50));
        g.setPaint(bg);
        g.fillRect(0, 0, W, H);
        g.setPaint(null);

        // Bintang
        for (int[] s : stars) {
            int brightness = 150 + rng.nextInt(3);
            g.setColor(new Color(brightness, brightness, brightness, 180));
            g.fillOval(s[0], s[1], s[2], s[2]);
        }

        // Grid perspektif di bawah (tanah futuristik)
        g.setColor(new Color(80, 40, 160, 60));
        g.setStroke(new BasicStroke(1f));
        int horizon = H - 100;
        for (int gx = 0; gx <= W; gx += 60) {
            g.drawLine(gx, horizon, W/2, H+50);
        }
        for (int gy = horizon; gy <= H; gy += 30) {
            g.drawLine(0, gy, W, gy);
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ── Menu utama ───────────────────────────────────────────────────────────
    private void drawMenu(Graphics2D g) {
        // Panel judul
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(60, 80, W-120, 500, 30, 30);
        g.setColor(new Color(100, 80, 255, 100));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(60, 80, W-120, 500, 30, 30);
        g.setStroke(new BasicStroke(1f));

        // Judul
        g.setFont(new Font("Arial Black", Font.BOLD, 42));
        drawShadowText(g, "BULLET", W/2, 160, new Color(255,220,0), Color.BLACK);
        drawShadowText(g, "MATH", W/2, 210, new Color(255,120,40), Color.BLACK);

        // Sub-judul
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(new Color(180,180,255));
        drawCenterText(g, "Reverse Thinking Shooter", W/2, 245);

        // Penjelasan cara main
        g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        String[] howto = {
            "🎯  Cara Bermain:",
            "",
            "  Kamu punya PELURU dengan sebuah ANGKA.",
            "  Musuh datang membawa EKSPRESI MATEMATIKA.",
            "  Tembak musuh yang ekspresinya",
            "  menghasilkan angka di pelurumu!",
            "",
            "  Contoh: Peluru = 5",
            "  Musuh A: 3 + 6 = 9  ✗",
            "  Musuh B: 2 + 3 = 5  ✓  ← TEMBAK INI!",
            "  Musuh C: 7 + 1 = 8  ✗",
        };
        int ly = 285;
        for (String line : howto) {
            if (line.startsWith("🎯")) {
                g.setFont(new Font("Segoe UI", Font.BOLD, 15));
                g.setColor(new Color(255,220,100));
            } else if (line.contains("✓")) {
                g.setColor(new Color(80,255,120));
                g.setFont(new Font("Consolas", Font.BOLD, 14));
            } else if (line.contains("✗")) {
                g.setColor(new Color(255,100,80));
                g.setFont(new Font("Consolas", Font.PLAIN, 14));
            } else {
                g.setColor(new Color(210,210,255));
                g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            }
            g.drawString(line, 100, ly);
            ly += 20;
        }

        // Tombol MAIN
        int bx = W/2-90, by = 520, bw = 180, bh = 45;
        GradientPaint btnGrad = new GradientPaint(bx,by,new Color(80,200,100),bx,by+bh,new Color(30,140,60));
        g.setPaint(btnGrad);
        g.fillRoundRect(bx, by, bw, bh, 22, 22);
        g.setPaint(null);
        g.setColor(new Color(255,255,255,200));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(bx, by, bw, bh, 22, 22);
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("Arial Black", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        drawCenterText(g, "▶  MAIN!", W/2, by+30);
    }

    // ── Layar bermain ─────────────────────────────────────────────────────────
    private void drawGame(Graphics2D g) {
        // HUD atas
        drawHUD(g);

        // Musuh
        for (Enemy en : enemies) en.draw(g);

        // Partikel
        for (Particle p : particles) p.draw(g);

        // Float texts
        for (FloatText ft : floatTexts) ft.draw(g);

        // Area gun bawah
        drawGunArea(g);

        // Feedback pesan
        if (feedbackTimer > 0) {
            float alpha = Math.min(1f, feedbackTimer / 30f);
            g.setFont(new Font("Arial Black", Font.BOLD, 20));
            g.setColor(new Color(feedbackColor.getRed(), feedbackColor.getGreen(), feedbackColor.getBlue(), (int)(alpha*220)));
            drawCenterText(g, feedback, W/2, H - 180);
        }
    }

    // ── HUD (Score, Lives, Level) ─────────────────────────────────────────────
    private void drawHUD(Graphics2D g) {
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(0, 0, W, 55, 0, 0);

        // Score
        g.setFont(new Font("Arial Black", Font.BOLD, 14));
        g.setColor(new Color(255,220,80));
        g.drawString("SKOR: " + score, 12, 22);

        // Level
        g.setColor(new Color(100,200,255));
        drawCenterText(g, "LEVEL " + level, W/2, 22);

        // Lives (hati)
        g.setFont(new Font("Arial Black", Font.BOLD, 14));
        g.setColor(new Color(255,80,80));
        StringBuilder heartsStr = new StringBuilder();
        for (int i = 0; i < 3; i++) heartsStr.append(i < lives ? "♥ " : "♡ ");
        g.drawString(heartsStr.toString().trim(), W - 100, 22);

        // Progress bar level
        int prog = (int)((float)correctThisLevel / 5f * (W - 20));
        g.setColor(new Color(255,255,255,30));
        g.fillRect(0, 50, W, 5);
        g.setColor(new Color(80,220,120));
        g.fillRect(0, 50, prog, 5);
    }

    // ── Area senjata bawah ────────────────────────────────────────────────────
    private void drawGunArea(Graphics2D g) {
        // Panel bawah
        g.setColor(new Color(0,0,0,200));
        g.fillRect(0, H-140, W, 140);
        g.setColor(new Color(80,60,160,120));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(0, H-140, W, H-140);
        g.setStroke(new BasicStroke(1f));

        // Instruksi
        g.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        g.setColor(new Color(180,180,220,180));
        drawCenterText(g, "Klik musuh yang ekspresinya = angka peluru!", W/2, H-118);

        // Gambar GUN
        int gx = W/2, gy = H - 55;
        drawGun(g, gx, gy);

        // Angka peluru besar
        g.setFont(new Font("Arial Black", Font.BOLD, 48));
        GradientPaint numGrad = new GradientPaint(gx-40, gy-60, new Color(255,220,0), gx+40, gy-10, new Color(255,120,0));
        g.setPaint(numGrad);
        String numStr = String.valueOf(bulletNum);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(numStr, gx - fm.stringWidth(numStr)/2 - 100, gy - 10);
        g.setPaint(null);

        // Label "PELURU"
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(new Color(200,180,100));
        drawCenterText(g, "PELURU", gx - 100, gy + 12);
    }

    // ── Gambar senjata ────────────────────────────────────────────────────────
    private void drawGun(Graphics2D g, int cx, int cy) {
        int rx = (int) gunRecoil;   // Efek mundur ke belakang saat menembak

        // Laras
        g.setColor(new Color(160,160,180));
        g.fillRoundRect(cx - 8 + rx, cy - 50, 16, 40, 6, 6);

        // Badan senjata
        GradientPaint gunGrad = new GradientPaint(cx-25,cy-10,new Color(100,100,120),cx+25,cy+30,new Color(50,50,70));
        g.setPaint(gunGrad);
        g.fillRoundRect(cx - 25 + rx, cy - 15, 50, 35, 10, 10);
        g.setPaint(null);

        // Detail laras
        g.setColor(new Color(80,80,100));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx - 8 + rx, cy - 50, 16, 40, 6, 6);
        g.drawRoundRect(cx - 25 + rx, cy - 15, 50, 35, 10, 10);
        g.setStroke(new BasicStroke(1f));

        // Lingkaran kilap
        g.setColor(new Color(255,255,255,60));
        g.fillOval(cx - 15 + rx, cy - 12, 12, 8);
    }

    // ── Game Over screen ──────────────────────────────────────────────────────
    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0,0,0,180));
        g.fillRoundRect(60, 120, W-120, 420, 30, 30);
        g.setColor(new Color(200,50,50,120));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(60, 120, W-120, 420, 30, 30);
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("Arial Black", Font.BOLD, 48));
        drawShadowText(g, "GAME", W/2, 200, new Color(255,60,60), Color.BLACK);
        drawShadowText(g, "OVER", W/2, 258, new Color(255,60,60), Color.BLACK);

        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(new Color(255,220,80));
        drawCenterText(g, "Skor Akhir: " + score, W/2, 310);
        g.setColor(new Color(160,220,255));
        drawCenterText(g, "Level Dicapai: " + level, W/2, 345);

        // Peringkat
        String rank;
        Color  rankColor;
        if (score >= 200) { rank = "★ LUAR BIASA! ★"; rankColor = new Color(255,200,0); }
        else if (score >= 100) { rank = "✦ HEBAT! ✦";  rankColor = new Color(100,220,255); }
        else if (score >= 50)  { rank = "Cukup Baik";   rankColor = new Color(120,220,120); }
        else                   { rank = "Terus Latihan!"; rankColor = new Color(200,200,200); }

        g.setFont(new Font("Arial Black", Font.BOLD, 18));
        g.setColor(rankColor);
        drawCenterText(g, rank, W/2, 385);

        // Tombol Main Lagi
        int bx=W/2-100, by=420, bw=200, bh=48;
        GradientPaint btnG = new GradientPaint(bx,by,new Color(60,120,220),bx,by+bh,new Color(30,60,140));
        g.setPaint(btnG);
        g.fillRoundRect(bx, by, bw, bh, 24, 24);
        g.setPaint(null);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(bx, by, bw, bh, 24, 24);
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("Arial Black", Font.BOLD, 18));
        drawCenterText(g, "↺  MAIN LAGI", W/2, by+32);

        // Tombol Menu
        int bx2=W/2-80, by2=485;
        g.setColor(new Color(255,255,255,40));
        g.fillRoundRect(bx2, by2, 160, 38, 20, 20);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(new Color(200,200,255));
        drawCenterText(g, "⟵  Menu Utama", W/2, by2+25);
    }

    // ── Helper: teks di tengah ────────────────────────────────────────────────
    private void drawCenterText(Graphics2D g, String text, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text)/2, y);
    }

    private void drawShadowText(Graphics2D g, String text, int cx, int y, Color col, Color shadow) {
        FontMetrics fm = g.getFontMetrics();
        int x = cx - fm.stringWidth(text)/2;
        g.setColor(shadow);
        g.drawString(text, x+3, y+3);
        g.setColor(col);
        g.drawString(text, x, y);
    }

    // ── Partikel ledakan ──────────────────────────────────────────────────────
    private void spawnParticles(int cx, int cy, Color col) {
        for (int i = 0; i < 18; i++)
            particles.add(new Particle(cx, cy, col, rng));
    }

    private void spawnShake() {
        // Visual feedback saat nyawa berkurang (partikel merah di tepi)
        for(int i=0;i<10;i++)
            particles.add(new Particle(rng.nextInt(W), H-130+rng.nextInt(10), new Color(255,50,50), rng));
    }

    private void addFloatText(String text, int x, int y, Color col) {
        floatTexts.add(new FloatText(text, x, y, col));
    }

    private void showFeedback(String msg, Color col) {
        feedback      = msg;
        feedbackColor = col;
        feedbackTimer = 90;
    }

    // ── Reset game ────────────────────────────────────────────────────────────
    private void resetGame() {
        score             = 0;
        lives             = 3;
        level             = 1;
        correctThisLevel  = 0;
        waveSize          = 3;
        spawnInterval     = 120;
        spawnCooldown     = 30;
        enemies.clear();
        particles.clear();
        floatTexts.clear();
        bulletNum         = 0;
        feedback          = "";
        feedbackTimer     = 0;
        state             = State.PLAYING;
    }

    // ── Mouse events ──────────────────────────────────────────────────────────
    @Override public void mouseClicked(MouseEvent e) {
        int mx = e.getX(), my = e.getY();

        if (state == State.MENU) {
            // Cek klik tombol MAIN
            int bx=W/2-90, by=520, bw=180, bh=45;
            if (mx>=bx && mx<=bx+bw && my>=by && my<=by+bh) resetGame();
        }
        else if (state == State.PLAYING) {
            shoot(mx, my);
        }
        else if (state == State.GAME_OVER) {
            // Klik tombol Main Lagi
            int bx=W/2-100, by=420, bw=200, bh=48;
            if (mx>=bx && mx<=bx+bw && my>=by && my<=by+bh) resetGame();
            // Klik tombol Menu
            int bx2=W/2-80, by2=485;
            if (mx>=bx2 && mx<=bx2+160 && my>=by2 && my<=by2+38) state=State.MENU;
        }
    }
    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}
