package com.test.origins;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootTable {

    private final List<LootEntry> entries = new ArrayList<>();
    private double totalWeight = 0;
    private double giftChance = 0.7;

    public static class LootEntry {
        Material type;
        int minAmount;
        int maxAmount;
        double weight;

        public LootEntry(Material type, int minAmount, int maxAmount, double weight) {
            this.type = type;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.weight = weight;
        }
    }

    public void addEntry(Material type, int minAmount, int maxAmount, double weight) {
        entries.add(new LootEntry(type, minAmount, maxAmount, weight));
        totalWeight += weight;
    }

    public void setGiftChance(double giftChance) {
        this.giftChance = giftChance;
    }

    public double getGiftChance() {
        return giftChance;
    }

    public ItemStack getRandomItem() {
        if (entries.isEmpty() || totalWeight <= 0) return null;

        double r = new Random().nextDouble() * totalWeight;
        double cumulative = 0;

        for (LootEntry entry : entries) {
            cumulative += entry.weight;
            if (r <= cumulative) {
                int amount = entry.minAmount;
                if (entry.maxAmount > entry.minAmount) {
                    amount = new Random().nextInt(entry.maxAmount - entry.minAmount + 1) + entry.minAmount;
                }
                return new ItemStack(entry.type, amount);
            }
        }
        return null;
    }
}
