package com.mycompany.bullethmath;

import javax.swing.*;

/**
 * UnderwaterMathGame.java
 * Entry point aplikasi.
 * Alur: main() -> DatabaseManager -> SessionManager -> LoginForm
 */
public class UnderwaterMathGame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // 1. Buat DatabaseManager (koneksi ke math_adventure.db)
            DatabaseManager db = new DatabaseManager();

            // 2. Simpan ke SessionManager agar bisa diakses dari semua form
            SessionManager.setDb(db);

            // 3. Buka LoginForm
            new LoginForm().setVisible(true);
        });
    }
}
