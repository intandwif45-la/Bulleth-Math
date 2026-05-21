package com.mycompany.bullethmath;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseManager.java
 * Mengelola semua operasi database SQLite:
 *   - Tabel: users, progress, scores, leaderboard
 *   - Register/Login (SHA-256 hash)
 *   - Progress level per materi
 *   - Skor per level
 *   - Leaderboard per materi
 */
public class DatabaseManager {

    // Path otomatis mengikuti lokasi folder project (tidak perlu hardcode)
    private static final String DB_URL = "jdbc:sqlite:" +
        System.getProperty("user.dir").replace("\\", "/") + "/math_adventure.db";
    private Connection conn;

    // ── Konstanta materi ─────────────────────────────────────────────────────
    public static final String PENJUMLAHAN = "penjumlahan";
    public static final String PENGURANGAN = "pengurangan";
    public static final String PERKALIAN   = "perkalian";
    public static final String PEMBAGIAN   = "pembagian";

    // ── Konstruktor ──────────────────────────────────────────────────────────
    public DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("[DB] Mencoba konek ke: " + DB_URL);

            // Pakai Properties untuk set timeout sebelum konek
            java.util.Properties props = new java.util.Properties();
            props.setProperty("busy_timeout", "30000");

            conn = DriverManager.getConnection(DB_URL, props);

            // Set autoCommit true agar setiap DDL langsung commit
            conn.setAutoCommit(true);

            Statement st = conn.createStatement();
            // DELETE mode lebih aman dari WAL untuk menghindari locked
            st.execute("PRAGMA journal_mode=DELETE");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("PRAGMA busy_timeout=30000");
            st.execute("PRAGMA locking_mode=NORMAL");
            st.close();

            initTables();
            System.out.println("[DB] Koneksi berhasil!");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Driver SQLite tidak ditemukan: " + e.getMessage());
            conn = null;
        } catch (SQLException e) {
            System.err.println("[DB] Gagal konek: " + e.getMessage());
            conn = null;
        }
    }

    // ── Inisialisasi tabel ───────────────────────────────────────────────────
    private void initTables() throws SQLException {
        // Jalankan satu per satu dengan statement terpisah agar tidak ada
        // transaction yang menggantung
        executeSQL(
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id            INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  username      TEXT NOT NULL UNIQUE COLLATE NOCASE," +
            "  password_hash TEXT NOT NULL," +
            "  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        executeSQL(
            "CREATE TABLE IF NOT EXISTS progress (" +
            "  id            INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  user_id       INTEGER NOT NULL," +
            "  materi        TEXT NOT NULL," +
            "  level_terbuka INTEGER NOT NULL DEFAULT 1," +
            "  UNIQUE(user_id, materi)," +
            "  FOREIGN KEY(user_id) REFERENCES users(id)" +
            ")"
        );
        executeSQL(
            "CREATE TABLE IF NOT EXISTS scores (" +
            "  id        INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  user_id   INTEGER NOT NULL," +
            "  materi    TEXT NOT NULL," +
            "  level     INTEGER NOT NULL," +
            "  score     INTEGER NOT NULL DEFAULT 0," +
            "  stars     INTEGER NOT NULL DEFAULT 0," +
            "  played_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY(user_id) REFERENCES users(id)" +
            ")"
        );
        executeSQL(
            "CREATE TABLE IF NOT EXISTS leaderboard (" +
            "  id          INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  user_id     INTEGER NOT NULL," +
            "  materi      TEXT NOT NULL," +
            "  total_score INTEGER NOT NULL DEFAULT 0," +
            "  UNIQUE(user_id, materi)," +
            "  FOREIGN KEY(user_id) REFERENCES users(id)" +
            ")"
        );
        System.out.println("[DB] Semua tabel siap.");
    }

    /** Helper: jalankan satu SQL DDL dengan statement baru lalu langsung tutup */
    private void executeSQL(String sql) throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute(sql);
        }
    }

    // =========================================================================
    //  REGISTER & LOGIN
    // =========================================================================

    /** Daftarkan user baru. Return true jika berhasil. */
    public boolean registerUser(String username, String password) {
        if (conn == null) return false;
        String hash = sha256(password);
        if (hash == null) return false;
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, hash);
            ps.executeUpdate();
            // Inisialisasi progress untuk semua materi
            int uid = getLastInsertId();
            initProgressAllMateri(uid);
            System.out.println("[DB] Register: " + username);
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] Register gagal: " + e.getMessage());
            return false;
        }
    }

    private void initProgressAllMateri(int userId) {
        String[] mList = {PENJUMLAHAN, PENGURANGAN, PERKALIAN, PEMBAGIAN};
        for (String m : mList) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO progress (user_id, materi, level_terbuka) VALUES (?,?,1)");
                ps.setInt(1, userId);
                ps.setString(2, m);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ignore) {}
        }
    }

    private int getLastInsertId() throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    /** Login user. Return UserModel jika sukses, null jika gagal. */
    public UserModel loginUser(String username, String password) {
        if (conn == null) return null;
        String hash = sha256(password);
        if (hash == null) return null;
        String sql = "SELECT id, username FROM users WHERE LOWER(username)=LOWER(?) AND password_hash=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, hash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String uname = rs.getString("username");
                // Pastikan progress ada untuk semua materi
                initProgressAllMateri(id);
                System.out.println("[DB] Login: " + uname);
                return new UserModel(id, uname);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Login error: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    //  PROGRESS
    // =========================================================================

    /** Ambil level terbuka untuk user & materi tertentu. */
    public int getLevelTerbuka(int userId, String materi) {
        if (conn == null) return 1;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT level_terbuka FROM progress WHERE user_id=? AND materi=?")) {
            ps.setInt(1, userId);
            ps.setString(2, materi);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) {
            System.err.println("[DB] getLevelTerbuka: " + e.getMessage());
        }
        return 1;
    }

    /** Buka level berikutnya jika level saat ini sudah selesai. */
    public void unlockNextLevel(int userId, String materi, int levelSelesai) {
        if (conn == null) return;
        int current = getLevelTerbuka(userId, materi);
        int nextLevel = levelSelesai + 1;
        if (nextLevel > 50) nextLevel = 50; // Max 50 level
        if (nextLevel > current) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO progress (user_id, materi, level_terbuka) VALUES (?,?,?)")) {
                ps.setInt(1, userId);
                ps.setString(2, materi);
                ps.setInt(3, nextLevel);
                ps.executeUpdate();
                System.out.println("[DB] Unlock level " + nextLevel + " (" + materi + ")");
            } catch (SQLException e) {
                System.err.println("[DB] unlockNextLevel: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  SCORES
    // =========================================================================

    /** Simpan skor level. Hanya update jika lebih tinggi dari skor sebelumnya. */
    public void saveScore(int userId, String materi, int level, int score, int stars) {
        if (conn == null) return;
        // Cek skor terbaik sebelumnya
        int bestScore = getBestScore(userId, materi, level);
        if (score > bestScore) {
            // Update atau insert
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM scores WHERE user_id=? AND materi=? AND level=?");
                ps.setInt(1, userId); ps.setString(2, materi); ps.setInt(3, level);
                ps.executeUpdate(); ps.close();

                ps = conn.prepareStatement(
                    "INSERT INTO scores (user_id, materi, level, score, stars) VALUES (?,?,?,?,?)");
                ps.setInt(1, userId); ps.setString(2, materi); ps.setInt(3, level);
                ps.setInt(4, score);  ps.setInt(5, stars);
                ps.executeUpdate(); ps.close();

                // Update leaderboard
                updateLeaderboard(userId, materi, score - bestScore);
                System.out.println("[DB] Skor disimpan: " + score + " bintang=" + stars);
            } catch (SQLException e) {
                System.err.println("[DB] saveScore: " + e.getMessage());
            }
        }
    }

    public int getBestScore(int userId, String materi, int level) {
        if (conn == null) return 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT score FROM scores WHERE user_id=? AND materi=? AND level=?")) {
            ps.setInt(1, userId); ps.setString(2, materi); ps.setInt(3, level);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    public int getBestStars(int userId, String materi, int level) {
        if (conn == null) return 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT stars FROM scores WHERE user_id=? AND materi=? AND level=?")) {
            ps.setInt(1, userId); ps.setString(2, materi); ps.setInt(3, level);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    // =========================================================================
    //  LEADERBOARD
    // =========================================================================

    private void updateLeaderboard(int userId, String materi, int addScore) {
        if (conn == null || addScore <= 0) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO leaderboard (user_id, materi, total_score) VALUES (?,?,?) " +
                "ON CONFLICT(user_id, materi) DO UPDATE SET total_score = total_score + ?");
            ps.setInt(1, userId); ps.setString(2, materi);
            ps.setInt(3, addScore); ps.setInt(4, addScore);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) {
            System.err.println("[DB] updateLeaderboard: " + e.getMessage());
        }
    }

    // =========================================================================
    //  SCORE ENTRY (dipakai oleh GamePanel lama)
    // =========================================================================

    public static class ScoreEntry {
        public final String player;
        public final int    score;
        public final int    level;
        public ScoreEntry(String player, int score, int level) {
            this.player = player;
            this.score  = score;
            this.level  = level;
        }
    }

    /** Simpan skor dari GamePanel lama (3 param: player, score, level). */
    public void saveScore(String player, int score, int level) {
        if (conn == null) return;
        try {
            // Cari user_id berdasarkan username
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM users WHERE LOWER(username)=LOWER(?)");
            ps.setString(1, player);
            ResultSet rs = ps.executeQuery();
            int uid = rs.next() ? rs.getInt(1) : -1;
            ps.close();
            if (uid < 0) return;
            // Simpan ke scores dengan materi kosong & stars=0
            ps = conn.prepareStatement(
                "INSERT INTO scores (user_id, materi, level, score, stars) VALUES (?,?,?,?,0)");
            ps.setInt(1, uid); ps.setString(2, ""); ps.setInt(3, level); ps.setInt(4, score);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) {
            System.err.println("[DB] saveScore(legacy): " + e.getMessage());
        }
    }

    /** Ambil top N skor global (dipakai GamePanel lama). */
    public java.util.List<ScoreEntry> getTopScores(int limit) {
        java.util.List<ScoreEntry> list = new java.util.ArrayList<>();
        if (conn == null) return list;
        String sql =
            "SELECT u.username, s.score, s.level " +
            "FROM scores s JOIN users u ON s.user_id = u.id " +
            "ORDER BY s.score DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new ScoreEntry(
                    rs.getString("username"),
                    rs.getInt("score"),
                    rs.getInt("level")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB] getTopScores: " + e.getMessage());
        }
        return list;
    }

    /** Update progress level user (dipakai GamePanel lama). */
    public void updateProgressLevel(int userId, int level) {
        // Delegasikan ke unlockNextLevel dengan materi kosong
        if (conn == null) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO progress (user_id, materi, level_terbuka) VALUES (?,?,?)")) {
            ps.setInt(1, userId); ps.setString(2, ""); ps.setInt(3, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] updateProgressLevel: " + e.getMessage());
        }
    }

    public static class LeaderboardEntry {
        public final String username;
        public final int    totalScore;
        public final int    rank;
        public LeaderboardEntry(int rank, String username, int totalScore) {
            this.rank = rank; this.username = username; this.totalScore = totalScore;
        }
    }

    public List<LeaderboardEntry> getLeaderboard(String materi, int limit) {
        List<LeaderboardEntry> list = new ArrayList<>();
        if (conn == null) return list;
        String sql =
            "SELECT u.username, l.total_score " +
            "FROM leaderboard l JOIN users u ON l.user_id = u.id " +
            "WHERE l.materi=? " +
            "ORDER BY l.total_score DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, materi); ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            int rank = 1;
            while (rs.next()) {
                list.add(new LeaderboardEntry(
                    rank++,
                    rs.getString("username"),
                    rs.getInt("total_score")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB] getLeaderboard: " + e.getMessage());
        }
        return list;
    }

    // =========================================================================
    //  UTILITY
    // =========================================================================

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("[DB] SHA-256 error: " + e.getMessage());
            return null;
        }
    }

    public boolean isConnected() { return conn != null; }

    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("[DB] Koneksi ditutup.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] close error: " + e.getMessage());
        }
    }
}