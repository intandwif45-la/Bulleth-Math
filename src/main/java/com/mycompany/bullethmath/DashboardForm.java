package com.mycompany.bullethmath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * DashboardForm.java — Menu utama.
 * Layout: header → 4 kartu menu → strip progress → footer
 */
public class DashboardForm extends BaseForm {

    // ── Konstanta layout kartu (dipakai juga oleh MateriForm) ────────────────
    static final int CARD_W   = 210;
    static final int CARD_H   = 250;
    static final int CARD_GAP = 22;

    private final DatabaseManager db;
    private final int             userId;
    private final String          username;
    private float                 starPhase = 0f;

    public DashboardForm() {
        super("Math Adventure - Dashboard", 860, 640);
        this.db       = SessionManager.getDb();
        this.userId   = SessionManager.getUserId();
        this.username = SessionManager.getUsername();
        buildUI();
        SwingUtilities.invokeLater(() -> TransitionManager.fadeIn(this));
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        int headerTop = (int)(H * 0.04);

        JLabel lblTitle = makeLabel("🌊  Math Adventure  🌊",
                UITheme.TITLE_FONT.deriveFont(36f), UITheme.GOLD);
        place(lblTitle, 0, headerTop, W, 48);

        JLabel lblSub = makeLabel("Belajar Matematika Seru Bersama Teman Laut!",
                UITheme.BODY_FONT, UITheme.OCEAN_SURF);
        place(lblSub, 0, headerTop + 52, W, 28);

        int totalStars = getTotalStars();
        JLabel lblUser = makeLabel(
                "Halo, " + username + "!   ⭐ " + totalStars + " bintang",
                UITheme.SUBHEAD.deriveFont(Font.BOLD, 17f), UITheme.TEXT_WHITE);
        place(lblUser, 0, headerTop + 84, W, 26);

        // ── 4 Kartu Menu ──────────────────────────────────────────────────────
        int totalW  = 4 * CARD_W + 3 * CARD_GAP;
        int startX  = (W - totalW) / 2;
        int cardY   = headerTop + 122;

        String[] labels  = {"MAIN GAME", "LEADERBOARD", "PENGATURAN", "LOGOUT"};
        String[] emojis  = {"🎮",         "🏆",           "\u2699",     "🚪"};
        Color[]  colors  = {
            new Color(28, 155, 215),
            new Color(215, 160, 18),
            new Color(55, 170, 75),
            new Color(205, 60, 75)
        };
        String[] actions = {"mulai", "leaderboard", "settings", "logout"};

        for (int i = 0; i < 4; i++) {
            final String action = actions[i];
            JPanel card = makeMenuCard(emojis[i], labels[i], colors[i], action);
            card.setBounds(startX + i * (CARD_W + CARD_GAP), cardY, CARD_W, CARD_H);
            contentPane.add(card);
        }

        // ── Strip Progress ────────────────────────────────────────────────────
        int stripY = cardY + CARD_H + 20;
        addProgressStrip(stripY);

        // ── Footer ────────────────────────────────────────────────────────────
        JLabel lblVer = makeLabel("Math Adventure v2.0  |  © 2026",
                UITheme.SMALL_FONT, new Color(120, 180, 220, 170));
        place(lblVer, 0, H - 26, W, 20);
    }

    // ── Kartu menu ────────────────────────────────────────────────────────────
    private JPanel makeMenuCard(String emoji, String label,
                                 Color accent, String action) {
        JPanel card = new JPanel(null) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                    @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) { handleMenu(action); }
                });
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                paintCard(g2, getWidth(), getHeight(), accent, emoji, label, hover);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    // ── Logika paint kartu (dipakai ulang di MateriForm) ─────────────────────
    static void paintCard(Graphics2D g2, int w, int h,
                           Color accent, String emoji, String label,
                           boolean hover) {
        int yo = hover ? 0 : 4;

        // Shadow
        g2.setColor(new Color(0, 0, 0, hover ? 60 : 40));
        g2.fillRoundRect(5, yo + 8, w - 8, h - yo - 8, 28, 28);

        // Body gradien atas→bawah
        GradientPaint body = new GradientPaint(
            0, yo,     hover ? accent.brighter() : accent,
            0, yo + h, accent.darker().darker()
        );
        g2.setPaint(body);
        g2.fillRoundRect(2, yo, w - 4, h - yo - 4, 28, 28);

        // Glossy highlight atas
        g2.setColor(new Color(255, 255, 255, hover ? 65 : 45));
        g2.fillRoundRect(4, yo + 2, w - 8, (h - yo - 4) / 2, 26, 26);

        // Border
        g2.setColor(new Color(255, 255, 255, hover ? 150 : 90));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(2, yo, w - 4, h - yo - 4, 28, 28);
        g2.setStroke(new BasicStroke(1f));

        // ── Lingkaran ikon ────────────────────────────────────────────────
        int circR  = 46;
        int circCX = w / 2;
        int circCY = yo + 28 + circR;          // center Y lingkaran
        g2.setColor(new Color(255, 255, 255, 28));
        g2.fillOval(circCX - circR, circCY - circR, circR * 2, circR * 2);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawOval(circCX - circR, circCY - circR, circR * 2, circR * 2);
        g2.setStroke(new BasicStroke(1f));

        // ── Emoji ─────────────────────────────────────────────────────────
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        FontMetrics fmE = g2.getFontMetrics();
        // Pakai GlyphVector untuk bounding box yang akurat (fix ⚙ tidak center)
        java.awt.font.GlyphVector gv = g2.getFont().createGlyphVector(
            g2.getFontRenderContext(), emoji);
        java.awt.geom.Rectangle2D bounds = gv.getVisualBounds();
        int ex = (int)(w / 2 - bounds.getWidth() / 2 - bounds.getX());
        int ey = circCY + (int)(fmE.getAscent() / 2 - fmE.getDescent() / 2) - 2;
        g2.setColor(Color.WHITE);
        g2.drawString(emoji, ex, ey);

        // ── Label ─────────────────────────────────────────────────────────
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        FontMetrics fmL = g2.getFontMetrics();
        int labelY = circCY + circR + 28;      // 28px di bawah lingkaran
        int lx = (w - fmL.stringWidth(label)) / 2;
        // Shadow teks
        g2.setColor(new Color(0, 0, 0, 65));
        g2.drawString(label, lx + 1, labelY + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(label, lx, labelY);

        // ── Panah hover ───────────────────────────────────────────────────
        if (hover) {
            g2.setColor(new Color(255, 255, 255, 170));
            int ax = w / 2, ay = h - yo - 16;
            g2.fillPolygon(
                new int[]{ax - 9, ax + 9, ax},
                new int[]{ay - 7, ay - 7, ay + 2}, 3
            );
        }
    }

    // ── Strip progress ────────────────────────────────────────────────────────
    private void addProgressStrip(int y) {
        String[] keys  = {
            DatabaseManager.PENJUMLAHAN, DatabaseManager.PENGURANGAN,
            DatabaseManager.PERKALIAN,   DatabaseManager.PEMBAGIAN
        };
        String[] names = {"➕ Penjumlahan", "➖ Pengurangan",
                           "✖ Perkalian",   "➗ Pembagian"};
        Color[]  cols  = {
            new Color(50, 200, 120), new Color(255, 140, 60),
            new Color(80, 180, 255), new Color(200, 100, 255)
        };

        int sw = W - 60, sh = 100;
        JPanel strip = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 25, 70, 130));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(255, 255, 255, 38));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 22, 22);
                g2.dispose();
            }
        };
        strip.setBounds(30, y, sw, sh);
        strip.setOpaque(false);
        contentPane.add(strip);

        JLabel lblTitle = new JLabel("📊  Progress Belajarmu");
        lblTitle.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f));
        lblTitle.setForeground(new Color(170, 225, 255));
        lblTitle.setBounds(16, 10, 240, 20);
        strip.add(lblTitle);

        // Hitung lebar tiap kolom secara merata
        int padding = 16;
        int colW    = (sw - padding * 2) / 4;

        for (int i = 0; i < 4; i++) {
            int lv  = db.getLevelTerbuka(userId, keys[i]);
            float pct = Math.min((lv - 1) / 50f, 1f);
            final Color bc  = cols[i];
            final float bpct = pct;
            final int   lvl  = lv;

            int bx = padding + i * colW;
            int bw = colW - 8;

            JLabel lblName = new JLabel(names[i], SwingConstants.CENTER);
            lblName.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 12f));
            lblName.setForeground(Color.WHITE);
            lblName.setBounds(bx, 34, bw, 18);
            strip.add(lblName);

            JPanel bar = new JPanel(null) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    int bw2 = getWidth(), bh = getHeight();
                    g2.setColor(new Color(0, 0, 0, 85));
                    g2.fillRoundRect(0, 0, bw2, bh, bh, bh);
                    int fw = (int)(bw2 * bpct);
                    if (fw > 2) {
                        g2.setPaint(new GradientPaint(0, 0, bc.brighter(), fw, 0, bc));
                        g2.fillRoundRect(0, 0, fw, bh, bh, bh);
                        g2.setColor(new Color(255, 255, 255, 55));
                        g2.fillRoundRect(0, 0, fw, bh / 2, bh, bh);
                    }
                    g2.setColor(new Color(255, 255, 255, 45));
                    g2.setStroke(new BasicStroke(0.9f));
                    g2.drawRoundRect(0, 0, bw2-1, bh-1, bh, bh);
                    g2.dispose();
                }
            };
            bar.setOpaque(false);
            bar.setBounds(bx, 56, bw, 16);
            strip.add(bar);

            JLabel lblLv = new JLabel("Level " + lvl + " / 50", SwingConstants.CENTER);
            lblLv.setFont(UITheme.SMALL_FONT.deriveFont(10f));
            lblLv.setForeground(new Color(195, 228, 255, 200));
            lblLv.setBounds(bx, 76, bw, 16);
            strip.add(lblLv);
        }
    }

    private int getTotalStars() {
        String[] keys = {
            DatabaseManager.PENJUMLAHAN, DatabaseManager.PENGURANGAN,
            DatabaseManager.PERKALIAN,   DatabaseManager.PEMBAGIAN
        };
        int total = 0;
        for (String k : keys)
            for (int lv = 1; lv <= 50; lv++)
                total += db.getBestStars(userId, k, lv);
        return total;
    }

    @Override
    protected void drawExtras(Graphics2D g2) {
        starPhase += 0.05f;
        int[][] pts = {
            {42, 32}, {W-52, 26}, {W/2-175, 52},
            {W/2+155, 46}, {78, 88}, {W-88, 82}
        };
        for (int i = 0; i < pts.length; i++) {
            float alpha = 0.35f + (float)(Math.sin(starPhase + i * 1.1f) * 0.38f);
            float sc    = 0.55f + (float)(Math.sin(starPhase * 1.3f + i * 0.8f) * 0.3f);
            int   sz    = Math.max(3, (int)(10 * sc));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                            Math.max(0.05f, Math.min(0.88f, alpha))));
            drawSparkle(g2, pts[i][0], pts[i][1], sz, new Color(255, 228, 70));
            g2.setComposite(old);
        }
    }

    private void drawSparkle(Graphics2D g2, int cx, int cy, int r, Color col) {
        g2.setColor(col);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx-r, cy, cx+r, cy);
        g2.drawLine(cx, cy-r, cx, cy+r);
        int d = (int)(r * 0.65f);
        g2.drawLine(cx-d, cy-d, cx+d, cy+d);
        g2.drawLine(cx-d, cy+d, cx+d, cy-d);
        g2.fillOval(cx-2, cy-2, 4, 4);
        g2.setStroke(new BasicStroke(1f));
    }

    private void handleMenu(String action) {
        switch (action) {
            case "mulai":
                TransitionManager.fadeOut(this, () -> {
                    new MateriForm().setVisible(true);
                    dispose();
                });
                break;
            case "leaderboard": new LeaderboardForm(this).setVisible(true);  break;
            case "settings":    new SettingsForm(this).setVisible(true);     break;
            case "logout":      doLogout();                                   break;
        }
    }

    private void doLogout() {
        int c = JOptionPane.showConfirmDialog(this,
            "Apakah kamu yakin ingin keluar?", "Konfirmasi Logout",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (c == JOptionPane.YES_OPTION) {
            SessionManager.logout();
            TransitionManager.fadeOut(this, () -> {
                new LoginForm().setVisible(true);
                dispose();
            });
        }
    }
}
