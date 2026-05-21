
package com.mycompany.bullethmath;

import java.sql.*;

public class DatabaseConnection {
    private static Connection conn = null;

    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection("jdbc:sqlite:game_data.db");
            
            // Buat tabel kalau belum ada
            Statement stmt = conn.createStatement();
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "level INTEGER NOT NULL DEFAULT 1)"
            );
            System.out.println("✅ Koneksi SQLite berhasil!");
        }
        return conn;
    }

    public static void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                conn = null;
            }
        } catch (SQLException e) {
            System.err.println("⚠ Gagal menutup koneksi: " + e.getMessage());
        }
    }

    public static boolean isConnected() {
        try { return conn != null && !conn.isClosed(); }
        catch (SQLException e) { return false; }
    }
}