package com.mycompany.bullethmath;

import java.util.Random;

/**
 * LevelGenerator.java
 *
 * Penjumlahan & Pengurangan
 *   Level  1- 9 : satuan  (1–9)
 *   Level 10-19 : puluhan (10–99)
 *   Level 20-29 : ratusan (100–999)
 *   Level 30-50 : ribuan  (1000–9999)
 *
 * Perkalian
 *   Level  1-10 : 2–5  × 1–9
 *   Level 11-20 : 1–9  × 1–12
 *   Level 21-30 : 2–15 × 2–15
 *   Level 31-50 : 2–25 × 2–25
 *
 * Pembagian
 *   Level  1-10 : hasil 1–5,  pembagi 1–9
 *   Level 11-20 : hasil 1–10, pembagi 1–12
 *   Level 21-30 : hasil 1–20, pembagi 2–15
 *   Level 31-50 : hasil 1–50, pembagi 2–25
 */
public class LevelGenerator {

    private static final Random rng = new Random();

    // =========================================================================
    //  PUBLIC API
    // =========================================================================

    /** Hasilkan soal. Return int[]{a, b, jawaban} */
    public static int[] generateQuestion(String materi, int level) {
        switch (materi.toLowerCase()) {
            case DatabaseManager.PENJUMLAHAN: return genPenjumlahan(level);
            case DatabaseManager.PENGURANGAN: return genPengurangan(level);
            case DatabaseManager.PERKALIAN:   return genPerkalian(level);
            case DatabaseManager.PEMBAGIAN:   return genPembagian(level);
            default:                          return new int[]{1, 1, 2};
        }
    }

    public static String getOperator(String materi) {
        switch (materi.toLowerCase()) {
            case DatabaseManager.PENJUMLAHAN: return "+";
            case DatabaseManager.PENGURANGAN: return "-";
            case DatabaseManager.PERKALIAN:   return "x";
            case DatabaseManager.PEMBAGIAN:   return ":";
            default:                          return "?";
        }
    }

    public static String getMateriName(String materi) {
        switch (materi.toLowerCase()) {
            case DatabaseManager.PENJUMLAHAN: return "Penjumlahan";
            case DatabaseManager.PENGURANGAN: return "Pengurangan";
            case DatabaseManager.PERKALIAN:   return "Perkalian";
            case DatabaseManager.PEMBAGIAN:   return "Pembagian";
            default:                          return materi;
        }
    }

    public static String getMateriEmoji(String materi) {
        switch (materi.toLowerCase()) {
            case DatabaseManager.PENJUMLAHAN: return "+";
            case DatabaseManager.PENGURANGAN: return "-";
            case DatabaseManager.PERKALIAN:   return "x";
            case DatabaseManager.PEMBAGIAN:   return "/";
            default:                          return "?";
        }
    }

    public static java.awt.Color getMateriColor(String materi) {
        switch (materi.toLowerCase()) {
            case DatabaseManager.PENJUMLAHAN: return new java.awt.Color(40,  200, 120);
            case DatabaseManager.PENGURANGAN: return new java.awt.Color(255, 100,  50);
            case DatabaseManager.PERKALIAN:   return new java.awt.Color(255, 200,  20);
            case DatabaseManager.PEMBAGIAN:   return new java.awt.Color(100, 180, 255);
            default:                          return java.awt.Color.WHITE;
        }
    }

    /** Timer tidak lagi dipakai (GameplayForm pakai TIMER_SECS=10), tapi tetap ada agar tidak error. */
    public static int getTimerSeconds(int level) {
        return 10;
    }

    // =========================================================================
    //  PENJUMLAHAN
    //   1- 9 : satuan  1–9
    //  10-19 : puluhan 10–99
    //  20-29 : ratusan 100–999
    //  30-50 : ribuan  1000–9999
    // =========================================================================
    private static int[] genPenjumlahan(int level) {
        int lo = penjLo(level);
        int hi = penjHi(level);
        int a  = randRange(lo, hi);
        int b  = randRange(lo, hi);
        return new int[]{a, b, a + b};
    }

    private static int penjLo(int level) {
        if (level <= 9)  return 1;
        if (level <= 19) return 10;
        if (level <= 29) return 100;
        return 1000;
    }

    private static int penjHi(int level) {
        if (level <= 9)  return 9;
        if (level <= 19) return 99;
        if (level <= 29) return 999;
        return 9999;
    }

    // =========================================================================
    //  PENGURANGAN — rentang sama dengan penjumlahan, hasil selalu ≥ 0
    // =========================================================================
    private static int[] genPengurangan(int level) {
        int lo = penjLo(level);
        int hi = penjHi(level);
        int a  = randRange(lo, hi);
        int b  = randRange(lo, hi);
        if (b > a) { int t = a; a = b; b = t; }   // pastikan a ≥ b
        return new int[]{a, b, a - b};
    }

    // =========================================================================
    //  PERKALIAN
    //   1-10 : 2–5  × 1–9
    //  11-20 : 1–9  × 1–12
    //  21-30 : 2–15 × 2–15
    //  31-50 : 2–25 × 2–25
    // =========================================================================
    private static int[] genPerkalian(int level) {
        int minA, maxA, minB, maxB;
        if (level <= 10) {
            minA = 2; maxA = 5;  minB = 1; maxB = 9;
        } else if (level <= 20) {
            minA = 1; maxA = 9;  minB = 1; maxB = 12;
        } else if (level <= 30) {
            minA = 2; maxA = 15; minB = 2; maxB = 15;
        } else {
            minA = 2; maxA = 25; minB = 2; maxB = 25;
        }
        int a = randRange(minA, maxA);
        int b = randRange(minB, maxB);
        return new int[]{a, b, a * b};
    }

    // =========================================================================
    //  PEMBAGIAN
    //   1-10 : hasil 1–5,  pembagi 1–9
    //  11-20 : hasil 1–10, pembagi 1–12
    //  21-30 : hasil 1–20, pembagi 2–15
    //  31-50 : hasil 1–50, pembagi 2–25
    //  Soal dibentuk: (hasil × pembagi) : pembagi = hasil  → selalu bulat
    // =========================================================================
    private static int[] genPembagian(int level) {
        int minHasil, maxHasil, minBagi, maxBagi;
        if (level <= 10) {
            minHasil = 1; maxHasil = 5;  minBagi = 1; maxBagi = 9;
        } else if (level <= 20) {
            minHasil = 1; maxHasil = 10; minBagi = 1; maxBagi = 12;
        } else if (level <= 30) {
            minHasil = 1; maxHasil = 20; minBagi = 2; maxBagi = 15;
        } else {
            minHasil = 1; maxHasil = 50; minBagi = 2; maxBagi = 25;
        }
        int hasil = randRange(minHasil, maxHasil);
        int bagi  = randRange(minBagi,  maxBagi);
        // Soal: (hasil * bagi) : bagi = hasil
        return new int[]{hasil * bagi, bagi, hasil};
    }

    // =========================================================================
    //  HELPER
    // =========================================================================
    private static int randRange(int min, int max) {
        if (min >= max) return min;
        return min + rng.nextInt(max - min + 1);
    }
}
