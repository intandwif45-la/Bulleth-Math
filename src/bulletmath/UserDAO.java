package bulletmath;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * UserDAO (Data Access Object) — Menangani semua query database untuk tabel users.
 *
 * Kelas ini memisahkan logika database dari logika tampilan (UI).
 * Setiap method melakukan SATU operasi database yang spesifik.
 *
 * Metode yang tersedia:
 *   • login(username, password) → verifikasi login
 *   • register(username, password) → daftar user baru
 *   • updateLevel(userId, newLevel) → simpan level setelah bermain
 *   • getUserLevel(userId) → ambil level user dari DB
 */
public class UserDAO {

    // ── 1. LOGIN ──────────────────────────────────────────────────────────────

    /**
     * Memverifikasi username dan password pengguna.
     *
     * Menggunakan PreparedStatement agar aman dari SQL Injection.
     * (SQL Injection adalah serangan di mana input jahat memanipulasi query SQL.)
     *
     * @param username username yang diinput user
     * @param password password yang diinput user
     * @return int[] berisi { id, level } jika login berhasil; null jika gagal
     */
    public static int[] login(String username, String password) {
        // Query: cari baris yang username DAN password-nya cocok
        String sql = "SELECT id, level FROM users WHERE username = ? AND password = ?";

        // try-with-resources: Connection dan PreparedStatement otomatis ditutup
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Isi placeholder "?" dengan nilai yang aman
            pstmt.setString(1, username);   // ? pertama = username
            pstmt.setString(2, password);   // ? kedua   = password

            // Jalankan query dan ambil hasilnya
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Baris ditemukan → login berhasil
                int id    = rs.getInt("id");
                int level = rs.getInt("level");
                System.out.println("✅ Login berhasil! User: " + username + " | Level: " + level);
                return new int[]{ id, level };  // Kembalikan id dan level
            }

        } catch (SQLException e) {
            System.err.println("❌ Error saat login: " + e.getMessage());
        }

        // Tidak ada baris → username/password salah
        return null;
    }


    // ── 2. REGISTRASI ─────────────────────────────────────────────────────────

    /**
     * Mendaftarkan pengguna baru ke database.
     * Level awal selalu dimulai dari 1.
     *
     * @param username username baru (harus unik)
     * @param password password baru
     * @return true jika berhasil; false jika username sudah dipakai
     */
    public static boolean register(String username, String password) {
        // Langkah 1: Cek apakah username sudah ada
        String checkSql  = "SELECT id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password, level) VALUES (?, ?, 1)";

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Cek username
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    System.out.println("⚠ Username sudah digunakan: " + username);
                    return false;   // Username sudah ada
                }
            }

            // Langkah 2: Masukkan user baru
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.executeUpdate();
                System.out.println("✅ Registrasi berhasil: " + username);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error saat registrasi: " + e.getMessage());
        }

        return false;
    }


    // ── 3. UPDATE LEVEL ───────────────────────────────────────────────────────

    /**
     * Menyimpan level terbaru user ke database.
     *
     * Catatan: level hanya diupdate kalau level BARU lebih tinggi dari yang tersimpan.
     * Ini agar skor terbaik user tidak hilang.
     *
     * Contoh penggunaan:
     *   UserDAO.updateLevel(3, 5);  // Update user id=3 ke level 5
     *
     * @param userId   ID user yang akan diupdate
     * @param newLevel level baru yang dicapai
     */
    public static void updateLevel(int userId, int newLevel) {
        // UPDATE hanya dijalankan jika newLevel > level saat ini di DB
        String sql = "UPDATE users SET level = ? WHERE id = ? AND level < ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newLevel);   // Nilai level baru
            pstmt.setInt(2, userId);     // ID user
            pstmt.setInt(3, newLevel);   // Kondisi: hanya update jika level DB lebih kecil

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Level diperbarui → Level " + newLevel);
            } else {
                System.out.println("ℹ Level tidak diperbarui (sudah setara atau lebih tinggi).");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error saat update level: " + e.getMessage());
        }
    }


    // ── 4. AMBIL LEVEL ────────────────────────────────────────────────────────

    /**
     * Mengambil level terkini user dari database.
     * Dipakai saat game mulai untuk melanjutkan dari level terakhir.
     *
     * @param userId ID user
     * @return level user (minimal 1)
     */
    public static int getUserLevel(int userId) {
        String sql = "SELECT level FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("level");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error saat ambil level: " + e.getMessage());
        }

        return 1;   // Default: level 1
    }
}