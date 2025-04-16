package com.example.fts;

// Constants class for easy property adjustments
public class GameConstants {
    // Cell properties
    public static final double CELL_RADIUS = 20;

    // Player properties
    public static final double PLAYER_SPEED = 150;
    public static final int PLAYER_INITIAL_HEALTH = 100;
    public static final double SHIELD_DURATION = 5.0;
    public static final double SHIELD_COOLDOWN = 10.0;
    public static final double PLAYER_INVINCIBILITY_DURATION = 5.0;
    public static final double PLAYER_FLICKER_INTERVAL = 0.15;

    // Antigen properties
    public static final int ANTIGEN_DAMAGE = 10;
    public static final double ANTIGEN_INVINCIBILITY_DURATION = 2.0;
    public static final double ANTIGEN_FLICKER_INTERVAL = 0.15;

    // Cell speeds
    public static final double NORMAL_CELL_SPEED = 40;
    public static final double BACTERIA_SPEED = 40;
    public static final double VIRUS_SPEED = 20;
    public static final double INFECTED_SPEED = 15;

    // Game mechanics
    public static final int BACTERIA_HEALTH = 2;
    public static final int INFECTED_BOUNCE_TRANSFORM = 2;
}
