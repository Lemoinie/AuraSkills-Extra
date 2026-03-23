package com.test.spells;

public enum Spell {
    KINETIC_SHOVE("Kinetic Shove", SpellTier.COMMON, "A short-range wind blast that knocks mobs back."),
    SPARK("Spark", SpellTier.COMMON, "Shoots a tiny fire projectile. Ignites players and TNT."),
    MAGE_LIGHT("Mage Light", SpellTier.COMMON, "Summons a floating light orb for 60s."),
    NATURES_ROOT("Nature's Root", SpellTier.UNCOMMON, "Freezes a single mob in place for 3 seconds."),
    FIREBOLT("Firebolt", SpellTier.UNCOMMON, "Straight fire projectile. Moderate damage + burn."),
    FROST_TOUCH("Frost Touch", SpellTier.UNCOMMON, "Slows enemy by 40% for 4s."),
    ARCANE_SURGE("Arcane Surge", SpellTier.RARE, "A dash-blink that reduces the mana cost of your next spell."),
    METEOR("Meteor", SpellTier.LEGENDARY, "Massive AOE explosion."),
    THUNDERSTORM("Thunderstorm", SpellTier.LEGENDARY, "Lightning strikes all enemies around caster multiple times."),
    STORM_CALLING("Storm Calling", SpellTier.RARE, "Changes the weather to a thunderstorm."),
    METEOR_SHOWER("Meteor Shower", SpellTier.LEGENDARY, "Drops multiple small meteors in a radius."),
    CHAIN_LIGHTNING("Chain Lightning", SpellTier.LEGENDARY, "A lightning beam that jumps between enemies."),
    MALEVOLENT_SHRINE("Malevolent Shrine", SpellTier.MYTHIC, "A devastating ritual that slashes enemies in a massive radius.");

    private String displayName;
    private SpellTier tier;
    private String description;
    private double manaCost;
    private long cooldown;
    private int xpCost;
    private int wisdomRequirement;

    Spell(String displayName, SpellTier tier, String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
        this.xpCost = tier.getXpCost();
        this.wisdomRequirement = tier.getWisdomRequirement();
        
        // Default mana costs based on tier
        if (tier == SpellTier.COMMON) this.manaCost = 15;
        else if (tier == SpellTier.UNCOMMON) this.manaCost = 30;
        else if (tier == SpellTier.RARE) this.manaCost = 60;
        else if (tier == SpellTier.LEGENDARY) this.manaCost = 150;
        else if (tier == SpellTier.MYTHIC) this.manaCost = 300;
        else this.manaCost = 20;

        // Default cooldowns
        this.cooldown = 5;
        if (this.name().equals("SPARK")) this.cooldown = 1;
        else if (this.name().equals("KINETIC_SHOVE")) this.cooldown = 3;
        else if (this.name().equals("MAGE_LIGHT")) this.cooldown = 5;
        else if (this.name().equals("NATURES_ROOT")) this.cooldown = 8;
        else if (this.name().equals("FIREBOLT")) this.cooldown = 5;
        else if (this.name().equals("FROST_TOUCH")) this.cooldown = 8;
        else if (this.name().equals("ARCANE_SURGE")) this.cooldown = 15;
        else if (this.name().equals("THUNDERSTORM")) this.cooldown = 30;
        else if (this.name().equals("STORM_CALLING")) this.cooldown = 300;
        else if (this.name().equals("METEOR")) this.cooldown = 45;
        else if (this.name().equals("METEOR_SHOWER")) this.cooldown = 60;
        else if (this.name().equals("CHAIN_LIGHTNING")) this.cooldown = 20;
        else if (this.name().equals("MALEVOLENT_SHRINE")) this.cooldown = 120;
    }

    public void update(String displayName, SpellTier tier, String description, double manaCost, long cooldown, int xpCost, int wisdomRequirement) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.xpCost = xpCost;
        this.wisdomRequirement = wisdomRequirement;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SpellTier getTier() {
        return tier;
    }

    public String getDescription() {
        return description;
    }

    public double getManaCost() {
        return manaCost;
    }

    public long getCooldown() {
        return cooldown;
    }

    public int getXpCost() {
        return xpCost;
    }

    public int getWisdomRequirement() {
        return wisdomRequirement;
    }
}
