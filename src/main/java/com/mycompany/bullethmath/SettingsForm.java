package com.mycompany.bullethmath;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

/**
 * SettingsForm.java — Pengaturan volume & suara, terhubung ke SoundManager.
 */
public class SettingsForm extends JDialog {

    public SettingsForm(JFrame parent) {
        super(parent, "⚙️ Pengaturan", true);
        setSize(440, 360);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        SoundManager sm = SoundManager.getInstance();

        // Background panel dengan tema laut
        JPanel main = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.drawOceanBg(g2, getWidth(), getHeight());
                g2.dispose();
            }
        };
        main.setOpaque(true);
        setContentPane(main);

        // Panel kaca
        JPanel glass = UITheme.makeDarkGlassPanel(null);
        glass.setBounds(20, 16, 400, 310);
        main.add(glass);

        // ── Judul ─────────────────────────────────────────────────────────────
        JLabel title = new JLabel("⚙️  Pengaturan", SwingConstants.CENTER);
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.TEXT_DARK);
        title.setBounds(0, 14, 400, 36);
        glass.add(title);

        // ── Volume Musik ──────────────────────────────────────────────────────
        addLabel(glass, "🎵  Volume Musik", 30, 68);
        JSlider sliderBGM = makeSlider((int)(sm.getBGMVolume() * 100));
        sliderBGM.setBounds(30, 90, 260, 28);
        glass.add(sliderBGM);

        JLabel lblBGMVal = makeValLabel((int)(sm.getBGMVolume() * 100) + "%");
        lblBGMVal.setBounds(300, 90, 60, 28);
        glass.add(lblBGMVal);

        JCheckBox chkBGM = makeCheckbox("Aktif", sm.isBGMEnabled());
        chkBGM.setBounds(300, 68, 80, 24);
        glass.add(chkBGM);

        // ── Volume SFX ────────────────────────────────────────────────────────
        addLabel(glass, "🔊  Efek Suara", 30, 136);
        JSlider sliderSFX = makeSlider((int)(sm.getSFXVolume() * 100));
        sliderSFX.setBounds(30, 158, 260, 28);
        glass.add(sliderSFX);

        JLabel lblSFXVal = makeValLabel((int)(sm.getSFXVolume() * 100) + "%");
        lblSFXVal.setBounds(300, 158, 60, 28);
        glass.add(lblSFXVal);

        JCheckBox chkSFX = makeCheckbox("Aktif", sm.isSFXEnabled());
        chkSFX.setBounds(300, 136, 80, 24);
        glass.add(chkSFX);

        // ── Info akun ─────────────────────────────────────────────────────────
        JLabel lblAkun = new JLabel("👤  Login sebagai: " + SessionManager.getUsername());
        lblAkun.setFont(UITheme.SMALL_FONT.deriveFont(Font.ITALIC, 13f));
        lblAkun.setForeground(new Color(40, 120, 90));
        lblAkun.setBounds(30, 204, 340, 24);
        glass.add(lblAkun);

        // ── Tombol ────────────────────────────────────────────────────────────
        JButton btnSave = UITheme.makeButton("💾  Simpan", UITheme.SEA_GREEN, Color.WHITE);
        btnSave.setBounds(30, 244, 160, 48);
        glass.add(btnSave);

        JButton btnClose = UITheme.makeButton("✖  Tutup", UITheme.CRIMSON, Color.WHITE);
        btnClose.setBounds(210, 244, 160, 48);
        glass.add(btnClose);

        // ── Events ────────────────────────────────────────────────────────────
        sliderBGM.addChangeListener((ChangeEvent e) -> {
            int v = sliderBGM.getValue();
            lblBGMVal.setText(v + "%");
            sm.setBGMVolume(v / 100f);
        });

        sliderSFX.addChangeListener((ChangeEvent e) -> {
            int v = sliderSFX.getValue();
            lblSFXVal.setText(v + "%");
            sm.setSFXVolume(v / 100f);
        });

        chkBGM.addActionListener(e -> sm.setBGMEnabled(chkBGM.isSelected()));
        chkSFX.addActionListener(e -> sm.setSFXEnabled(chkSFX.isSelected()));

        btnSave.addActionListener(e -> {
            sm.play("click");
            JOptionPane.showMessageDialog(this, "Pengaturan disimpan!",
                "Info", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });
        btnClose.addActionListener(e -> dispose());
    }

    private void addLabel(JPanel panel, String text, int x, int y) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 14f));
        lbl.setForeground(UITheme.TEXT_SOFT);
        lbl.setBounds(x, y, 260, 24);
        panel.add(lbl);
    }

    private JSlider makeSlider(int value) {
        JSlider s = new JSlider(0, 100, value);
        s.setOpaque(false);
        s.setForeground(UITheme.OCEAN_MID);
        s.setPaintTicks(false);
        s.setPaintLabels(false);
        return s;
    }

    private JLabel makeValLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f));
        lbl.setForeground(UITheme.TEXT_DARK);
        return lbl;
    }

    private JCheckBox makeCheckbox(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f));
        cb.setForeground(UITheme.TEXT_SOFT);
        cb.setOpaque(false);
        return cb;
    }
}
