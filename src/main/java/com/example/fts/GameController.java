package com.example.fts;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public class GameController implements Initializable {

    @FXML private Pane gamePane;
    @FXML private Label normalCellsCount;
    @FXML private Label bacteriaCount;
    @FXML private Label virusCount;
    @FXML private Label healthLabel;
    @FXML private Label timeLabel;
    @FXML private Label gameStatusLabel;
    @FXML private Button startButton;
    @FXML private Button resetButton;
    @FXML private ProgressBar shieldBar;

    private static final int INITIAL_NORMAL_CELLS = 20;
    private static final int INITIAL_BACTERIA = 1;
    private static final int INITIAL_VIRUSES = 2;
    private static final double NORMAL_CELL_REPRODUCTION_TIME = 15; // seconds

    private Player player;
    private List<Normal> normalCells = new ArrayList<>();
    private List<Bacteria> bacteriaCells = new ArrayList<>();
    private List<Virus> virusCells = new ArrayList<>();
    private List<Infected> infectedCells = new ArrayList<>();
    private List<NPC> NPCs = new ArrayList<>();

    private AnimationTimer gameLoop;
    private double gameTime = 0;
    private double cellReproductionTimer = 0;
    private boolean gameRunning = false;
    private Random random = new Random();
    private long lastUpdateTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up keyboard listeners for both press and release
        javafx.application.Platform.runLater(() -> {
            gamePane.getScene().setOnKeyPressed(this::handleKeyPress);
            gamePane.getScene().setOnKeyReleased(this::handleKeyRelease);

            resetGame(null);

            gameLoop = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    // Convert to seconds
                    double currentTime = now / 1_000_000_000.0;
                    if (lastUpdateTime == 0) {
                        lastUpdateTime = now;
                        return;
                    }

                    double elapsedTime = (now - lastUpdateTime) / 1_000_000_000.0;
                    lastUpdateTime = now;

                    if (gameRunning) {
                        updateGame(elapsedTime);
                    }
                }
            };
            gameLoop.start();
        });
    }

    @FXML
    public void startGame(ActionEvent event) {
        if (!gameRunning) {
            gameRunning = true;
            gameStatusLabel.setText("Game running! Use WASD to move, SHIFT for shield");
            startButton.setDisable(true);
            lastUpdateTime = 0;

            // Make sure focus is set for key events
            gamePane.requestFocus();
        }
    }

    @FXML
    public void resetGame(ActionEvent event) {
        // Reset game state
        gameRunning = false;
        gameTime = 0;
        cellReproductionTimer = 0;
        timeLabel.setText("0");
        startButton.setDisable(false);
        gameStatusLabel.setText("Use WASD to move, SHIFT for shield. Eliminate all enemies!");

        // Clear cells
        normalCells.clear();
        bacteriaCells.clear();
        virusCells.clear();
        infectedCells.clear();
        NPCs.clear();
        gamePane.getChildren().clear();

        // Create player at center
        double centerX = gamePane.getPrefWidth() / 2 - 15;
        double centerY = gamePane.getPrefHeight() / 2 - 15;
        player = new Player(centerX, centerY);
        gamePane.getChildren().add(player);

        // Place normal cells
        for (int i = 0; i < INITIAL_NORMAL_CELLS; i++) {
            placeRandomCell(new Normal(0, 0));
        }

        // Place bacteria cells - no more player targeting
        for (int i = 0; i < INITIAL_BACTERIA; i++) {
            placeRandomCell(new Bacteria(0, 0));
        }

        // Place virus cells - no more player targeting
        for (int i = 0; i < INITIAL_VIRUSES; i++) {
            placeRandomCell(new Virus(0, 0));
        }

        // Update counts
        updateCellCounts();

        // Update health display
        updateHealthDisplay();

        // Initialize shield bar
        shieldBar.setProgress(1.0);
    }

    private void handleKeyPress(KeyEvent event) {
        if (!gameRunning) return;

        switch (event.getCode()) {
            case W:
                player.setMovingUp(true);
                break;
            case S:
                player.setMovingDown(true);
                break;
            case A:
                player.setMovingLeft(true);
                break;
            case D:
                player.setMovingRight(true);
                break;
            case SHIFT:
                player.activateShield();
                break;
            default:
                break;
        }

        // Consume the event to prevent it from propagating
        event.consume();
    }

    private void handleKeyRelease(KeyEvent event) {
        if (!gameRunning) return;

        switch (event.getCode()) {
            case W:
                player.setMovingUp(false);
                break;
            case S:
                player.setMovingDown(false);
                break;
            case A:
                player.setMovingLeft(false);
                break;
            case D:
                player.setMovingRight(false);
                break;
            default:
                break;
        }

        // Consume the event to prevent it from propagating
        event.consume();
    }

    private void updateGame(double elapsedTime) {
        // Update game time
        gameTime += elapsedTime;
        timeLabel.setText(String.format("%.1f", gameTime));

        // Update cell reproduction timer
        cellReproductionTimer += elapsedTime;
        if (cellReproductionTimer >= NORMAL_CELL_REPRODUCTION_TIME) {
            cellReproductionTimer = 0;
            reproduceNormalCells();
        }

        // Update player
        player.update(elapsedTime, gamePane.getWidth(), gamePane.getHeight());

        // Update shield bar
        shieldBar.setProgress(player.getShieldCooldownPercentage());

        // Update normal cells
        for (Normal cell : normalCells) {
            cell.update(elapsedTime, gamePane.getWidth(), gamePane.getHeight());
        }

        // Update bacteria cells
        for (Bacteria cell : bacteriaCells) {
            cell.update(elapsedTime, gamePane.getWidth(), gamePane.getHeight());
        }

        // Update virus cells
        for (Virus cell : virusCells) {
            cell.update(elapsedTime, gamePane.getWidth(), gamePane.getHeight());
        }

        // Update infected cells
        Iterator<Infected> infectedIterator = infectedCells.iterator();
        while (infectedIterator.hasNext()) {
            Infected infected = infectedIterator.next();
            infected.update(elapsedTime, gamePane.getWidth(), gamePane.getHeight());

            // Check if infected cell should transform into viruses
            if (infected.getBounceCount() >= 2) {
                // Create two viruses at this location
                for (int i = 0; i < GameConstants.VIRUS_DIVISION; i++) {
                    Virus virus = new Virus(infected.getX(), infected.getY());
                    virusCells.add(virus);
                    gamePane.getChildren().add(virus);
                }

                // Remove the infected cell
                gamePane.getChildren().remove(infected);
                infectedIterator.remove();
            }
        }

        // Contain all NPC in NPC List
        containAllNPCs();

        // Check for collisions
        checkCollisions();

        // Update health display
        updateHealthDisplay();

        // Update cell counts
        updateCellCounts();

        // Check game status
        checkGameStatus();
    }

    private void containAllNPCs() {
        NPCs.clear();
        NPCs.addAll(normalCells);
        NPCs.addAll(infectedCells);
        NPCs.addAll(bacteriaCells);
        NPCs.addAll(virusCells);
    }

    private void checkCollisions() {
        // Check collisions between player and all bacteria cells
        for (Bacteria bacteria : new ArrayList<>(bacteriaCells)) {
            // Replace the existing intersects check with continuous contact handling
            handleContinuousContactWithPlayer(player, bacteria);
            if (!bacteria.isAlive()) {
                gamePane.getChildren().remove(bacteria);
                bacteriaCells.remove(bacteria);
            }
        }

        // Check collisions between player and all virus cells
        for (Virus virus : new ArrayList<>(virusCells)) {
            // Replace the existing intersects check with continuous contact handling
            handleContinuousContactWithPlayer(player, virus);
            if (!virus.isAlive()) {
                gamePane.getChildren().remove(virus);
                virusCells.remove(virus);
            }
        }

        for (Normal normal : new ArrayList<>(normalCells)) {
            if (normal.intersects(player)) {
                player.handleCellCollision(normal);
            }
        }

        for (Infected infected : new ArrayList<>(infectedCells)) {
            if (infected.intersects(player)) {
                player.handleCellCollision(infected);
            }
        }

        // Check bacteria collisions with normal cells
        for (Bacteria bacteria : new ArrayList<>(bacteriaCells)) {
            if (!bacteria.isAlive()) continue;

            Iterator<Normal> normalIterator = normalCells.iterator();
            while (normalIterator.hasNext()) {
                Normal normal = normalIterator.next();
                if (bacteria.intersects(normal)) {
                    bacteria.collideWithNormalCell(normal);

                    if (!normal.isAlive()) {
                        // Convert to bacteria
                        gamePane.getChildren().remove(normal);
                        normalIterator.remove();

                        Bacteria newBacteria = new Bacteria(normal.getX(), normal.getY());
                        bacteriaCells.add(newBacteria);
                        gamePane.getChildren().add(newBacteria);
                    }
                }
            }
        }

        // Check virus collisions with normal cells
        for (Virus virus : new ArrayList<>(virusCells)) {
            if (!virus.isAlive()) continue;

            for (Normal normal : new ArrayList<>(normalCells)) {
                if (virus.intersects(normal) && !normal.isInfected()) {
                    // Infect the normal cell
                    virus.collideWithNormalCell(normal);

                    if (!virus.isAlive()) {
                        gamePane.getChildren().remove(virus);
                        virusCells.remove(virus);

                        // Create an infected cell to replace the normal cell
                        Infected infectedCell = new Infected(normal.getX(), normal.getY());
                        gamePane.getChildren().remove(normal);
                        normalCells.remove(normal);

                        infectedCells.add(infectedCell);
                        gamePane.getChildren().add(infectedCell);

                        break; // Virus is used up
                    }
                }
            }
        }

        // Check NPC collisions with each other for bouncing
        checkNPCCollisions();
    }

    private void checkNPCCollisions() {
        // Normal cells collide with each other
        for (int i = 0; i < NPCs.size(); i++) {
            for (int j = i + 1; j < NPCs.size(); j++) {
                Cell cell1 = NPCs.get(i);
                Cell cell2 = NPCs.get(j);

                if (cell1.intersects(cell2)) {
                    cell1.collideWith(cell2);
                    cell2.collideWith(cell1);
                }
            }
        }
    }

    private void reproduceNormalCells() {
        int initialCount = normalCells.size();

        // Each normal cell has a chance to reproduce
        for (Normal cell : new ArrayList<>(normalCells)) {
            // 25% chance to reproduce if not infected
            if (!cell.isInfected() && random.nextInt(4) == 0) {
                double newX = cell.getX() + (Math.random() * 40 - 20);
                double newY = cell.getY() + (Math.random() * 40 - 20);
                newX = Math.max(0, Math.min(gamePane.getWidth() - 30, newX));
                newY = Math.max(0, Math.min(gamePane.getHeight() - 30, newY));

                Normal newCell = new Normal(newX, newY);
                normalCells.add(newCell);
                gamePane.getChildren().add(newCell);
            }
        }

        if (normalCells.size() > initialCount) {
            updateCellCounts();
        }
    }

    private void placeRandomCell(Cell cell) {
        // Add padding to prevent spawning too close to edges
        double padding = cell.getRadius() * 3;
        double x = padding + Math.random() * (gamePane.getWidth() - padding * 2);
        double y = padding + Math.random() * (gamePane.getHeight() - padding * 2);

        // Ensure we don't place too close to player
        double playerX = player.getX();
        double playerY = player.getY();
        double distance = Math.sqrt(Math.pow(x - playerX, 2) + Math.pow(y - playerY, 2));

        if (distance < 100) {
            // Try again if too close to player
            placeRandomCell(cell);
            return;
        }

        cell.setPosition(x, y);
        gamePane.getChildren().add(cell);

        if (cell instanceof Normal) {
            normalCells.add((Normal) cell);
        } else if (cell instanceof Bacteria) {
            bacteriaCells.add((Bacteria) cell);
        } else if (cell instanceof Virus) {
            virusCells.add((Virus) cell);
        } else if (cell instanceof Infected) {
            infectedCells.add((Infected) cell);
        }

        // Ensure all cells start with a valid direction
        cell.randomizeDirection();
    }

    private void updateCellCounts() {
        normalCellsCount.setText(String.valueOf(normalCells.size()));
        bacteriaCount.setText(String.valueOf(bacteriaCells.size()));
        virusCount.setText(String.valueOf(virusCells.size() + infectedCells.size()));
    }

    private void updateHealthDisplay() {
        healthLabel.setText("Health: " + player.getHealth());
    }

    private void checkGameStatus() {
        if (!gameRunning) return;

        if (bacteriaCells.isEmpty() && virusCells.isEmpty() && infectedCells.isEmpty()) {
            endGame(true);
        } else if (normalCells.isEmpty() || !player.isAlive()) {
            endGame(false);
        }
    }

    private void endGame(boolean playerWon) {
        gameRunning = false;

        if (playerWon) {
            gameStatusLabel.setText("You win! All pathogens eliminated.");
        } else {
            if (!player.isAlive()) {
                gameStatusLabel.setText("You lose! Your white blood cell has been destroyed.");
            } else {
                gameStatusLabel.setText("You lose! All normal cells have been infected or destroyed.");
            }
        }

        startButton.setDisable(false);
    }

    public void handleContinuousContactWithPlayer(Player player, Antigen antigen) {
        if (player.intersects(antigen)) {
            if (antigen.isInvincible()) {
                // No interaction while antigen is invincible
                return;
            }

            if (player.isShieldActive()) {
                // Shield active, apply antigen effects
                antigen.onShieldContact(player);
            } else if (!player.isInvincible()) {
                // No shield, player takes damage if not invincible
                player.takeDamage(antigen.getDamage());
            }
        }
    }
}