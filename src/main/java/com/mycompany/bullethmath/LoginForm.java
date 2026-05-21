package com.mycompany.bullethmath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * LoginForm.java
 * Halaman login dengan tema laut yang ceria.
 */
public class LoginForm extends BaseForm {

    private final AuthManager auth;

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JLabel         lblStatus;

    public LoginForm() {
        super("Math Adventure - Login", 820, 600);
        this.auth = new AuthManager(SessionManager.getDb());
        buildUI();
        SwingUtilities.invokeLater(() -> TransitionManager.fadeIn(this));
    }

    private void buildUI() {
    // ── Judul ─────────────────────────────────────────────────────────────
    JLabel lblTitle = new JLabel("🌊 BullethMath 🌊", SwingConstants.CENTER) {
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        // Warna judul: navy ke turquoise, lebih nyatu dengan tema laut
        GradientPaint gp = new GradientPaint(
            0, 0, new Color(18, 74, 120),
            getWidth(), 0, new Color(16, 145, 160)
        );

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;

        // Bayangan lembut supaya teks makin kebaca
        g2.setColor(new Color(255, 255, 255, 120));
        g2.drawString(getText(), x + 2, getFont().getSize() + 2);

        // Teks utama
        g2.setPaint(gp);
        g2.drawString(getText(), x, getFont().getSize());

        g2.dispose();
    }
};
        lblTitle.setFont(UITheme.TITLE_FONT.deriveFont(40f));
        lblTitle.setOpaque(false);
        place(lblTitle, 0, 30, W, 55);

        JLabel lblSub = makeLabel("Belajar Matematika Seru Bersama Teman Laut!",
                                   UITheme.BODY_FONT, UITheme.OCEAN_SURF);
        place(lblSub, 0, 82, W, 30);

        // ── Panel Kaca ────────────────────────────────────────────────────────
        JPanel glass = UITheme.makeDarkGlassPanel(null);
        int gw = 400, gh = 360;
        int gx = (W - gw) / 2, gy = 130;
        glass.setBounds(gx, gy, gw, gh);
        contentPane.add(glass);

        // Label "Masuk"
        JLabel lblMasuk = new JLabel("🔑 Masuk", SwingConstants.CENTER);
        lblMasuk.setFont(UITheme.HEADER_FONT);
        lblMasuk.setForeground(UITheme.TEXT_DARK);
        lblMasuk.setOpaque(false);
        lblMasuk.setBounds(0, 18, gw, 38);
        glass.add(lblMasuk);

        // Username
        JLabel lblU = new JLabel("  👤  Username");
        lblU.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 14f));
        lblU.setForeground(UITheme.TEXT_SOFT);
        lblU.setBounds(30, 68, gw - 60, 24);
        glass.add(lblU);

        txtUsername = UITheme.makeField();
        txtUsername.setBounds(30, 94, gw - 60, 46);
        glass.add(txtUsername);

        // Password
        JLabel lblP = new JLabel("  🔒  Password");
        lblP.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 14f));
        lblP.setForeground(UITheme.TEXT_SOFT);
        lblP.setBounds(30, 154, gw - 60, 24);
        glass.add(lblP);

        txtPassword = UITheme.makePassField();
        txtPassword.setBounds(30, 180, gw - 60, 46);
        glass.add(txtPassword);

        // Status
        lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f));
        lblStatus.setForeground(UITheme.CORAL);
        lblStatus.setBounds(10, 236, gw - 20, 24);
        glass.add(lblStatus);

        // Tombol Login
        JButton btnLogin = UITheme.makeButton("🌊  MASUK", UITheme.OCEAN_LIGHT, Color.WHITE);
        btnLogin.setBounds(30, 268, gw - 60, 52);
        glass.add(btnLogin);

        // Tombol Register
        JButton btnRegister = UITheme.makeButton("📝  DAFTAR BARU", UITheme.CORAL, Color.WHITE);
        int rgy = gy + gh + 18;
        btnRegister.setBounds((W - 240) / 2, rgy, 240, 48);
        contentPane.add(btnRegister);

        JLabel lblHint = makeLabel("Belum punya akun? Klik DAFTAR BARU",
                                    UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f),
                                    UITheme.TEXT_DARK);
        place(lblHint, 0, rgy + 54, W, 24);

        // ── Dekorasi ikan kecil di pojok ──────────────────────────────────────
        // (ditangani di drawExtras)

        // ── Events ───────────────────────────────────────────────────────────
        btnLogin.addActionListener(e -> doLogin());
        btnRegister.addActionListener(e -> openRegister());

        // Enter untuk login
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        txtUsername.addKeyListener(enter);
        txtPassword.addKeyListener(enter);
    }

    @Override
    protected void drawExtras(Graphics2D g2) {
        // Gambar ikan kecil dekoratif di sudut
        drawFish(g2, 60, 520, 1);
        drawFish(g2, W - 100, 490, -1);
        drawFish(g2, 180, 560, 1);
    }

    private void drawFish(Graphics2D g2, int x, int y, int dir) {
        g2.setColor(new Color(255, 180, 50, 160));
        int[] fishX, fishY;
        if (dir > 0) {
            fishX = new int[]{x, x+30, x+20, x+20, x+30};
            fishY = new int[]{y, y-10, y-10, y+10, y+10};
            g2.fillOval(x, y - 8, 28, 16);
            fishX = new int[]{x, x-12, x-12}; fishY = new int[]{y, y-8, y+8};
        } else {
            g2.fillOval(x, y - 8, 28, 16);
            fishX = new int[]{x+28, x+40, x+40}; fishY = new int[]{y, y-8, y+8};
        }
        g2.setColor(new Color(255, 220, 100, 180));
        g2.fillPolygon(fishX, fishY, 3);
        // Mata
        int ex = dir > 0 ? x + 22 : x + 5;
        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillOval(ex, y - 3, 4, 4);
    }

    private void doLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            showStatus("Username dan password tidak boleh kosong!", UITheme.CORAL);
            return;
        }

        AuthManager.AuthResult result = auth.login(user, pass);
        if (result.success) {
            showStatus(result.message, UITheme.SEA_GREEN);
            Timer t = new Timer(400, ev -> {
                TransitionManager.fadeOut(this, () -> {
                    new DashboardForm().setVisible(true);
                    dispose();
                });
            });
            t.setRepeats(false);
            t.start();
        } else {
            showStatus(result.message, UITheme.CORAL);
            txtPassword.setText("");
        }
    }

    private void openRegister() {
        new RegisterForm(this).setVisible(true);
    }

    private void showStatus(String msg, Color color) {
        // Cari lblStatus di glass panel
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }
}