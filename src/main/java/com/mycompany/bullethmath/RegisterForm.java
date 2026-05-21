package com.mycompany.bullethmath;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * RegisterForm.java
 * Dialog registrasi yang muncul di atas LoginForm.
 */
public class RegisterForm extends JDialog {

    private final AuthManager  auth;
    private final LoginForm    parent;

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirm;
    private JLabel         lblStatus;

    public RegisterForm(LoginForm parent) {
        super(parent, "Daftar Akun Baru", true);
        this.parent = parent;
        this.auth   = new AuthManager(SessionManager.getDb());
        setSize(460, 520);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        // Background panel — sama persis dengan LoginForm
        JPanel bg = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.drawOceanBg(g2, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bg.setOpaque(true);
        setContentPane(bg);

        int gw = 400, gh = 470;
        int gx = (460 - gw) / 2;
        int gy = 18;

        // Panel kaca — pakai makeDarkGlassPanel agar mirip LoginForm
        JPanel glass = UITheme.makeDarkGlassPanel(null);
        glass.setBounds(gx, gy, gw, gh);
        bg.add(glass);

        // ── Judul ─────────────────────────────────────────────────────────
        JLabel lbl = new JLabel("🐠  Daftar Akun Baru", SwingConstants.CENTER);
        lbl.setFont(UITheme.HEADER_FONT);
        lbl.setForeground(UITheme.TEXT_DARK);
        lbl.setOpaque(false);
        lbl.setBounds(0, 16, gw, 38);
        glass.add(lbl);

        // ── Username ───────────────────────────────────────────────────────
        addLabel(glass, "  👤  Username", 30, 68, gw);
        txtUsername = UITheme.makeField();
        txtUsername.setBounds(30, 92, gw - 60, 46);
        glass.add(txtUsername);

        // ── Password ───────────────────────────────────────────────────────
        addLabel(glass, "  🔒  Password", 30, 152, gw);
        txtPassword = UITheme.makePassField();
        txtPassword.setBounds(30, 176, gw - 60, 46);
        glass.add(txtPassword);

        // ── Konfirmasi Password ────────────────────────────────────────────
        addLabel(glass, "  🔒  Ulangi Password", 30, 236, gw);
        txtConfirm = UITheme.makePassField();
        txtConfirm.setBounds(30, 260, gw - 60, 46);
        glass.add(txtConfirm);

        // ── Status ─────────────────────────────────────────────────────────
        lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 13f));
        lblStatus.setForeground(UITheme.CORAL);
        lblStatus.setBounds(10, 316, gw - 20, 24);
        glass.add(lblStatus);

        // ── Tombol ─────────────────────────────────────────────────────────
        int btnW = 160, btnH = 50;
        int btnY = 350;
        JButton btnDaftar = UITheme.makeButton("✅  DAFTAR", UITheme.SEA_GREEN, Color.WHITE);
        btnDaftar.setBounds(30, btnY, btnW, btnH);
        glass.add(btnDaftar);

        JButton btnBatal = UITheme.makeButton("✖  BATAL", UITheme.CORAL, Color.WHITE);
        btnBatal.setBounds(gw - 30 - btnW, btnY, btnW, btnH);
        glass.add(btnBatal);

        // ── Events ─────────────────────────────────────────────────────────
        btnDaftar.addActionListener(e -> doRegister());
        btnBatal.addActionListener(e -> dispose());

        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doRegister();
            }
        };
        txtUsername.addKeyListener(enter);
        txtPassword.addKeyListener(enter);
        txtConfirm.addKeyListener(enter);
    }

    private void addLabel(JPanel panel, String text, int x, int y, int gw) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.SMALL_FONT.deriveFont(Font.BOLD, 14f));
        lbl.setForeground(UITheme.TEXT_SOFT);
        lbl.setOpaque(false);
        lbl.setBounds(x, y, gw - x * 2, 24);
        panel.add(lbl);
    }

    private void doRegister() {
        String user    = txtUsername.getText().trim();
        String pass    = new String(txtPassword.getPassword());
        String confirm = new String(txtConfirm.getPassword());

        AuthManager.AuthResult result = auth.register(user, pass, confirm);
        if (result.success) {
            lblStatus.setForeground(UITheme.SEA_GREEN);
            lblStatus.setText("✅ " + result.message);
            Timer t = new Timer(1200, ev -> dispose());
            t.setRepeats(false);
            t.start();
        } else {
            lblStatus.setForeground(UITheme.CORAL);
            lblStatus.setText("❌ " + result.message);
            txtPassword.setText("");
            txtConfirm.setText("");
        }
    }
}