package com.mycompany.bullethmath;


/**
 * AuthManager.java
 * Layer validasi antara UI dan DatabaseManager.
 */
public class AuthManager {

    private final DatabaseManager db;

    public AuthManager(DatabaseManager db) {
        this.db = db;
    }

    // ── Hasil operasi auth ────────────────────────────────────────────────────
    public static class AuthResult {
        public final boolean   success;
        public final String    message;
        public final UserModel user;

        public AuthResult(boolean success, String message) {
            this(success, message, null);
        }
        public AuthResult(boolean success, String message, UserModel user) {
            this.success = success;
            this.message = message;
            this.user    = user;
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────
    public AuthResult register(String username, String password, String confirmPassword) {
        if (username == null || username.trim().isEmpty())
            return new AuthResult(false, "Username tidak boleh kosong!");
        if (username.trim().length() < 3)
            return new AuthResult(false, "Username minimal 3 karakter!");
        if (!username.trim().matches("[a-zA-Z0-9_]+"))
            return new AuthResult(false, "Username hanya boleh huruf, angka, dan underscore (_).");
        if (password == null || password.isEmpty())
            return new AuthResult(false, "Password tidak boleh kosong!");
        if (password.length() < 6)
            return new AuthResult(false, "Password minimal 6 karakter!");
        if (!password.equals(confirmPassword))
            return new AuthResult(false, "Konfirmasi password tidak cocok!");

        boolean ok = db.registerUser(username.trim(), password);
        return ok
            ? new AuthResult(true,  "Registrasi berhasil! Silakan login.")
            : new AuthResult(false, "Username sudah dipakai. Coba username lain.");
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    public AuthResult login(String username, String password) {
        if (username == null || username.trim().isEmpty())
            return new AuthResult(false, "Username tidak boleh kosong!");
        if (password == null || password.isEmpty())
            return new AuthResult(false, "Password tidak boleh kosong!");

        UserModel user = db.loginUser(username.trim(), password);
        if (user != null) {
            SessionManager.login(user);
            return new AuthResult(true, "Selamat datang, " + user.getUsername() + "! 🌊", user);
        }
        return new AuthResult(false, "Username atau password salah.");
    }
}