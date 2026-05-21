package com.mycompany.bullethmath;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * LevelForm.java
 * Grid 50 level dengan status locked/unlocked dan bintang per level.
 */
public class LevelForm extends BaseForm {

    private static final int TOTAL_LEVELS = 50;
    private static final int COLS = 10;
    private static final int ROWS = 5;

    private final String materi;
    private final int    userId;
    private final int    levelTerbuka;
    private final DatabaseManager db;

    public LevelForm(String materi) {
        super("Math Adventure - Pilih Level: " +
              LevelGenerator.getMateriName(materi), 940, 680);
        this.materi       = materi;
        this.userId       = SessionManager.getUserId();
        this.db           = SessionManager.getDb();
        this.levelTerbuka = db.getLevelTerbuka(userId, materi);
        buildUI();
        // Fade-in saat form dibuka
        SwingUtilities.invokeLater(() -> TransitionManager.fadeIn(this));
    }

    private void buildUI() {
        Color materColor = LevelGenerator.getMateriColor(materi);

        // ── Header ────────────────────────────────────────────────────────────
        JLabel lblTitle = makeLabel(
            LevelGenerator.getMateriName(materi) + "  —  Pilih Level",
            UITheme.HEADER_FONT.deriveFont(28f), UITheme.GOLD
        );
        place(lblTitle, 0, 18, W, 42);

        JLabel lblInfo = makeLabel(
            "Level terbuka: " + levelTerbuka + " / " + TOTAL_LEVELS,
            UITheme.BODY_FONT, materColor
        );
        place(lblInfo, 0, 58, W, 28);

        // ── Grid Level ────────────────────────────────────────────────────────
        int cellW = 74, cellH = 74, gapX = 10, gapY = 10;
        int gridW = COLS * cellW + (COLS - 1) * gapX;
        int gridH = ROWS * cellH + (ROWS - 1) * gapY;
        int gridX = (W - gridW) / 2;
        int gridY = 98;

        // Panel scroll (jika perlu) — tapi 50 level muat di 5x10
        for (int i = 1; i <= TOTAL_LEVELS; i++) {
            int row = (i - 1) / COLS;
            int col = (i - 1) % COLS;
            int cx = gridX + col * (cellW + gapX);
            int cy = gridY + row * (cellH + gapY);

            boolean unlocked = (i <= levelTerbuka);
            int stars = unlocked ? db.getBestStars(userId, materi, i) : 0;

            JPanel cell = createLevelCell(i, unlocked, stars, materColor);
            cell.setBounds(cx, cy, cellW, cellH);
            contentPane.add(cell);
        }

        // ── Tombol Kembali ────────────────────────────────────────────────────
        JButton btnBack = UITheme.makeButton("◀  Kembali", UITheme.OCEAN_MID, Color.WHITE);
        btnBack.setBounds((W - 180) / 2, gridY + gridH + 18, 180, 46);
        contentPane.add(btnBack);
        btnBack.addActionListener(e -> {
            TransitionManager.fadeOut(this, () -> {
                new MateriForm().setVisible(true);
                dispose();
            });
        });

        // Legenda
        addLegend(gridY + gridH + 75);
    }

    private JPanel createLevelCell(int level, boolean unlocked, int stars, Color accentColor) {
        JPanel cell = new JPanel(null) {
            private boolean hover = false;
            {
                if (unlocked) {
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                        @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
                        @Override public void mouseClicked(MouseEvent e) { openGameplay(level); }
                    });
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth() - 2, h = getHeight() - 4;
                int yo = hover ? 0 : 2;

                // Shadow
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRoundRect(2, yo + 4, w, h, 14, 14);

                if (unlocked) {
                    // Gradien hijau/warna materi
                    GradientPaint gp = new GradientPaint(0, yo, accentColor,
                                                         0, yo + h, accentColor.darker().darker());
                    g2.setPaint(gp);
                } else {
                    // Abu-abu untuk locked
                    g2.setColor(UITheme.LOCK_COLOR);
                }
                g2.fillRoundRect(1, yo, w, h, 14, 14);

                // Border
                g2.setColor(unlocked
                    ? new Color(255, 255, 255, hover ? 120 : 60)
                    : new Color(255, 255, 255, 20));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, yo, w, h, 14, 14);

                if (unlocked) {
                    // Nomor level
                    g2.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD, 20f));
                    FontMetrics fm = g2.getFontMetrics();
                    String numStr = String.valueOf(level);
                    g2.setColor(Color.WHITE);
                    g2.drawString(numStr, (getWidth() - fm.stringWidth(numStr)) / 2,
                                  yo + 30);

                    // Bintang (3 kecil di bawah)
                    drawMiniStars(g2, stars, getWidth() / 2, yo + h - 12);
                } else {
                    // Ikon gembok
                    g2.setColor(new Color(200, 200, 220, 180));
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 24));
                    FontMetrics fm = g2.getFontMetrics();
                    String lock = "🔒";
                    // Gunakan angka jika emoji tidak tersedia
                    g2.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 11f));
                    fm = g2.getFontMetrics();
                    String ls = String.valueOf(level);
                    g2.setColor(new Color(140, 140, 160, 180));
                    g2.drawString(ls, (getWidth() - fm.stringWidth(ls)) / 2, yo + 24);
                    // Gambar gembok manual
                    drawLockIcon(g2, getWidth()/2, yo + h/2 + 4);
                }

                g2.dispose();
            }
        };
        cell.setOpaque(false);
        return cell;
    }

    private void drawMiniStars(Graphics2D g2, int stars, int cx, int cy) {
        int[] offsets = {-16, 0, 16};
        for (int i = 0; i < 3; i++) {
            Color c = (i < stars) ? UITheme.STAR_ON : new Color(60, 60, 80, 180);
            drawTinyStar(g2, cx + offsets[i], cy, 7, c);
        }
    }

    private void drawTinyStar(Graphics2D g2, int cx, int cy, int r, Color color) {
        int[] xp = new int[10], yp = new int[10];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            int rd = (i % 2 == 0) ? r : r / 2;
            xp[i] = cx + (int)(rd * Math.cos(angle));
            yp[i] = cy + (int)(rd * Math.sin(angle));
        }
        g2.setColor(color);
        g2.fillPolygon(xp, yp, 10);
    }

    private void drawLockIcon(Graphics2D g2, int cx, int cy) {
        // Badan gembok
        g2.setColor(new Color(160, 160, 180, 160));
        g2.fillRoundRect(cx - 8, cy - 2, 16, 12, 4, 4);
        // Lengkungan atas gembok
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawArc(cx - 6, cy - 12, 12, 12, 0, 180);
        // Lubang kunci
        g2.setColor(UITheme.LOCK_COLOR);
        g2.fillOval(cx - 2, cy + 1, 4, 4);
        g2.fillRect(cx - 1, cy + 3, 2, 4);
    }

    private void addLegend(int y) {
        JPanel legend = UITheme.makeGlassPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        legend.setBounds((W - 400) / 2, y, 400, 38);
        contentPane.add(legend);

        // Contoh level terbuka
        JLabel l1 = new JLabel("■ Terbuka");
        l1.setForeground(LevelGenerator.getMateriColor(materi));
        l1.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f));
        legend.add(l1);

        JLabel l2 = new JLabel("■ Terkunci");
        l2.setForeground(new Color(120, 120, 140));
        l2.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f));
        legend.add(l2);

        JLabel l3 = new JLabel("★ = Bintang");
        l3.setForeground(UITheme.STAR_ON);
        l3.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f));
        legend.add(l3);
    }

    private void openGameplay(int level) {
        TransitionManager.fadeOut(this, () -> {
            new GameplayForm(materi, level).setVisible(true);
            dispose();
        });
    }
}