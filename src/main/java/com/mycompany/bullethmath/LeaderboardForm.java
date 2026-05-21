package com.mycompany.bullethmath;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * LeaderboardForm.java
 * Menampilkan papan peringkat (leaderboard) per materi.
 */
public class LeaderboardForm extends JDialog {

    private static final String[] MATEIRS = {
        DatabaseManager.PENJUMLAHAN,
        DatabaseManager.PENGURANGAN,
        DatabaseManager.PERKALIAN,
        DatabaseManager.PEMBAGIAN
    };
    private static final String[] LABELS = {
        "Penjumlahan", "Pengurangan", "Perkalian", "Pembagian"
    };

    private final DatabaseManager db;

    public LeaderboardForm(JFrame parent) {
        super(parent, "🏆 Leaderboard", true);
        this.db = SessionManager.getDb();
        setSize(600, 520);
        setLocationRelativeTo(parent);
        setUndecorated(false);
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(new Color(10, 40, 80));
        main.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Judul
        JLabel title = new JLabel("🏆  Papan Peringkat", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(255, 215, 0));
        main.add(title, BorderLayout.NORTH);

        // Tab panel per materi
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(new Color(10, 40, 80));
        tabs.setForeground(Color.WHITE);

        for (int i = 0; i < MATEIRS.length; i++) {
            tabs.addTab(LABELS[i], buildMateriPanel(MATEIRS[i]));
        }
        main.add(tabs, BorderLayout.CENTER);

        // Tombol tutup
        JButton btnClose = new JButton("✖  Tutup");
        btnClose.setBackground(new Color(180, 60, 60));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnClose.setFocusPainted(false);
        btnClose.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btnClose.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(new Color(10, 40, 80));
        btnPanel.add(btnClose);
        main.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(main);
    }

    private JPanel buildMateriPanel(String materi) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(15, 50, 90));

        List<DatabaseManager.LeaderboardEntry> entries = db.getLeaderboard(materi, 10);

        String[] cols = {"Rank", "Username", "Total Skor"};
        Object[][] data = new Object[entries.size()][3];
        for (int i = 0; i < entries.size(); i++) {
            DatabaseManager.LeaderboardEntry e = entries.get(i);
            data[i][0] = "#" + e.rank;
            data[i][1] = e.username;
            data[i][2] = e.totalScore;
        }

        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table.setBackground(new Color(15, 50, 90));
        table.setForeground(Color.WHITE);
        table.setFont(new Font("Consolas", Font.PLAIN, 15));
        table.setRowHeight(32);
        table.getTableHeader().setBackground(new Color(0, 80, 140));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        table.setGridColor(new Color(40, 80, 120));

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        if (entries.isEmpty()) {
            JLabel noData = new JLabel("Belum ada data leaderboard.", SwingConstants.CENTER);
            noData.setForeground(new Color(150, 180, 220));
            noData.setFont(new Font("SansSerif", Font.ITALIC, 14));
            panel.add(noData, BorderLayout.NORTH);
        }

        return panel;
    }
}
