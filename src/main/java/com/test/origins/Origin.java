package com.test.origins;

import org.bukkit.Material;
import java.util.Arrays;
import java.util.List;

public enum Origin {
    NONE("None", Material.BARRIER, 1.0, 0.0, 1.0, 1.0, 0.0,
            java.util.Collections.emptyList(),
            java.util.Collections.emptyList()),
    HUMAN("Human", Material.WHEAT, 1.0, 0.0, 1.0, 1.0, 0.0,
            Arrays.asList("Jack of all Trades: +0.1% XP Gain per Wisdom point"),
            Arrays.asList("Nocturnal Fatigue: -10% Attk/Speed & -30% XP at night")),
    FELINE("Feline", Material.ORANGE_WOOL, 0.5, -0.2, 1.0, 0.85, 0.0,
            Arrays.asList("Cat-like Reflexes: Reduced fall damage", "Scary: Creepers avoid you", "Night Hunter: Gifts when waking up from bed", "Agile: +10% XP from Agility", "Stealth: Hostile mobs find you harder to detect"),
            Arrays.asList("Small: -50% Size (Harder to hit, more fragile)", "Fragile: -20% Max Health", "Nine Lives: Slightly less damage")),
    DWARF("Dwarf", Material.IRON_PICKAXE, 0.5, 0.0, 0.9, 1.0, 0.0,
            Arrays.asList("Miner: Haste when mining", "Underground Sight: Night Vision in caves or at night", "Master Miner: +10% XP from Mining"),
            Arrays.asList("Dense: Sinks in water", "Sunlight Sensitivity: Blindness in sun without helmet", "Undergrounder: Higher trade costs", "Slow: -10% Speed")),
    WOOD_ELF("Wood Elf", Material.BOW, 1.3, -0.1, 1.0, 0.9, 0.0,
            Arrays.asList("Nature's Grace: Night Vision in forests", "Focus: Charged bow shots deal 20% more damage", "Archer: +10% XP from Archery"),
            Arrays.asList("Fragile: -10% Max Health")),
    GOBLIN("Goblin", Material.GOLD_INGOT, 0.5, -0.3, 1.25, 0.7, 0.1,
            Arrays.asList("Greedy: 40% Discount with villagers when wearing gold", "Nimble: +25% Movement Speed"),
            Arrays.asList("Small: -30% Max Health", "Weak: -30% Damage")),
    UNDEAD("Undead", Material.ROTTEN_FLESH, 1.0, 0.0, 1.0, 1.0, 0.0,
            Arrays.asList("Soul Bound: Undead mobs are neutral", "Grave Digestion: Rotten flesh is highly nutritious and gives strength", "Unholy Vitality: Harming potions heal you"),
            Arrays.asList("Sunbeam Allergy: Burn in sunlight without helmet", "Outcast: Villagers refuse to trade", "Holy Weakness: Healing potions damage you")),
    ORC("Orc", Material.IRON_AXE, 1.3, 0.0, 1.0, 1.15, 0.0,
            Arrays.asList("Colossus: 1.3x Size", "Brutal Strength: +15% Melee damage", "Thick Hide: -20% Damage taken from all sources", "Warrior: +10% Fighting & Defense XP"),
            Arrays.asList("Intimidating: Higher villager prices", "Dull Mind: -10% Wisdom (Global XP gain limitation)")),
    DRAGONBLOOD("Dragonblood", Material.BLAZE_POWDER, 1.0, 0.1, 0.9, 1.0, 0.0,
            Arrays.asList("Fireborn: Immune to fire and lava", "Blazing Strikes: Fire attacks deal +35% damage"),
            Arrays.asList("Aquatic Weakness: Water slows you", "Hydrophobia: Wetness reduces damage and speed")),
    FAE_TOUCHED("Fae-Touched", Material.FEATHER, 0.8, -0.1, 1.0, 1.0, 0.2,
            Arrays.asList("Wings: Can double jump", "Nocturnal: +15% Speed at night", "Nature's Mend: Natural health regeneration", "Scholar: +10% XP from Enchanting"),
            Arrays.asList("Iron Weight: Heavy armor reduces speed", "Fragile: -10% Max Health")),
    VAMPIRE("Vampire", Material.REDSTONE, 1.0, -0.2, 1.0, 1.0, 0.0,
            Arrays.asList("Bloodlust: +20% Damage at night", "Blood Drinker: Shift-Right-Click mobs to heal", "Immortal Soul: High natural regeneration"),
            Arrays.asList("Sun Allergy: Take massive damage in sunlight", "Weakened State: -50% Damage at day", "Beetroot Allergy: Weakness near beetroots")),
    WEREWOLF("Werewolf", Material.BONE, 1.0, -0.5, 0.9, 0.7, 0.0,
            Arrays.asList("Moon Mutation: Transform at night and full moons (Scale, HP, Speed, Damage)", "Primal Instinct: Massive unarmed combat bonus in wolf form"),
            Arrays.asList("Silver Sensitivity: Iron weapons deal double damage", "Iron Weight: Iron armor burns your skin", "Lunar Weakness: Significantly weaker during the day"));

    private final String displayName;
    private final Material icon;
    private final double scale;
    private final double healthModifier;
    private final double speedModifier;
    private final double damageModifier;
    private final double luckModifier;
    private final List<String> pros;
    private final List<String> cons;

    Origin(String displayName, Material icon, double scale, double healthModifier, double speedModifier, double damageModifier, double luckModifier, List<String> pros, List<String> cons) {
        this.displayName = displayName;
        this.icon = icon;
        this.scale = scale;
        this.healthModifier = healthModifier;
        this.speedModifier = speedModifier;
        this.damageModifier = damageModifier;
        this.luckModifier = luckModifier;
        this.pros = pros;
        this.cons = cons;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public double getScale() {
        return scale;
    }

    public double getHealthModifier() {
        return healthModifier;
    }

    public double getSpeedModifier() {
        return speedModifier;
    }

    public double getDamageModifier() {
        return damageModifier;
    }

    public double getLuckModifier() {
        return luckModifier;
    }

    public List<String> getPros() {
        return pros;
    }

    public List<String> getCons() {
        return cons;
    }
}
