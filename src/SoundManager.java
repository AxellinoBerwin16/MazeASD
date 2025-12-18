import javax.sound.sampled.*;
import java.io.File;

public class SoundManager {


    private static final String SCAN_SOUND = "src/scan.wav";

    private static final String POP_SOUND = "src/pop.wav";

    private static Clip scanClip;
    private static Clip popClip;

    static {
        scanClip = loadClip(SCAN_SOUND);
        popClip = loadClip(POP_SOUND);
    }

    private static Clip loadClip(String path) {
        try {
            File f = new File(path);
            if (f.exists()) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                Clip c = AudioSystem.getClip();
                c.open(ais);
                return c;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // --- LOGIC BUAT FASE SCANNING (2 Detik Loop) ---
    public static void startScanning() {
        if (scanClip != null) {
            if (scanClip.isRunning()) scanClip.stop();
            scanClip.setFramePosition(0);
            scanClip.loop(Clip.LOOP_CONTINUOUSLY); // <--- INI KUNCINYA
            scanClip.start();
        }
    }

    public static void stopScanning() {
        if (scanClip != null && scanClip.isRunning()) {
            scanClip.stop();
        }
    }

    // --- LOGIC BUAT FASE PATH (Pop Pendek) ---
    public static void playPop() {
        if (popClip != null) {
            if (popClip.isRunning()) popClip.stop();
            popClip.setFramePosition(0);
            popClip.start();
        }
    }
}