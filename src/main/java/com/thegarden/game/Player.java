package com.thegarden.game;

import java.util.*;

public class Player {
    public String id;
    public String name;
    public String seed; // "hot" or "cold"
    public int temp = 2;
    public int water = 2;
    public int coins = 2;
    public int fertActions = 0;
    public double fertBonus = 0.0;

    public String currentAction; // "temp", "water", "fert"
    public String waterChoice;   // "share" or "steal"
    public boolean actionSubmitted = false;

    public int currentBid = 0;
    public boolean bidSubmitted = false;
    public String bidEventChoice;

    public int actionCardsBought = 0;
    public String actionCardTarget; // "temp" or "water"
    public String actionCardWaterChoice;

    public List<String> personalLog = new ArrayList<>(); // Private events only visible to this player

    public Player() {}
    public Player(String id, String name, String seed) {
        this.id = id; this.name = name; this.seed = seed;
    }

    public double getScore() {
        return (temp * water) + (fertBonus * 1.5);
    }
}
