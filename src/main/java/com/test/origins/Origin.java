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
            Arrays.asList("Nocturnal Fatigue: -10% Attk/Speed & -15% XP at night")),
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
            Arrays.asList("Greedy: 40% Discount with villagers when wearing gold", "Nimble: +25% Movement Speed", "Scamper: +5% Speed per nearby entity (5 blocks)"),
            Arrays.asList("Small: -30% Max Health", "Weak: -30% Damage")),
    UNDEAD("Undead", Material.ZOMBIE_HEAD, 1.0, 0.0, 1.0, 1.0, 0.0,
            Arrays.asList("Necrotic Body: Zombies and skeletons are neutral", "Rotten Feast: Rotten flesh mends your body", "Plague Carrier: Potions effects are reversed (Harming heals, Healing hurts)"),
            Arrays.asList("Burn in Sunlight: Catch fire in daylight", "Outcast: Villagers won't trade with you", "Holy Weakness: Healing potions damage you")),
    ORC("Orc", Material.IRON_AXE, 1.3, 0.0, 1.0, 1.15, 0.0,
            Arrays.asList("Colossus: 1.3x Size", "Brutal Strength: +15% Melee damage", "Thick Hide: -20% Damage taken from all sources", "Warrior: +10% Fighting & Defense XP"),
            Arrays.asList("Intimidating: Higher villager prices", "Dull Mind: -10% Wisdom (Global XP gain limitation)")),
    DRAGONBLOOD("Dragonblood", Material.BLAZE_POWDER, 1.0, 0.1, 0.9, 1.0, 0.0,
            Arrays.asList("Fireborn: Immune to fire and lava", "Blazing Strikes: Fire attacks (inc. breath) deal +35% damage", "Fire Breath: Hold Shift + Right-Click to breathe fire (3-wide, Costs Mana)"),
            Arrays.asList("Aquatic Weakness: Water slows you", "Hydrophobia: Taking damage in water or rain (1HP/2s)")),
    FAE_TOUCHED("Fae-Touched", Material.FEATHER, 0.8, -0.1, 1.0, 1.0, 0.2,
            Arrays.asList("Wings: Can double jump", "Nocturnal: +15% Speed at night", "Nature's Mend: Natural health regeneration", "Scholar: +10% XP from Enchanting"),
            Arrays.asList("Iron Weight: Heavy armor reduces speed", "Fragile: -25% Max Health")),
    VAMPIRE("Vampire", Material.POTION, 1.0, 0.0, 1.0, 1.2, 0.0,
            Arrays.asList("Nocturnal: +20% Damage at night", "Blood Suck: Shift+Right-Click to drain 1 heart (1s CD, Costs Mana)", "Unholy Reach: +1 Reach distance", "Blood feast: Receive Regeneration I after feeding"),
            Arrays.asList("Sunlight Sensitivity: Burn in daylight", "Beetroot Weakness: Nearby beetroot weakens you", "Blood Hunger: Only heal through Blood Suck (No natural regeneration)")),
    WEREWOLF("Werewolf", Material.BONE, 1.0, -0.5, 0.9, 0.7, 0.0,
            Arrays.asList("Moon Mutation: Transform at night and full moons (Scale, HP, Speed, Damage)", "Primal Instinct: Massive unarmed combat bonus in wolf form"),
            Arrays.asList("Silver Sensitivity: Iron weapons deal double damage", "Iron Weight: Iron armor burns your skin", "Lunar Weakness: Significantly weaker during the day")),
    SIREN("Siren", Material.PRISMARINE_SHARD, 1.0, 0.0, 1.0, 1.0, 0.0,
            Arrays.asList("Aquatic: Breathe underwater & 50% faster swimming", "Ocean's Gift: Night Vision underwater", "Heat Steal: Frost aura damages & freezes (Hold Shift+RightClick, Costs Mana)", "Wave Dash: Double-click space in water to dash"),
            Arrays.asList("Dehydration: Must stay hydrated (water/rain)", "Nether Weakness: Constant fire in the Nether", "Reverse Oxygen: Drown on land", "Land Slowness: Slowness III while on land", "Fragile Grip: Cannot use shields")),
    CREEPER("Creeper", Material.GUNPOWDER, 0.8, -0.2, 1.0, 1.0, 0.0,
            Arrays.asList("Volatile: Hold Shift + Right-Click to prime, release to explode (3-stages: Base, Mana, Suicide)", "Explosive Resilience: Immune to your own explosion", "Explosive XP: +10% XP from combat skills"),
            Arrays.asList("Feline Terror: Near Feline origins, you receive Weakness II", "Fragile: -20% Max Health", "Small: 0.8x Scale")),
    WOLF("Wolf", Material.BONE, 0.75, 0.0, 1.0, 1.0, 0.0,
            Arrays.asList("Alpha: Wild wolves are passive, Tamed wolves deal +10% dmg and have +30% HP", "Pack Tactics: +2% Damage per nearby tamed wolf", "Bone Collector: Skeletons are scared of you", "Alpha Howl: Shift + Right-Click to heal pack (Costs Mana)"),
            Arrays.asList("Meat Eater: Can only eat meat", "Small Stature: -25% Size"));

    private String displayName;
    private Material icon;
    private double scale;
    private double healthModifier;
    private double speedModifier;
    private double damageModifier;
    private double luckModifier;
    private List<String> pros;
    private List<String> cons;

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

    public void update(String displayName, Material icon, double scale, double healthModifier, double speedModifier, double damageModifier, double luckModifier, List<String> pros, List<String> cons) {
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
