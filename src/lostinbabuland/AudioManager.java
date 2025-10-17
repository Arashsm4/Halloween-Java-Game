// AudioManager.java - manages background music and sound effects
package lostinbabuland;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class AudioManager {
    private static MediaPlayer backgroundPlayer;

    // Play background music in a loop
    public static void playBackgroundMusic(String fileName) {
        try {
            // Load and play the background music file
            Media media = new Media(AudioManager.class.getResource(fileName).toExternalForm());
            backgroundPlayer = new MediaPlayer(media);
            backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundPlayer.play();
        } catch (Exception e) {
            System.out.println("Background music could not be played: " + e.getMessage());
        }
    }

    // Stop the background music if playing
    public static void stopBackgroundMusic() {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
            backgroundPlayer = null;
        }
    }

    // Play a short sound effect
    public static void playSoundEffect(String fileName) {
        try {
            AudioClip clip = new AudioClip(AudioManager.class.getResource(fileName).toExternalForm());
            clip.play();
        } catch (Exception e) {
            System.out.println("Sound effect could not be played: " + e.getMessage());
        }
    }
}
