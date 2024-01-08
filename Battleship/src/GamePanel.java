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


public class GamePanel extends JPanel implements MouseListener, MouseMotionListener {
    public enum GameState { PlacingShips, FiringShots, GameOver }
    private StatusPanel statusPanel;
    protected SelectionGrid computer;
    protected SelectionGrid player;
    private BattleshipAI aiController;
    private Ship placingShip;
    private Position tempPlacingPosition;
    private int placingShipIndex;
    private GameState gameState;
    public static boolean debugModeActive;
    public boolean hasExtraTurn;
    public static void playSound(String soundFileName) {
        try {
            File soundFile = new File(soundFileName);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GamePanel(int aiChoice) {
        int gap = 60; // Gap between the two grids

        // Initialize the grids
        computer = new SelectionGrid(0, 0);
        player = new SelectionGrid(computer.getWidth() + gap, 0);

        // Set layout and background color
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        // Set the preferred size of the panel to accommodate both grids horizontally
        int totalWidth = computer.getWidth() + player.getWidth() + gap;
        int maxHeight = Math.max(computer.getHeight(), player.getHeight());
        setPreferredSize(new Dimension(totalWidth, maxHeight + 100));

        // Add mouse listeners
        addMouseListener(this);
        addMouseMotionListener(this);

        // Initialize AI controller
        if (aiChoice == 0) {
            aiController = new SimpleRandomAI(player);
        } else {
            aiController = new SmarterAI(player, aiChoice == 2, aiChoice == 2);
        }

        // Initialize and position the status panel
        statusPanel = new StatusPanel(new Position(0, maxHeight), totalWidth, 49);
        //add(statusPanel, BorderLayout.SOUTH); // Example: placing it at the bottom
        hasExtraTurn = false;
        // Start a new game
        restart();
    }

    /**
     * Draws the grids for both players, any ship being placed, and the status panel.
     *
     * @param g Reference to the Graphics object for drawing.
     */
    public void paint(Graphics g) {
        super.paint(g);

        computer.paint(g);
        player.paint(g);
        if(gameState == GameState.PlacingShips) {
            placingShip.paint(g);
        }
        statusPanel.paint(g);
    }

    /**
     * Handles input based on keys that are pressed.
     * Escape quits the application. S restarts.
     * R rotates the ship while in PlacingShips state.
     * D activates the debug mode to show computer ships.
     *
     * @param keyCode The key that was pressed.
     */
    public void handleInput(int keyCode) {
        if(keyCode == KeyEvent.VK_ESCAPE) {
            System.exit(1);
        } else if(keyCode == KeyEvent.VK_S) {
            restart(); //S to restart
        } else if(gameState == GameState.PlacingShips && keyCode == KeyEvent.VK_R) {
            placingShip.toggleSideways(); //R to rotate the ship
            updateShipPlacement(tempPlacingPosition);
        } else if(keyCode == KeyEvent.VK_D) {
            debugModeActive = !debugModeActive; //D to active the debug mode
        }
        repaint();
    }

    /**
     * Resets all the class's properties back to their defaults ready for a new game to begin.
     */
    public void restart() {
        computer.reset();
        player.reset();
        // Player can see their own ships by default
        player.setShowShips(true);
        aiController.reset();
        tempPlacingPosition = new Position(0,0);
        placingShip = new Ship(new Position(0,0),
                               new Position(player.getPosition().x,player.getPosition().y),
                               SelectionGrid.BOAT_SIZES[0], true);
        placingShipIndex = 0;
        updateShipPlacement(tempPlacingPosition);
        computer.populateShips();
        debugModeActive = false;
        statusPanel.reset();
        gameState = GameState.PlacingShips;
        hasExtraTurn = false;
    }

    /**
     * Uses the mouse position to test update the ship being placed during the
     * PlacingShip state. Then if the place it has been placed is valid the ship will
     * be locked in by calling placeShip().
     *
     * @param mousePosition Mouse coordinates inside the panel.
     */
    private void tryPlaceShip(Position mousePosition) {
        Position targetPosition = player.getPositionInGrid(mousePosition.x, mousePosition.y);
        updateShipPlacement(targetPosition);
        if(player.canPlaceShipAt(targetPosition.x, targetPosition.y,
                SelectionGrid.BOAT_SIZES[placingShipIndex],placingShip.isSideways())) {
            placeShip(targetPosition);
        }
    }

    /**
     * Finalises the insertion of the ship being placed by storing it in the player's grid.
     * Then either prepares the next ship for placing, or moves to the next state.
     *
     * @param targetPosition The position on the grid to insert the ship at.
     */
    private void placeShip(Position targetPosition) {
        placingShip.setShipPlacementColour(Ship.ShipPlacementColour.Placed);
        player.placeShip(placingShip,tempPlacingPosition.x,tempPlacingPosition.y);
        placingShipIndex++;
        // If there are still ships to place
        if(placingShipIndex < SelectionGrid.BOAT_SIZES.length) {
            placingShip = new Ship(new Position(targetPosition.x, targetPosition.y),
                          new Position(player.getPosition().x + targetPosition.x * SelectionGrid.CELL_SIZE,
                       player.getPosition().y + targetPosition.y * SelectionGrid.CELL_SIZE),
                          SelectionGrid.BOAT_SIZES[placingShipIndex], true);
            updateShipPlacement(tempPlacingPosition);
        } else {
            gameState = GameState.FiringShots;
            statusPanel.setTopLine("ATTACK THE ENEMY!");
            statusPanel.setBottomLine("DESTROY ALL SHIPS TO WIN!");
        }
    }

    /**
     * Attempts to fire at a position on the computer's board.
     * The player is notified if they hit/missed, or nothing if they
     * have clicked the same place again. After the player's turn,
     * the AI is given a turn if the game is not already ended.
     *
     * @param mousePosition Mouse coordinates inside the panel.
     */
    private void tryFireAtComputer(Position mousePosition) {
        playSound("shoot.wav");
        Position targetPosition = computer.getPositionInGrid(mousePosition.x,mousePosition.y);
        // Ignore if position was already clicked
        if(!computer.isPositionMarked(targetPosition)) {
            doPlayerTurn(targetPosition);
            // Only do the AI turn if the game didn't end from the player's turn.
            if(!computer.areAllShipsDestroyed() && !hasExtraTurn) {
                doAITurn();
            }
            hasExtraTurn = false;
        }
    }
    public void extraTurn() {
        hasExtraTurn = true;
    }

    /**
     * Processes the player's turn based on where they selected to attack.
     * Based on the result of the attack a message is displayed to the player,
     * and if they destroyed the last ship the game updates to a won state.
     *
     * @param targetPosition The grid position clicked on by the player.
     */


private void playVideo(String videoFileName) {
    JFXPanel jfxPanel = new JFXPanel(); 
    JFrame videoFrame = new JFrame(); 

    Platform.runLater(() -> {
        try {
            File videoFile = new File(videoFileName);
            MediaPlayer mediaPlayer = new MediaPlayer(new Media(videoFile.toURI().toString()));
            mediaPlayer.setAutoPlay(true);

            MediaView mediaView = new MediaView(mediaPlayer);
            StackPane root = new StackPane(mediaView); 
            Scene scene = new Scene(root); 
            jfxPanel.setScene(scene); 

            mediaPlayer.setOnEndOfMedia(() -> {
                
                javax.swing.SwingUtilities.invokeLater(videoFrame::dispose);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    javax.swing.SwingUtilities.invokeLater(() -> {
        videoFrame.add(jfxPanel);
        videoFrame.setSize(640, 480);
        videoFrame.setLocationRelativeTo(null);
        videoFrame.setVisible(true); 
        videoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
    });
}

    private void doPlayerTurn(Position targetPosition) {
        boolean hit = computer.markPosition(targetPosition);
        boolean hitTreasure = computer.isTreasureAtPosition(targetPosition);
        String statusMessage = "";

        if(hitTreasure) {
            statusMessage = "TREASURE FOUND! YOU HAVE 1 MORE MOVE!!";
            extraTurn();
            //doPlayerTurn(targetPosition);
            playSound("treasure.wav"); // Optionally play a sound
        }
        String hitMiss = hit ? "HIT!" : "MISSED!";
        String destroyed = "";

        Marker marker = computer.getMarkerAtPosition(targetPosition);
        if(hit && marker.getAssociatedShip() != null && marker.getAssociatedShip().isDestroyed()) {
            destroyed = "ENEMY'S SHIP HAS SUNK!";
        }
        statusPanel.setTopLine(statusMessage + " YOU " + hitMiss + " " + destroyed);

        if(computer.areAllShipsDestroyed()) {
            playSound("win.wav");
            gameState = GameState.GameOver;
            statusPanel.showGameOver(true);
            playVideo("toothless.mp4");
        }
    }


    /**
     * Processes the AI turn by using the AI Controller to select a move.
     * Then processes the result to display it to the player. If the AI
     * destroyed the last ship the game will end with AI winning.
     */
    private void doAITurn() {
        Position aiMove = aiController.selectMove();
        boolean hit = player.markPosition(aiMove);
        String hitMiss = hit ? "HIT!" : "MISSED!";
        String destroyed = "";
        if(hit && player.getMarkerAtPosition(aiMove).getAssociatedShip().isDestroyed()) {
            destroyed = "YOUR SHIP HAS SUNK!";
        }
        statusPanel.setBottomLine("ENEMY " + hitMiss + " " + destroyed);
        if(player.areAllShipsDestroyed()) {
            // Computer wins!
            playSound("lose.wav");
            playVideo("meme12.mp4"); 
            gameState = GameState.GameOver;
            statusPanel.showGameOver(false);
        }
    }
    /**
     * Updates the ship being placed location if the mouse is inside the grid.
     *
     * @param mousePosition Mouse coordinates inside the panel.
     */
    private void tryMovePlacingShip(Position mousePosition) {
        if(player.isPositionInside(mousePosition)) {
            Position targetPos = player.getPositionInGrid(mousePosition.x, mousePosition.y);
            updateShipPlacement(targetPos);
        }
    }

    /**
     * Constrains the ship to fit inside the grid. Updates the drawn position of the ship,
     * and changes the colour of the ship based on whether it is a valid or invalid placement.
     *
     * @param targetPos The grid coordinate where the ship being placed should change to.
     */
    private void updateShipPlacement(Position targetPos) {
        // Constrain to fit inside the grid
        if(placingShip.isSideways()) {
            targetPos.x = Math.min(targetPos.x, SelectionGrid.GRID_WIDTH - SelectionGrid.BOAT_SIZES[placingShipIndex]);
        } else {
            targetPos.y = Math.min(targetPos.y, SelectionGrid.GRID_HEIGHT - SelectionGrid.BOAT_SIZES[placingShipIndex]);
        }
        // Update drawing position to use the new target position
        placingShip.setDrawPosition(new Position(targetPos),
                                    new Position(player.getPosition().x + targetPos.x * SelectionGrid.CELL_SIZE,
                                 player.getPosition().y + targetPos.y * SelectionGrid.CELL_SIZE));
        // Store the grid position for other testing cases
        tempPlacingPosition = targetPos;
        // Change the colour of the ship based on whether it could be placed at the current location.
        if(player.canPlaceShipAt(tempPlacingPosition.x, tempPlacingPosition.y,
                SelectionGrid.BOAT_SIZES[placingShipIndex],placingShip.isSideways())) {
            placingShip.setShipPlacementColour(Ship.ShipPlacementColour.Valid);
        } else {
            placingShip.setShipPlacementColour(Ship.ShipPlacementColour.Invalid);
        }
    }

    /**
     * Triggered when the mouse button is released. If in the PlacingShips state and the
     * cursor is inside the player's grid it will try to place the ship.
     * Otherwise if in the FiringShots state and the cursor is in the computer's grid,
     * it will try to fire at the computer.
     *
     * @param e Details about where the mouse event occurred.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        Position mousePosition = new Position(e.getX(), e.getY());
        if(gameState == GameState.PlacingShips && player.isPositionInside(mousePosition)) {
            tryPlaceShip(mousePosition);
        } else if(gameState == GameState.FiringShots && computer.isPositionInside(mousePosition)) {
            tryFireAtComputer(mousePosition);
        }
        repaint();
    }

    /**
     * Triggered when the mouse moves inside the panel. Does nothing if not in the PlacingShips state.
     * Will try and move the ship that is currently being placed based on the mouse coordinates.
     *
     * @param e Details about where the mouse event occurred.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if(gameState != GameState.PlacingShips) return;
        tryMovePlacingShip(new Position(e.getX(), e.getY()));
        repaint();
    }

    /**
     * Not used.
     *
     * @param e Not used.
     */
    @Override
    public void mouseClicked(MouseEvent e) {}
    /**
     * Not used.
     *
     * @param e Not used.
     */
    @Override
    public void mousePressed(MouseEvent e) {}
    /**
     * Not used.
     *
     * @param e Not used.
     */
    @Override
    public void mouseEntered(MouseEvent e) {}
    /**
     * Not used.
     *
     * @param e Not used.
     */
    @Override
    public void mouseExited(MouseEvent e) {}
    /**
     * Not used.
     *
     * @param e Not used.
     */
    @Override
    public void mouseDragged(MouseEvent e) {}
}
