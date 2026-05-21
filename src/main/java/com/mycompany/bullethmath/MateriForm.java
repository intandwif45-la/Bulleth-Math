package com.mycompany.bullethmath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * MateriForm.java — Pilih materi belajar.
 * Kartu materi menggunakan ukuran & style yang SAMA dengan Dashboard.
 */
public class MateriForm extends BaseForm {

    private static final String[] MATER_LIST  = {
        DatabaseManager.PENJUMLAHAN,
        DatabaseManager.PENGURANGAN,
        DatabaseManager.PERKALIAN,
        DatabaseManager.PEMBAGIAN
    };
    private static final String[] MATER_NAME  = {
        "Penjumlahan", "Pengurangan", "Perkalian", "Pembagian"
    };
    private static final String[] MATER_EMOJI = { "➕", "➖", "✖", "➗" };
    private static final Color[]  CARD_COLORS = {
        new Color(35, 165, 95),
        new Color(215, 75, 55),
        new Color(195, 155, 0),
        new Color(45, 135, 215)
    };

    public MateriForm() {
        super("Math Adventure - Pilih Materi", 900, 620);
        buildUI();
        SwingUtilities.invokeLater(() -> TransitionManager.fadeIn(this));
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        int headerTop = (int)(H * 0.04);

        JLabel lblTitle = makeLabel("📚  Pilih Materi Belajar",
                UITheme.TITLE_FONT.deriveFont(34f), UITheme.GOLD);
        place(lblTitle, 0, headerTop, W, 48);

        JLabel lblSub = makeLabel("Pilih materi yang ingin kamu latih hari ini!",
                UITheme.BODY_FONT, UITheme.OCEAN_SURF);
        place(lblSub, 0, headerTop + 52, W, 28);

        // ── 4 Kartu Materi — ukuran SAMA dengan Dashboard ─────────────────────
        int cardW   = DashboardForm.CARD_W;
        int cardH   = DashboardForm.CARD_H;
        int gap     = DashboardForm.CARD_GAP;
        int totalW  = 4 * cardW + 3 * gap;
        int startX  = (W - totalW) / 2;
        int cardY   = headerTop + 100;

        for (int i = 0; i < 4; i++) {
            final String materi     = MATER_LIST[i];
            final Color  cardColor  = CARD_COLORS[i];
            final String emoji      = MATER_EMOJI[i];
            final String name       = MATER_NAME[i];

            JPanel card = createMateriCard(emoji, name, cardColor, materi);
            card.setBounds(startX + i * (cardW + gap), cardY, cardW, cardH);
            contentPane.add(card);
        }

        // ── Tombol Kembali — di bawah kartu dengan jarak cukup ────────────────
        int btnY = cardY + cardH + 28;
        JButton btnBack = UITheme.makeButton("◀  Kembali", UITheme.OCEAN_MID, Color.WHITE);
        btnBack.setBounds((W - 180) / 2, btnY, 180, 50);
        contentPane.add(btnBack);
        btnBack.addActionListener(e -> {
            new DashboardForm().setVisible(true);
            dispose();
        });

        // ── Motivasi ──────────────────────────────────────────────────────────
        JLabel lblMotiv = makeLabel(
                "🌟  Terus berlatih untuk membuka semua level!",
                UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f),
                UITheme.OCEAN_SURF);
        place(lblMotiv, 0, btnY + 60, W, 22);
    }

    // ── Kartu materi — style identik dengan kartu Dashboard ──────────────────
    private JPanel createMateriCard(String emoji, String name,
                                     Color cardColor, String materi) {
        JPanel card = new JPanel(null) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                    @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) { openLevel(materi); }
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

                int w = getWidth(), h = getHeight();
                int yo = hover ? 0 : 4;

                // Pakai paintCard yang sama dengan Dashboard
                DashboardForm.paintCard(g2, w, h, cardColor, emoji, name, hover);

                // ── Tambahan khusus MateriForm: progress bar di bawah label ──
                DatabaseManager db = SessionManager.getDb();
                int userId = SessionManager.getUserId();
                int lv = db.getLevelTerbuka(userId, materi);
                float prog = Math.min((lv - 1) / 50f, 1f);

                // Teks level
                g2.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 11f));
                FontMetrics fm = g2.getFontMetrics();
                String lvStr = "Level " + lv + " / 50";
                int lx = (w - fm.stringWidth(lvStr)) / 2;
                g2.setColor(new Color(255, 255, 255, 195));
                g2.drawString(lvStr, lx, yo + 210);

                // Progress bar
                int barW = w - 44, barH = 8;
                int barX = 22, barY = yo + 216;
                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillRoundRect(barX, barY, barW, barH, barH, barH);
                if (prog > 0) {
                    g2.setColor(new Color(255, 255, 255, 190));
                    g2.fillRoundRect(barX, barY, (int)(barW * prog), barH, barH, barH);
                }

                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    private void openLevel(String materi) {
        new LevelForm(materi).setVisible(true);
        dispose();
    }
}
