/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bulletmath;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
 
/**
 * DatabaseConnection — Kelas untuk mengelola koneksi ke database MySQL.
 *
 * CARA SETUP SEBELUM MENJALANKAN:
 *   1. Buka XAMPP Control Panel → Start: Apache + MySQL
 *   2. Import file db_game_anak.sql ke phpMyAdmin
 *   3. Di NetBeans: klik kanan project → Properties → Libraries
 *      → Add JAR/Folder → pilih mysql-connector-j-x.x.x.jar
 *
 * POLA: Singleton — hanya ada satu koneksi aktif di seluruh aplikasi.
 */
public class DatabaseConnection {
 
    // ── Konfigurasi koneksi database ──────────────────────────────────────────
    //    Sesuaikan jika XAMPP kamu menggunakan port atau password berbeda.
 
    private static final String HOST     = "localhost";      // Alamat server MySQL
    private static final String PORT     = "3306";           // Port default MySQL
    private static final String DATABASE = "db_game_anak";   // Nama database
    private static final String USER     = "root";           // Username XAMPP default
    private static final String PASSWORD = "";               // Password XAMPP default (kosong)
 
    // URL lengkap untuk koneksi JDBC
    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
        + "?useSSL=false"
        + "&serverTimezone=UTC"
        + "&allowPublicKeyRetrieval=true";
 
    // Menyimpan satu objek koneksi (Singleton)
    private static Connection conn = null;
 
    // ── Konstruktor private — kelas ini tidak boleh di-instantiate ────────────
    private DatabaseConnection() {}
 
    /**
     * Mendapatkan koneksi database.
     * Jika koneksi belum ada atau sudah tertutup, buat koneksi baru.
     *
     * Contoh penggunaan:
     *   Connection c = DatabaseConnection.getConnection();
     *
     * @return objek Connection yang aktif
     * @throws SQLException jika koneksi gagal
     */
    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            try {
                // Langkah 1: Muat driver MySQL ke memori
                Class.forName("com.mysql.cj.jdbc.Driver");
 
                // Langkah 2: Buka koneksi ke database
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
 
                System.out.println("✅ Koneksi ke database berhasil!");
 
            } catch (ClassNotFoundException e) {
                // Driver tidak ditemukan → JAR belum ditambahkan ke project
                throw new SQLException(
                    "❌ Driver MySQL tidak ditemukan!\n" +
                    "   Pastikan mysql-connector-j.jar sudah ditambahkan\n" +
                    "   ke Libraries proyek NetBeans.", e
                );
            }
        }
        return conn;
    }
 
    /**
     * Menutup koneksi database dengan aman.
     * Panggil metode ini saat aplikasi ditutup (di WindowListener).
     */
    public static void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                conn = null;
                System.out.println("🔒 Koneksi database ditutup.");
            }
        } catch (SQLException e) {
            System.err.println("⚠ Gagal menutup koneksi: " + e.getMessage());
        }
    }
 
    /**
     * Mengecek apakah koneksi sedang aktif.
     * Berguna untuk debug.
     *
     * @return true jika terhubung, false jika tidak
     */
    public static boolean isConnected() {
        try {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
 