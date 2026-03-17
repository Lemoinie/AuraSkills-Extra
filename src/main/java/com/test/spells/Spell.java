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
    METEOR_SHOWER("Meteor Shower", SpellTier.LEGENDARY, "Drops multiple small meteors in a radius."),
    CHAIN_LIGHTNING("Chain Lightning", SpellTier.LEGENDARY, "A lightning beam that jumps between enemies.");

    private final String displayName;
    private final SpellTier tier;
    private final String description;

    Spell(String displayName, SpellTier tier, String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
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
}
