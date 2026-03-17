package com.test.spells;

import org.bukkit.ChatColor;

public enum SpellTier {
    COMMON("Common", ChatColor.GRAY, 50, 5, true, true),
    UNCOMMON("Uncommon", ChatColor.GREEN, 150, 15, true, true),
    RARE("Rare", ChatColor.BLUE, 400, 30, true, true),
    EPIC("Epic", ChatColor.LIGHT_PURPLE, 1000, 50, true, true),
    LEGENDARY("Legendary", ChatColor.YELLOW, 2500, 75, true, true),
    MYTHIC("Mythic", ChatColor.RED, 6000, 100, true, true);

    private final String displayName;
    private final ChatColor color;
    private int xpCost;
    private int wisdomRequirement;
    private boolean learnable;
    private boolean usable;

    SpellTier(String displayName, ChatColor color, int xpCost, int wisdomRequirement, boolean learnable, boolean usable) {
        this.displayName = displayName;
        this.color = color;
        this.xpCost = xpCost;
        this.wisdomRequirement = wisdomRequirement;
        this.learnable = learnable;
        this.usable = usable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public int getXpCost() {
        return xpCost;
    }

    public void setXpCost(int xpCost) {
        this.xpCost = xpCost;
    }

    public int getWisdomRequirement() {
        return wisdomRequirement;
    }

    public void setWisdomRequirement(int wisdomRequirement) {
        this.wisdomRequirement = wisdomRequirement;
    }

    public boolean isLearnable() {
        return learnable;
    }

    public void setLearnable(boolean learnable) {
        this.learnable = learnable;
    }

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        this.usable = usable;
    }
}
