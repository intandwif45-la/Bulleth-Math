package bulletmath;

import javax.swing.*;
import java.awt.*;

/**
 * BulletMathGame - Main class / Entry point.
 * Game tembak angka dengan Reverse Thinking.
 *
 * Cara Bermain:
 *   - Kamu punya PELURU dengan sebuah ANGKA (misal: 5)
 *   - Musuh datang membawa EKSPRESI MATEMATIKA (misal: 2+3, 8-1, 3x4)
 *   - Klik/Tembak musuh yang ekspresinya SAMA DENGAN angka peluru!
 *   - Salah tembak = -1 nyawa
 *   - Musuh lolos ke bawah = -1 nyawa
 *   - Setiap 5 tembakan benar = naik level (semakin cepat & susah)
 *
 * Cara Menjalankan di NetBeans:
 *   1. Buat Java Project baru: "BulletMathGame"
 *   2. Buat package: "bulletmath"
 *   3. Copy semua file .java ke dalam package tersebut
 *   4. Set BulletMathGame.java sebagai Main Class
 *   5. Run (F6)
 */
public class BulletMathGame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🎯 BulletMath: Reverse Thinking Shooter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GamePanel panel = new GamePanel();
            frame.add(panel);
            frame.pack();

            // Tampilkan di tengah layar
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
