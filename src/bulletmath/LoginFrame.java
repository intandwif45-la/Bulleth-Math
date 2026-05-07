package bulletmath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * LoginFrame — Layar login bertema lautan untuk Ocean Math Adventure.
 *
 * Fitur:
 *   • Form login dengan username & password
 *   • Form registrasi (tab berganti)
 *   • Verifikasi ke database MySQL via UserDAO
 *   • Animasi gelembung dan ikan renang
 *   • Setelah login berhasil, buka GameFrame dengan level tersimpan
 */
public class LoginFrame extends JFrame implements ActionListener {

    // ── Warna tema lautan ─────────────────────────────────────────────────────
    private static final Color OCEAN_DEEP    = new Color(4,  13,  26);
    private static final Color OCEAN_MID     = new Color(10, 36,  64);
    private static final Color OCEAN_LIGHT   = new Color(21, 90, 138);
    private static final Color AQUA          = new Color(0,  180, 166);
    private static final Color GOLD          = new Color(249,231,  79);
    private static final Color CORAL_RED     = new Color(231, 76,  60);
    private static final Color TEXT_LIGHT    = new Color(174,214,241);
    private static final Color TEXT_MUTED    = new Color(127,179,211);
    private static final Color PANEL_BG      = new Color(10, 30, 60, 200);
    private static final Color FIELD_BG      = new Color(255,255,255, 25);
    private static final Color FIELD_BORDER  = new Color(100,180,220, 90);

    // ── Komponen UI ───────────────────────────────────────────────────────────
    private JTextField     tfUsername;
    private JPasswordField pfPassword;
    private JTextField     tfRegUser;
    private JPasswordField pfRegPass;
    private JPasswordField pfRegPass2;
    private JButton        btnLogin;
    private JButton        btnRegister;
    private JButton        btnSwitchToReg;
    private JButton        btnSwitchToLogin;
    private JLabel         lblMessage;

    private boolean showingLogin = true;   // true = panel login; false = panel registrasi
    private OceanBackground bgPanel;       // Panel latar belakang animasi

    // ── Konstruktor ───────────────────────────────────────────────────────────
    public LoginFrame() {
        setTitle("Ocean Math Adventure — Login");
        setSize(480, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Tutup koneksi database saat jendela ditutup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseConnection.closeConnection();
            }
        });

        buildUI();
    }

    // ── Bangun antarmuka ──────────────────────────────────────────────────────
    private void buildUI() {
        // Background animasi lautan
        bgPanel = new OceanBackground();
        bgPanel.setLayout(null);
        setContentPane(bgPanel);

        int W = 480, H = 580;

        // ── Logo & Judul ──────────────────────────────────────────────────────
        JLabel lblLogo = new JLabel(createFishIcon(), SwingConstants.CENTER);
        lblLogo.setBounds(190, 28, 100, 70);
        bgPanel.add(lblLogo);

        JLabel lblTitle = makeLabel("Ocean Math Adventure", 20, Font.BOLD, GOLD);
        lblTitle.setBounds(40, 98, W - 80, 30);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        bgPanel.add(lblTitle);

        JLabel lblSub = makeLabel("Petualangan Matematika Lautan", 12, Font.PLAIN, TEXT_MUTED);
        lblSub.setBounds(40, 128, W - 80, 20);
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        bgPanel.add(lblSub);

        // ── Panel kartu login / registrasi ────────────────────────────────────
        buildLoginPanel(W, H);
        buildRegisterPanel(W, H);

        // ── Pesan status ──────────────────────────────────────────────────────
        lblMessage = makeLabel("", 12, Font.BOLD, CORAL_RED);
        lblMessage.setBounds(60, H - 38, W - 120, 22);
        lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
        bgPanel.add(lblMessage);

        showLoginPanel();
    }

    // ── Panel Login ───────────────────────────────────────────────────────────
    private void buildLoginPanel(int W, int H) {
        int px = 70, py = 162, pw = W - 140, ph = 280;

        // Kartu semi-transparan
        RoundPanel card = new RoundPanel(PANEL_BG, 18);
        card.setLayout(null);
        card.setBounds(px, py, pw, ph);
        bgPanel.add(card);

        JLabel lbl = makeLabel("Masuk ke Akunmu", 14, Font.BOLD, TEXT_LIGHT);
        lbl.setBounds(0, 14, pw, 22);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lbl);

        // Username
        JLabel lUser = makeLabel("Nama Pengguna", 11, Font.PLAIN, TEXT_MUTED);
        lUser.setBounds(20, 52, pw - 40, 18);
        card.add(lUser);

        tfUsername = makeField();
        tfUsername.setBounds(20, 70, pw - 40, 38);
        card.add(tfUsername);

        // Password
        JLabel lPass = makeLabel("Password", 11, Font.PLAIN, TEXT_MUTED);
        lPass.setBounds(20, 120, pw - 40, 18);
        card.add(lPass);

        pfPassword = makePasswordField();
        pfPassword.setBounds(20, 138, pw - 40, 38);
        pfPassword.addActionListener(this);
        card.add(pfPassword);

        // Tombol Dive In
        btnLogin = makeButton("Dive In! ▶", AQUA, Color.WHITE, 16);
        btnLogin.setBounds(20, 195, pw - 40, 46);
        btnLogin.addActionListener(this);
        card.add(btnLogin);

        btnSwitchToReg = makeLinkButton("Belum punya akun? Daftar");
        btnSwitchToReg.setBounds(0, 250, pw, 22);
        btnSwitchToReg.addActionListener(this);
        card.add(btnSwitchToReg);
    }

    // ── Panel Registrasi ──────────────────────────────────────────────────────
    private void buildRegisterPanel(int W, int H) {
        int px = 70, py = 152, pw = W - 140, ph = 330;

        RoundPanel card = new RoundPanel(PANEL_BG, 18);
        card.setLayout(null);
        card.setBounds(px, py, pw, ph);
        card.setVisible(false);
        bgPanel.add(card);
        // simpan referensi untuk toggle
        card.setName("regCard");

        JLabel lbl = makeLabel("Buat Akun Baru", 14, Font.BOLD, TEXT_LIGHT);
        lbl.setBounds(0, 14, pw, 22);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lbl);

        JLabel lUser = makeLabel("Nama Pengguna", 11, Font.PLAIN, TEXT_MUTED);
        lUser.setBounds(20, 48, pw - 40, 18);
        card.add(lUser);

        tfRegUser = makeField();
        tfRegUser.setBounds(20, 66, pw - 40, 38);
        card.add(tfRegUser);

        JLabel lPass = makeLabel("Password", 11, Font.PLAIN, TEXT_MUTED);
        lPass.setBounds(20, 116, pw - 40, 18);
        card.add(lPass);

        pfRegPass = makePasswordField();
        pfRegPass.setBounds(20, 134, pw - 40, 38);
        card.add(pfRegPass);

        JLabel lPass2 = makeLabel("Ulangi Password", 11, Font.PLAIN, TEXT_MUTED);
        lPass2.setBounds(20, 184, pw - 40, 18);
        card.add(lPass2);

        pfRegPass2 = makePasswordField();
        pfRegPass2.setBounds(20, 202, pw - 40, 38);
        card.add(pfRegPass2);

        btnRegister = makeButton("Daftar Sekarang!", new Color(39,174,96), Color.WHITE, 14);
        btnRegister.setBounds(20, 256, pw - 40, 44);
        btnRegister.addActionListener(this);
        card.add(btnRegister);

        btnSwitchToLogin = makeLinkButton("Sudah punya akun? Masuk");
        btnSwitchToLogin.setBounds(0, 308, pw, 22);
        btnSwitchToLogin.addActionListener(this);
        card.add(btnSwitchToLogin);
    }

    // ── Tampilkan panel login ─────────────────────────────────────────────────
    private void showLoginPanel() {
        showingLogin = true;
        for (Component c : bgPanel.getComponents()) {
            if (c instanceof RoundPanel) {
                c.setVisible(!c.getName().equals("regCard"));
            }
        }
        setMessage("", false);
    }

    private void showRegisterPanel() {
        showingLogin = false;
        for (Component c : bgPanel.getComponents()) {
            if (c instanceof RoundPanel) {
                c.setVisible(c.getName().equals("regCard"));
            }
        }
        setMessage("", false);
    }

    // ── Action handler ────────────────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == btnSwitchToReg)   { showRegisterPanel(); return; }
        if (src == btnSwitchToLogin) { showLoginPanel();    return; }

        if (src == btnLogin || src == pfPassword) {
            handleLogin();
        } else if (src == btnRegister) {
            handleRegister();
        }
    }

    // ── Proses Login ──────────────────────────────────────────────────────────
    private void handleLogin() {
        String user = tfUsername.getText().trim();
        String pass = new String(pfPassword.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            setMessage("Username dan password tidak boleh kosong!", true);
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Memeriksa...");

        // Jalankan di thread terpisah agar UI tidak freeze
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                return UserDAO.login(user, pass);
            }

            @Override
            protected void done() {
                try {
                    int[] result = get();
                    if (result != null) {
                        // Login berhasil → buka game
                        int userId = result[0];
                        int level  = result[1];
                        openGame(user, userId, level);
                    } else {
                        setMessage("Username atau password salah!", true);
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Dive In! ▶");
                    }
                } catch (Exception ex) {
                    setMessage("Gagal terhubung ke database!", true);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Dive In! ▶");
                }
            }
        };
        worker.execute();
    }

    // ── Proses Registrasi ─────────────────────────────────────────────────────
    private void handleRegister() {
        String user  = tfRegUser.getText().trim();
        String pass  = new String(pfRegPass.getPassword()).trim();
        String pass2 = new String(pfRegPass2.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            setMessage("Semua kolom harus diisi!", true);
            return;
        }
        if (user.length() < 3) {
            setMessage("Username minimal 3 karakter!", true);
            return;
        }
        if (!pass.equals(pass2)) {
            setMessage("Password tidak cocok!", true);
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Mendaftarkan...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return UserDAO.register(user, pass);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        setMessage("Akun berhasil dibuat! Silakan login.", false);
                        showLoginPanel();
                        tfUsername.setText(user);
                    } else {
                        setMessage("Username sudah dipakai, coba lain!", true);
                    }
                } catch (Exception ex) {
                    setMessage("Gagal terhubung ke database!", true);
                } finally {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Daftar Sekarang!");
                }
            }
        };
        worker.execute();
    }

    // ── Buka game setelah login berhasil ──────────────────────────────────────
    private void openGame(String username, int userId, int startLevel) {
        SwingUtilities.invokeLater(() -> {
            JFrame gameFrame = new JFrame("Ocean Math Adventure — " + username);
            gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            gameFrame.setResizable(false);

            GamePanel panel = new GamePanel(userId, username, startLevel);
            gameFrame.add(panel);
            gameFrame.pack();
            gameFrame.setLocationRelativeTo(null);
            gameFrame.setVisible(true);

            dispose();   // Tutup LoginFrame
        });
    }

    // ── Helper: set pesan status ──────────────────────────────────────────────
    private void setMessage(String msg, boolean isError) {
        lblMessage.setText(msg);
        lblMessage.setForeground(isError ? CORAL_RED : new Color(88,214,141));
    }

    // ── Helper: buat label ────────────────────────────────────────────────────
    private JLabel makeLabel(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Trebuchet MS", style, size));
        l.setForeground(color);
        l.setOpaque(false);
        return l;
    }

    // ── Helper: buat text field bertema lautan ────────────────────────────────
    private JTextField makeField() {
        JTextField f = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
            }
        };
        styleField(f);
        return f;
    }

    private JPasswordField makePasswordField() {
        JPasswordField f = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
            }
        };
        styleField(f);
        return f;
    }

    private void styleField(JTextField f) {
        f.setOpaque(false);
        f.setForeground(new Color(214,234,248));
        f.setCaretColor(AQUA);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        f.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
    }

    // ── Helper: buat tombol ───────────────────────────────────────────────────
    private JButton makeButton(String text, Color bg, Color fg, int fontSize) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Trebuchet MS", Font.BOLD, fontSize));
        b.setForeground(fg);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton makeLinkButton(String text) {
        JButton b = new JButton("<html><u>" + text + "</u></html>");
        b.setForeground(new Color(93,173,226));
        b.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.CENTER);
        return b;
    }

    // ── Ikon ikan sederhana ───────────────────────────────────────────────────
    private ImageIcon createFishIcon() {
        int w = 80, h = 60;
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Lingkaran latar
        g.setColor(new Color(0,180,166,50));
        g.fillOval(0, 0, w, h);

        // Ekor
        int[] tx = {18, 2, 2}, ty = {30, 12, 48};
        g.setColor(new Color(52,152,219));
        g.fillPolygon(tx, ty, 3);

        // Badan ikan
        g.setColor(new Color(52,152,219));
        g.fillOval(14, 14, 52, 32);
        g.setColor(new Color(133,193,233));
        g.fillOval(20, 16, 30, 18);

        // Mata
        g.setColor(Color.WHITE);
        g.fillOval(50, 17, 13, 13);
        g.setColor(new Color(26,37,47));
        g.fillOval(53, 20, 7, 7);
        g.setColor(Color.WHITE);
        g.fillOval(55, 21, 2, 2);

        // Sirip atas
        int[] fx = {30, 24, 44}, fy = {18, 6, 10};
        g.setColor(new Color(41,128,185));
        g.fillPolygon(fx, fy, 3);

        g.dispose();
        return new ImageIcon(img);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Inner class: Latar belakang animasi lautan
    // ══════════════════════════════════════════════════════════════════════════
    private static class OceanBackground extends JPanel {
        private final float[][] bubbles;  // [i][0]=x, [1]=y, [2]=size, [3]=speed
        private float fishX  = 480;
        private float fishX2 = -60;
        private final javax.swing.Timer timer;
        private final java.util.Random rng = new java.util.Random();

        OceanBackground() {
            setOpaque(true);

            // Inisialisasi gelembung acak
            bubbles = new float[14][4];
            for (float[] b : bubbles) {
                b[0] = rng.nextFloat() * 480;
                b[1] = rng.nextFloat() * 580;
                b[2] = 4 + rng.nextFloat() * 10;
                b[3] = 0.4f + rng.nextFloat() * 0.8f;
            }

            // Timer animasi 40fps
            timer = new javax.swing.Timer(25, e -> {
                animateBubbles();
                fishX  -= 0.9f;
                fishX2 += 0.6f;
                if (fishX  < -60)  fishX  = 490;
                if (fishX2 > 490)  fishX2 = -60;
                repaint();
            });
            timer.start();
        }

        private void animateBubbles() {
            for (float[] b : bubbles) {
                b[1] -= b[3];
                if (b[1] < -15) {
                    b[1] = 590;
                    b[0] = rng.nextFloat() * 480;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();

            // Gradien lautan
            GradientPaint ocean = new GradientPaint(
                0, 0, OCEAN_DEEP, 0, H, OCEAN_LIGHT);
            g2.setPaint(ocean);
            g2.fillRect(0, 0, W, H);
            g2.setPaint(null);

            // Sinar cahaya dari atas
            g2.setColor(new Color(255,255,255,7));
            for (int i = 0; i < 5; i++) {
                int lx = 50 + i * 100;
                int[] px = {lx-20, lx+20, lx+60, lx-60};
                int[] py = {0, 0, H, H};
                g2.fillPolygon(px, py, 4);
            }

            // Gelembung
            for (float[] b : bubbles) {
                int a = 80 + rng.nextInt(40);
                g2.setColor(new Color(255,255,255,a));
                g2.drawOval((int)b[0], (int)b[1], (int)b[2], (int)b[2]);
                g2.setColor(new Color(255,255,255,30));
                g2.fillOval((int)b[0]+1, (int)b[1]+1, (int)b[2]-2, (int)b[2]-2);
            }

            // Ikan kecil yang berenang
            drawSwimFish(g2, (int)fishX,  H/3,   new Color(243,156,18), 1);
            drawSwimFish(g2, (int)fishX2, H*2/3, new Color(155,89,182), -1);

            // Lantai lautan + terumbu karang
            drawSeaFloor(g2, W, H);
        }

        private void drawSwimFish(Graphics2D g, int x, int y, Color col, int dir) {
            g.setColor(col);
            // Badan
            g.fillOval(x, y - 8, 40 * dir == 1 ? 40 : -40, 16);
            // Ekor
            if (dir == 1) {
                int[] tx = {x, x-10, x-10};
                int[] ty = {y, y-7, y+7};
                g.fillPolygon(tx, ty, 3);
            } else {
                int[] tx = {x+40, x+50, x+50};
                int[] ty = {y, y-7, y+7};
                g.fillPolygon(tx, ty, 3);
            }
            // Mata
            g.setColor(Color.WHITE);
            int eyeX = dir == 1 ? x+30 : x+10;
            g.fillOval(eyeX, y-5, 8, 8);
            g.setColor(new Color(26,37,47));
            g.fillOval(eyeX+2, y-3, 4, 4);
        }

        private void drawSeaFloor(Graphics2D g, int W, int H) {
            // Dasar laut
            int[] xs = {0, 80, 160, 240, 320, 400, 480, 480, 0};
            int[] ys = {H-38, H-52, H-42, H-58, H-44, H-55, H-40, H, H};
            g.setColor(new Color(10,61,46,180));
            g.fillPolygon(xs, ys, xs.length);

            // Rumput laut (seaweed) di kiri
            g.setColor(new Color(46,204,113));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawSeaweed(g, 30, H-38, 36);
            drawSeaweed(g, 60, H-38, 28);

            // Seaweed kanan
            drawSeaweed(g, 400, H-40, 40);
            drawSeaweed(g, 430, H-40, 30);

            g.setStroke(new BasicStroke(1f));

            // Karang merah kecil di kiri
            drawCoral(g, 120, H-44, new Color(192,57,43));
            // Karang ungu di kanan
            drawCoral(g, 340, H-55, new Color(136,78,160));
        }

        private void drawSeaweed(Graphics2D g, int x, int baseY, int height) {
            g.drawLine(x, baseY, x+4, baseY-height/3);
            g.drawLine(x+4, baseY-height/3, x, baseY-height*2/3);
            g.drawLine(x, baseY-height*2/3, x+4, baseY-height);
        }

        private void drawCoral(Graphics2D g, int x, int y, Color col) {
            g.setColor(col);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x, y, x, y-14);
            g.drawLine(x, y-6, x-8, y-16);
            g.drawLine(x, y-6, x+8, y-16);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Inner class: Panel dengan sudut melengkung dan latar transparan
    // ══════════════════════════════════════════════════════════════════════════
    private static class RoundPanel extends JPanel {
        private final Color bg;
        private final int radius;

        RoundPanel(Color bg, int radius) {
            this.bg     = bg;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.setColor(new Color(100,180,220,70));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
            super.paintComponent(g);
        }
    }
}