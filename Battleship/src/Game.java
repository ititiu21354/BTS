import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;


import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import java.io.File;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class Game implements KeyListener {
    /**
     * Entry point for the application to create an instance of the Game class.
     *
     * @param args Not used.
     */
    public static void main(String[] args) {
        Game game = new Game();
    }

    /**
     * Reference to the GamePanel object to pass key events to.
     */
    private GamePanel gamePanel;
    private JFrame frame;

    /**
     * Creates the JFrame with a GamePanel inside it, attaches a key listener,
     * and makes everything visible.
     */
    public Game() {
        // Choose Level Difficulty
        String[] menu = new String[] {"STRATEGY", "PLAY NOW"};
        String[] options = new String[] {"EASY", "MEDIUM", "HARD"};

        BackgroundPanel backgroundPanel = new BackgroundPanel("bg.png");
        BackgroundPanel levelPanel = new BackgroundPanel("lvl.png");
        BackgroundPanel strategyPanel = new BackgroundPanel("strate.png");

        levelPanel.setLayout(new BorderLayout());
        levelPanel.setPreferredSize(new Dimension(1192/2, 705));
        strategyPanel.setLayout(new BorderLayout());
        strategyPanel.setPreferredSize(new Dimension(1192/2, 705));
        backgroundPanel.setLayout(new BorderLayout());
        backgroundPanel.setPreferredSize(new Dimension(1192/2, 705));

        int menuChoice = JOptionPane.showOptionDialog(null, backgroundPanel, "MENU", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,null, menu, menu[0]);
        if (menuChoice == 0) {
            JOptionPane.showMessageDialog(null, strategyPanel, "STRATEGY", JOptionPane.PLAIN_MESSAGE);
        }
        int difficultyChoice = JOptionPane.showOptionDialog(null, levelPanel,
                "DIFFICULTY",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        JFrame frame = new JFrame("BATTLE SHIP");
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        gamePanel = new GamePanel(difficultyChoice);
        frame.getContentPane().add(gamePanel);

        frame.addKeyListener(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        JFXPanel jfxPanel = new JFXPanel(); // This will initialize JavaFX Toolkit
        Platform.runLater(() -> createScene(jfxPanel));

        frame.getContentPane().add(jfxPanel, BorderLayout.CENTER);
    }

        private void createScene(JFXPanel jfxPanel) {
        Media media = new Media(new File("path/to/your/video.mp4").toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(true);
        
        MediaView mediaView = new MediaView(mediaPlayer);
        
        jfxPanel.setScene(new Scene(new Group(mediaView)));
        mediaView.setLocationRelativeTo(null);
        mediaView.setFitWidth(1200); // Set width and height as per your requirement
        mediaView.setFitHeight(700 );
    }

    /**
     * Called when the key is pressed down. Passes the key press on to the GamePanel.
     *
     * @param e Information about what key was pressed.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        gamePanel.handleInput(e.getKeyCode());
    }

    /**
     * Not used.
     *
     * @param e Not used.
     */
    @Override
    public void keyTyped(KeyEvent e) {}
    /**
     * Not used.
     *
     * @param e Not used.
     */
    @Override
    public void keyReleased(KeyEvent e) {}
}
