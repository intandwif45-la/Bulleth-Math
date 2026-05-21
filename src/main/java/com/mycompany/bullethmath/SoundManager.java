package com.mycompany.bullethmath;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;

/**
 * SoundManager — Generate semua suara secara programatik (tanpa file WAV).
 *
 * Suara yang tersedia:
 *   "bgm"      — background music loop (underwater vibe)
 *   "correct"  — jawaban benar (ascending ding)
 *   "wrong"    — jawaban salah (descending buzz)
 *   "bubble"   — gelembung pop
 *   "shoot"    — tembakan cannon
 *   "levelup"  — naik level (fanfare kecil)
 *   "gameover" — game over (descending sad)
 *   "click"    — klik tombol
 *   "countdown"— countdown beep
 */
public class SoundManager {

    private static SoundManager instance;

    private final Map<String, byte[]> soundData = new HashMap<>();
    private Clip   bgmClip;
    private float  bgmVolume  = 1.0f;   // 0.0 – 1.0  (maksimal)
    private float  sfxVolume  = 1.0f;
    private boolean sfxEnabled = true;
    private boolean bgmEnabled = true;

    private static final int SAMPLE_RATE = 44100;
    // Gunakan 16-bit signed untuk volume jauh lebih keras
    private static final AudioFormat AUDIO_FMT =
        new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    // ── Singleton ─────────────────────────────────────────────────────────────
    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    private SoundManager() {
        generateAll();
        startBGM();
    }

    // =========================================================================
    //  GENERATE SEMUA SUARA
    // =========================================================================
    private void generateAll() {
        soundData.put("correct",   genCorrect());
        soundData.put("wrong",     genWrong());
        soundData.put("bubble",    genBubble());
        soundData.put("shoot",     genShoot());
        soundData.put("levelup",   genLevelUp());
        soundData.put("gameover",  genGameOver());
        soundData.put("click",     genClick());
        soundData.put("countdown", genCountdown());
        soundData.put("bgm",       genBGM());
    }

    // ── Helper: konversi array double sample (-1.0..1.0) ke byte[] 16-bit ────
    private static byte[] toBytes(double[] samples) {
        byte[] buf = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int val = (int) Math.max(-32767, Math.min(32767, samples[i] * 32767));
            buf[i * 2]     = (byte)(val & 0xFF);
            buf[i * 2 + 1] = (byte)((val >> 8) & 0xFF);
        }
        return buf;
    }

    // ── Nada benar: do-mi-sol ascending ──────────────────────────────────────
    private byte[] genCorrect() {
        double[] freqs = {523.25, 659.25, 783.99};
        int dur = (int)(SAMPLE_RATE * 0.14);
        double[] samples = new double[freqs.length * dur];
        for (int n = 0; n < freqs.length; n++) {
            for (int i = 0; i < dur; i++) {
                double t   = i / (double) SAMPLE_RATE;
                double env = Math.min(1.0, (dur - i) / (dur * 0.3));
                samples[n * dur + i] = 0.85 * Math.sin(2 * Math.PI * freqs[n] * t) * env;
            }
        }
        return toBytes(samples);
    }

    // ── Nada salah: descending buzz ───────────────────────────────────────────
    private byte[] genWrong() {
        int dur = (int)(SAMPLE_RATE * 0.35);
        double[] samples = new double[dur];
        for (int i = 0; i < dur; i++) {
            double t    = i / (double) SAMPLE_RATE;
            double freq = 300 - 150 * (i / (double) dur);
            double env  = Math.max(0, 1.0 - i / (double) dur);
            samples[i]  = 0.85 * Math.sin(2 * Math.PI * freq * t) * env;
        }
        return toBytes(samples);
    }

    // ── Bubble pop ────────────────────────────────────────────────────────────
    private byte[] genBubble() {
        int dur = (int)(SAMPLE_RATE * 0.18);
        double[] samples = new double[dur];
        for (int i = 0; i < dur; i++) {
            double t    = i / (double) SAMPLE_RATE;
            double freq = 800 + 400 * (i / (double) dur);
            double env  = Math.max(0, 1.0 - i / (double) dur * 1.5);
            samples[i]  = 0.80 * Math.sin(2 * Math.PI * freq * t) * env;
        }
        return toBytes(samples);
    }

    // ── Background Music: nuansa dalam laut ──────────────────────────────────
    private byte[] genBGM() {
        int loopSamples = SAMPLE_RATE * 8;
        double[] samples = new double[loopSamples];

        double[] droneFreqs = { 55.0, 82.41, 110.0 };
        int noteCount = 8;
        double[] pingFreqs = {
            220.0, 261.63, 329.63, 220.0,
            196.0, 246.94, 293.66, 196.0
        };
        int noteDur = loopSamples / noteCount;

        for (int i = 0; i < loopSamples; i++) {
            double t = i / (double) SAMPLE_RATE;

            double droneEnv = 0.5 + 0.5 * Math.sin(2 * Math.PI * 0.08 * t);
            double drone = 0;
            for (double df : droneFreqs) {
                drone += 0.18 * Math.sin(2 * Math.PI * df * t) * droneEnv;
            }

            double bubble = 0;
            int bubblePeriod = (int)(SAMPLE_RATE * 1.5);
            int bubblePhase  = i % bubblePeriod;
            if (bubblePhase < 800) {
                double bEnv  = Math.exp(-bubblePhase / 200.0);
                double bFreq = 600 + 300 * (bubblePhase / 800.0);
                bubble = 0.22 * Math.sin(2 * Math.PI * bFreq * t) * bEnv;
            }

            int noteIdx = (i / noteDur) % noteCount;
            int notePos = i % noteDur;
            double pingEnv;
            int fadeLen = noteDur / 5;
            if (notePos < fadeLen) {
                pingEnv = notePos / (double) fadeLen;
            } else if (notePos > noteDur - fadeLen) {
                pingEnv = (noteDur - notePos) / (double) fadeLen;
            } else {
                pingEnv = 1.0;
            }
            pingEnv *= 0.65;
            double ping  = 0.35 * Math.sin(2 * Math.PI * pingFreqs[noteIdx] * t) * pingEnv;
            double pingH = 0.12 * Math.sin(2 * Math.PI * pingFreqs[noteIdx] * 2 * t) * pingEnv;
            double shimmer = 0.06 * Math.sin(2 * Math.PI * 880 * t)
                           * (0.3 + 0.7 * Math.sin(2 * Math.PI * 0.15 * t));

            samples[i] = Math.max(-1.0, Math.min(1.0, drone + bubble + ping + pingH + shimmer));
        }
        return toBytes(samples);
    }

    // ── Tembakan cannon: tegas ────────────────────────────────────────────────
    private byte[] genShoot() {
        int dur = (int)(SAMPLE_RATE * 0.22);
        double[] samples = new double[dur];
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < dur; i++) {
            double t     = i / (double) SAMPLE_RATE;
            double env   = Math.exp(-i / (double)(dur * 0.28));
            double noise = (rng.nextInt(65536) - 32768) / 32768.0 * env * 0.90;
            double tone  = 0.85 * Math.sin(2 * Math.PI * 80 * t)
                         * Math.exp(-i / (double)(dur * 0.15));
            double click = (i < 400)
                ? (rng.nextInt(65536) - 32768) / 32768.0 * 1.0 * (1.0 - i / 400.0)
                : 0;
            samples[i] = Math.max(-1.0, Math.min(1.0, noise + tone + click));
        }
        return toBytes(samples);
    }

    // ── Level up: ascending fanfare ───────────────────────────────────────────
    private byte[] genLevelUp() {
        double[] freqs = {523.25, 659.25, 783.99, 1046.5};
        int dur = (int)(SAMPLE_RATE * 0.14);
        double[] samples = new double[freqs.length * dur];
        for (int n = 0; n < freqs.length; n++) {
            for (int i = 0; i < dur; i++) {
                double t   = i / (double) SAMPLE_RATE;
                double env = Math.min(1.0, (dur - i) / (dur * 0.25));
                samples[n * dur + i] = 0.85 * Math.sin(2 * Math.PI * freqs[n] * t) * env;
            }
        }
        return toBytes(samples);
    }

    // ── Game over: sad descending ─────────────────────────────────────────────
    private byte[] genGameOver() {
        double[] freqs = {392.0, 349.23, 311.13, 261.63};
        int dur = (int)(SAMPLE_RATE * 0.22);
        double[] samples = new double[freqs.length * dur];
        for (int n = 0; n < freqs.length; n++) {
            for (int i = 0; i < dur; i++) {
                double t   = i / (double) SAMPLE_RATE;
                double env = Math.max(0, 1.0 - i / (double) dur);
                samples[n * dur + i] = 0.85 * Math.sin(2 * Math.PI * freqs[n] * t) * env;
            }
        }
        return toBytes(samples);
    }

    // ── Klik tombol ───────────────────────────────────────────────────────────
    private byte[] genClick() {
        int dur = (int)(SAMPLE_RATE * 0.06);
        double[] samples = new double[dur];
        for (int i = 0; i < dur; i++) {
            double t   = i / (double) SAMPLE_RATE;
            double env = Math.max(0, 1.0 - i / (double) dur * 3);
            samples[i] = 0.80 * Math.sin(2 * Math.PI * 1200 * t) * env;
        }
        return toBytes(samples);
    }

    // ── Countdown beep ────────────────────────────────────────────────────────
    private byte[] genCountdown() {
        int dur = (int)(SAMPLE_RATE * 0.15);
        double[] samples = new double[dur];
        for (int i = 0; i < dur; i++) {
            double t   = i / (double) SAMPLE_RATE;
            double env = Math.max(0, 1.0 - i / (double) dur * 2);
            samples[i] = 0.85 * Math.sin(2 * Math.PI * 880 * t) * env;
        }
        return toBytes(samples);
    }

    // =========================================================================
    //  PLAYBACK
    // =========================================================================

    /** Mainkan efek suara (non-blocking) */
    public void play(String name) {
        if (!sfxEnabled) return;
        byte[] data = soundData.get(name);
        if (data == null) return;
        try {
            DataLine.Info info = new DataLine.Info(Clip.class, AUDIO_FMT);
            if (!AudioSystem.isLineSupported(info)) return;
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(AUDIO_FMT, data, 0, data.length);
            setVolume(clip, sfxVolume);
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) clip.close();
            });
            clip.start();
        } catch (Exception e) {
            // Abaikan jika audio tidak tersedia
        }
    }

    /** Mulai background music (loop) */
    public void startBGM() {
        if (!bgmEnabled) return;
        stopBGM();

        // ── Coba load dari file resource dulu ────────────────────────────────
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/MUSIK PBO.mpeg.wav");
            if (is != null) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(
                    new java.io.BufferedInputStream(is));
                DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
                if (AudioSystem.isLineSupported(info)) {
                    bgmClip = (Clip) AudioSystem.getLine(info);
                    bgmClip.open(ais);
                    setVolume(bgmClip, bgmVolume);
                    bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                    bgmClip.start();
                    return;
                }
            }
        } catch (Exception e) {
            // Fallback ke generated BGM
        }

        // ── Fallback: generated BGM ───────────────────────────────────────────
        byte[] data = soundData.get("bgm");
        if (data == null) return;
        try {
            DataLine.Info info = new DataLine.Info(Clip.class, AUDIO_FMT);
            if (!AudioSystem.isLineSupported(info)) return;
            bgmClip = (Clip) AudioSystem.getLine(info);
            bgmClip.open(AUDIO_FMT, data, 0, data.length);
            setVolume(bgmClip, bgmVolume);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        } catch (Exception e) {
            // Abaikan
        }
    }

    /** Stop background music */
    public void stopBGM() {
        if (bgmClip != null && bgmClip.isOpen()) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }

    /** Set volume BGM (0.0–1.0) */
    public void setBGMVolume(float vol) {
        bgmVolume = Math.max(0f, Math.min(1f, vol));
        if (bgmClip != null && bgmClip.isOpen()) setVolume(bgmClip, bgmVolume);
    }

    /** Set volume SFX (0.0–1.0) */
    public void setSFXVolume(float vol) {
        sfxVolume = Math.max(0f, Math.min(1f, vol));
    }

    public void setBGMEnabled(boolean on) {
        bgmEnabled = on;
        if (on) startBGM(); else stopBGM();
    }

    public void setSFXEnabled(boolean on) { sfxEnabled = on; }

    public boolean isBGMEnabled()  { return bgmEnabled; }
    public boolean isSFXEnabled()  { return sfxEnabled; }
    public float   getBGMVolume()  { return bgmVolume; }
    public float   getSFXVolume()  { return sfxVolume; }

    private void setVolume(Clip clip, float vol) {
        try {
            FloatControl fc = (FloatControl) clip.getControl(
                FloatControl.Type.MASTER_GAIN);
            float dB = (float)(20.0 * Math.log10(Math.max(0.0001, vol)));
            fc.setValue(Math.max(fc.getMinimum(), Math.min(fc.getMaximum(), dB)));
        } catch (Exception ignored) {}
    }

    public void close() {
        stopBGM();
        soundData.clear();
    }
}
