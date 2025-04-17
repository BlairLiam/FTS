package com.example.fts;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

// Base Cell class - all cells derive from this
public abstract class Cell extends Pane {
    protected double x;
    protected double y;
    protected Circle circle;
    protected double speed;
    protected double size;
    protected double directionX, directionY;
    protected boolean isAlive = true;

    public Cell(double x, double y, Color color, double speed, double size) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.size = size;

        // Initialize direction
        randomizeDirection();

        // Create visual representation
        circle = new Circle(size);
        circle.setFill(color);
        circle.setCenterX(size);
        circle.setCenterY(size);
        getChildren().add(circle);

        // Set position
        relocate(x, y);
    }

    protected void randomizeDirection() {
        double angle = Math.random() * 2 * Math.PI;
        directionX = Math.cos(angle);
        directionY = Math.sin(angle);

        // Ensure minimum vector components to prevent near-zero movement
        if (Math.abs(directionX) < 0.1) {
            directionX = (directionX >= 0) ? 0.1 : -0.1;
        }
        if (Math.abs(directionY) < 0.1) {
            directionY = (directionY >= 0) ? 0.1 : -0.1;
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        relocate(x, y);
    }

    public double getRadius() {
        return size;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public boolean intersects(Cell other) {
        // If either cell is a Player and the other is NPC (not antigen), no intersection
        if ((this instanceof Player && (other instanceof Normal || other instanceof Infected)) ||
                (other instanceof Player && (this instanceof Normal || this instanceof Infected))) {
            return false;
        }

        // Regular intersection check
        double dx = (this.x + this.size) - (other.x + other.size);
        double dy = (this.y + this.size) - (other.y + other.size);
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (this.getRadius() + other.getRadius());
    }

    public void move(double elapsedTime, double maxX, double maxY) {
        double dx = directionX * speed * elapsedTime;
        double dy = directionY * speed * elapsedTime;

        double newX = x + dx;
        double newY = y + dy;

        // Improved wall collision handling
        boolean hitWall = false;

        if (newX <= 0) {
            directionX = Math.abs(directionX); // Force positive X direction
            newX = 1; // Push slightly away from wall
            hitWall = true;
        } else if (newX >= maxX - 2 * size) {
            directionX = -Math.abs(directionX); // Force negative X direction
            newX = maxX - 2 * size - 1; // Push slightly away from wall
            hitWall = true;
        }

        if (newY <= 0) {
            directionY = Math.abs(directionY); // Force positive Y direction
            newY = 1; // Push slightly away from wall
            hitWall = true;
        } else if (newY >= maxY - 2 * size) {
            directionY = -Math.abs(directionY); // Force negative Y direction
            newY = maxY - 2 * size - 1; // Push slightly away from wall
            hitWall = true;
        }

        if (hitWall) {
            onBounce();
        }

        setPosition(newX, newY);
    }

    // Called when this cell bounces off a wall or another cell
    protected void onBounce() {
        // Override in subclasses if needed
    }

    // Handle collision with another cell
    public void collideWith(Cell other) {
        // Calculate vector from other cell to this cell
        double dx = (this.x + this.size) - (other.x + other.size);
        double dy = (this.y + this.size) - (other.y + other.size);

        // Normalize the direction vector
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            dx /= length;
            dy /= length;
        }

        // Set new direction based on reflection
        directionX = dx;
        directionY = dy;

        // Ensure we don't get stuck in collision
        double pushDistance = (this.getRadius() + other.getRadius() - length + 1);
        if (pushDistance > 0) {
            this.x += dx * pushDistance / 2;
            this.y += dy * pushDistance / 2;
            this.relocate(this.x, this.y);
        }

        // Call the bounce handler
        onBounce();
    }

    public void setDirection(double dirX, double dirY) {
        this.directionX = dirX;
        this.directionY = dirY;
    }

    public abstract void update(double elapsedTime, double maxX, double maxY);
}

// NPC Cell - base class for all non-player cells
abstract class NPC extends Cell {
    public NPC(double x, double y, Color color, double speed, double size) {
        super(x, y, color, speed, size);
    }

    @Override
    public void update(double elapsedTime, double maxX, double maxY) {
        move(elapsedTime, maxX, maxY);
    }
}

// Antigen Cell - base class for hostile cells (Bacteria and Viruses)
abstract class Antigen extends NPC {
    protected boolean invincible = false;
    protected double invincibilityTimer = 0;
    protected double flickerTimer = 0;
    protected Color originalColor;

    public Antigen(double x, double y, Color color, double speed, double size) {
        super(x, y, color, speed, size);
        originalColor = color;
    }

    public int getDamage() {
        return GameConstants.ANTIGEN_DAMAGE;
    }

    public boolean isInvincible() {
        return invincible;
    }

    protected void makeInvincible() {
        invincible = true;
        invincibilityTimer = GameConstants.ANTIGEN_INVINCIBILITY_DURATION;
        // Store original color for flickering
        originalColor = (Color) circle.getFill();
    }

    @Override
    public void update(double elapsedTime, double maxX, double maxY) {
        // Update invincibility timer and handle flickering
        if (invincible) {
            invincibilityTimer -= elapsedTime;
            flickerTimer -= elapsedTime;

            // Handle flickering effect
            if (flickerTimer <= 0) {
                if (circle.getOpacity() == 1.0) {
                    circle.setOpacity(0.5);
                } else {
                    circle.setOpacity(1.0);
                }
                flickerTimer = GameConstants.ANTIGEN_FLICKER_INTERVAL;
            }

            // End invincibility when timer expires
            if (invincibilityTimer <= 0) {
                invincible = false;
                circle.setOpacity(1.0);
            }
        }

        // Regular movement
        move(elapsedTime, maxX, maxY);
    }

    // Method to handle shield contact
    public void onShieldContact(Player player) {
        if (!invincible) {
            // Calculate vector from player to this antigen
            double dx = (this.x + player.size) - (player.getX() + player.getRadius());
            double dy = (this.y + player.size) - (player.getY() + player.getRadius());

            // Normalize
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length > 0) {
                dx /= length;
                dy /= length;
            }

            // Apply stronger repulsion
            this.setDirection(dx * 2, dy * 2);

            // Handle specific damage
            onShieldContact();

            // Activate invincibility
            makeInvincible();
        }
    }

    // Abstract methods to be implemented by subclasses
    public abstract void onShieldContact();
    public abstract void collideWithNormalCell(Normal normalCell);
}

// Player (White Blood Cell)
class Player extends Cell {
    private boolean movingUp, movingDown, movingLeft, movingRight;
    private boolean shieldActive = false;
    private double shieldTimer = 0;
    private double shieldCooldown = 0;
    private int health = GameConstants.PLAYER_INITIAL_HEALTH;

    // Invincibility frame properties
    private boolean invincible = false;
    private double invincibilityTimer = 0;
    private double flickerTimer = 0;
    private Color originalColor;

    public Player(double x, double y) {
        super(x, y, Color.WHITE, GameConstants.PLAYER_SPEED, GameConstants.PLAYER_SIZE);
        originalColor = Color.WHITE;
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2);
    }

    public void setMovingUp(boolean moving) {
        movingUp = moving;
    }

    public void setMovingDown(boolean moving) {
        movingDown = moving;
    }

    public void setMovingLeft(boolean moving) {
        movingLeft = moving;
    }

    public void setMovingRight(boolean moving) {
        movingRight = moving;
    }

    public void activateShield() {
        if (shieldCooldown <= 0) {
            shieldActive = true;
            shieldTimer = GameConstants.SHIELD_DURATION;
            // Visual change to indicate shield is active
            circle.setStroke(Color.CYAN);
            circle.setStrokeWidth(4);
        }
    }

    public boolean isShieldActive() {
        return shieldActive;
    }

    public boolean isInvincible() {
        return invincible;
    }

    private void activateInvincibility() {
        invincible = true;
        invincibilityTimer = GameConstants.PLAYER_INVINCIBILITY_DURATION;
        flickerTimer = GameConstants.PLAYER_FLICKER_INTERVAL;
    }

    public int getHealth() {
        return health;
    }

    public void takeDamage(int amount) {
        // Only take damage if not invincible
        if (!invincible) {
            health -= amount;
            if (health <= 0) {
                health = 0;
                setAlive(false);
            }

            // Activate invincibility frame
            activateInvincibility();
        }
    }

    @Override
    public void update(double elapsedTime, double maxX, double maxY) {
        // Update shield timer and cooldown
        if (shieldActive) {
            shieldTimer -= elapsedTime;
            if (shieldTimer <= 0) {
                shieldActive = false;
                shieldCooldown = GameConstants.SHIELD_COOLDOWN;
                // Visual change to indicate shield is inactive
                circle.setStroke(Color.BLACK);
                circle.setStrokeWidth(2);
            }
        } else if (shieldCooldown > 0) {
            shieldCooldown -= elapsedTime;
        }

        // Update invincibility timer and handle flickering
        if (invincible) {
            invincibilityTimer -= elapsedTime;
            flickerTimer -= elapsedTime;

            // Handle flickering effect
            if (flickerTimer <= 0) {
                if (circle.getOpacity() == 1.0) {
                    circle.setOpacity(0.5);
                } else {
                    circle.setOpacity(1.0);
                }
                flickerTimer = GameConstants.PLAYER_FLICKER_INTERVAL;
            }

            // End invincibility when timer expires
            if (invincibilityTimer <= 0) {
                invincible = false;
                circle.setOpacity(1.0);
            }
        }

        // Handle movement
        double dx = 0;
        double dy = 0;

        if (movingUp) dy -= speed * elapsedTime;
        if (movingDown) dy += speed * elapsedTime;
        if (movingLeft) dx -= speed * elapsedTime;
        if (movingRight) dx += speed * elapsedTime;

        // Apply diagonal movement normalization
        if ((movingUp || movingDown) && (movingLeft || movingRight)) {
            dx *= 0.7071; // 1/sqrt(2)
            dy *= 0.7071;
        }

        // Manual movement (not using the inherited move method)
        double newX = Math.max(0, Math.min(maxX - 2 * size, x + dx));
        double newY = Math.max(0, Math.min(maxY - 2 * size, y + dy));
        setPosition(newX, newY);
    }

    public double getShieldCooldownPercentage() {
        if (shieldActive) {
            return shieldTimer / GameConstants.SHIELD_DURATION;
        } else if (shieldCooldown > 0) {
            return 1.0 - (shieldCooldown / GameConstants.SHIELD_COOLDOWN);
        } else {
            return 1.0; // Ready
        }
    }

    public void handleCellCollision(Cell other) {
        // Skip interaction with normal cells and infected cells
        if (other instanceof Normal || other instanceof Infected) {
            return;
        }

        // Handle antigens
        if (other instanceof Antigen) {
            Antigen antigen = (Antigen) other;

            // Skip if the antigen is invincible
            if (antigen.isInvincible()) {
                return;
            }

            // Calculate vector from player to antigen
            double dx = (other.getX() + other.getRadius()) - (this.x + this.getRadius());
            double dy = (other.getY() + other.getRadius()) - (this.y + this.getRadius());

            // Normalize
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length > 0) {
                dx /= length;
                dy /= length;
            }

            // Apply bounce to the antigen
            ((Cell) other).setDirection(dx, dy);

            // Handle shield and damage logic
            if (shieldActive) {
                antigen.onShieldContact(this);
            } else if (!invincible) {
                // Only take damage if not invincible
                takeDamage(antigen.getDamage());
            }
        }
    }
}

// Normal Cell
class Normal extends NPC {
    private int bounceCount = 0;
    private boolean infected = false;

    public Normal(double x, double y) {
        super(x, y, Color.GREEN, GameConstants.NORMAL_CELL_SPEED, GameConstants.NORMAL_CELL_SIZE);
    }

    public boolean isInfected() {
        return infected;
    }

    public void infect() {
        infected = true;
        circle.setFill(Color.ORANGE); // Visual indication of infection
    }

    public int getBounceCount() {
        return bounceCount;
    }

    @Override
    protected void onBounce() {
        if (infected) {
            bounceCount++;
        }
    }
}

// Bacteria Cell
class Bacteria extends Antigen {
    private int health = GameConstants.BACTERIA_HEALTH;

    public Bacteria(double x, double y) {
        super(x, y, Color.RED, GameConstants.BACTERIA_SPEED, GameConstants.BACTERIA_SIZE);
    }

    public void takeDamage() {
        health--;
        if (health <= 0) {
            setAlive(false);
        } else {
            // Visual indication of damage
            circle.setFill(Color.DARKRED);
        }
    }

    @Override
    public void onShieldContact() {
        if (!invincible) {
            takeDamage();
        }
    }

    @Override
    public void collideWithNormalCell(Normal normalCell) {
        // Convert normal cell to bacteria
        normalCell.setAlive(false); // Mark for removal

        // Bounce off
        collideWith(normalCell);
    }
}

// Virus Cell
class Virus extends Antigen {
    public Virus(double x, double y) {
        super(x, y, Color.PURPLE, GameConstants.VIRUS_SPEED, GameConstants.VIRUS_SIZE);
        // Ensure we have a non-zero initial direction
        while (Math.abs(directionX) < 0.2 || Math.abs(directionY) < 0.2) {
            randomizeDirection();
        }
    }

    @Override
    protected void randomizeDirection() {
        super.randomizeDirection();

        // Ensure minimum momentum in each direction
        if (Math.abs(directionX) < 0.2) {
            directionX = (directionX >= 0) ? 0.2 : -0.2;
        }
        if (Math.abs(directionY) < 0.2) {
            directionY = (directionY >= 0) ? 0.2 : -0.2;
        }
    }

    @Override
    public void update(double elapsedTime, double maxX, double maxY) {
        // Check if the virus is potentially stuck
        if (Math.abs(directionX) < 0.1 && Math.abs(directionY) < 0.1) {
            randomizeDirection(); // Get a new direction if nearly stopped
        }

        // Call the parent update method which handles invincibility and flickering
        super.update(elapsedTime, maxX, maxY);
    }

    @Override
    public void onShieldContact() {
        if (!invincible) {
            // Viruses are destroyed by a single shield contact
            setAlive(false);
        }
    }

    @Override
    public void collideWithNormalCell(Normal normalCell) {
        // Infect normal cell
        normalCell.infect();

        // Virus disappears after infecting
        setAlive(false);
    }
}

// Infected Normal Cell (will turn into viruses)
class Infected extends NPC {
    private int bounceCount = 0;

    public Infected(double x, double y) {
        super(x, y, Color.ORANGE, GameConstants.INFECTED_SPEED, GameConstants.INFECTED_SIZE);
    }

    @Override
    protected void onBounce() {
        bounceCount++;
        // After two bounces, it will be transformed into viruses in the GameController
    }

    public int getBounceCount() {
        return bounceCount;
    }
}