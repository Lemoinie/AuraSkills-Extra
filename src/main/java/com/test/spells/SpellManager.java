package com.test.spells;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class SpellManager {

    private final Plugin plugin;
    private final SpellDataStorage storage;
    private final Map<UUID, Set<Spell>> playerLearnedSpells = new HashMap<>();
    private final Map<UUID, List<Spell>> playerEquippedSpells = new HashMap<>();
    private final Map<UUID, Integer> activeSpellIndex = new HashMap<>();
    private final Map<UUID, Long> surgeDiscount = new HashMap<>();
    private final Map<UUID, Map<Spell, Long>> cooldowns = new HashMap<>();

    public SpellManager(Plugin plugin) {
        this.plugin = plugin;
        this.storage = new SpellDataStorage(plugin);
        this.playerLearnedSpells.putAll(storage.loadAllLearned());
        this.playerEquippedSpells.putAll(storage.loadAllEquipped());
        loadConfig();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "spells.yml");
        if (!configFile.exists()) {
            plugin.saveResource("spells.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        for (SpellTier tier : SpellTier.values()) {
            String path = "tiers." + tier.name();
            if (config.contains(path)) {
                int xpCost = config.getInt(path + ".xp_cost", tier.getXpCost());
                int wisdomReq = config.getInt(path + ".wisdom_requirement", tier.getWisdomRequirement());
                boolean learnable = config.getBoolean(path + ".learnable", tier.isLearnable());
                boolean usable = config.getBoolean(path + ".usable", tier.isUsable());
                
                tier.setXpCost(xpCost);
                tier.setWisdomRequirement(wisdomReq);
                tier.setLearnable(learnable);
                tier.setUsable(usable);
            }
        }
    }

    public Set<Spell> getLearnedSpells(Player player) {
        return playerLearnedSpells.getOrDefault(player.getUniqueId(), new HashSet<>());
    }

    public List<Spell> getEquippedSpells(Player player) {
        return playerEquippedSpells.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    public boolean isLearned(Player player, Spell spell) {
        return getLearnedSpells(player).contains(spell);
    }

    public boolean isEquipped(Player player, Spell spell) {
        return getEquippedSpells(player).contains(spell);
    }

    public void learnSpell(Player player, Spell spell) {
        UUID uuid = player.getUniqueId();
        Set<Spell> learned = playerLearnedSpells.computeIfAbsent(uuid, k -> new HashSet<>());
        learned.add(spell);
        savePlayer(player);
    }

    public void equipSpell(Player player, Spell spell) {
        UUID uuid = player.getUniqueId();
        List<Spell> equipped = playerEquippedSpells.computeIfAbsent(uuid, k -> new ArrayList<>());
        if (equipped.size() < 6 && !equipped.contains(spell)) {
            equipped.add(spell);
            savePlayer(player);
        }
    }

    public void unequipSpell(Player player, Spell spell) {
        UUID uuid = player.getUniqueId();
        List<Spell> equipped = playerEquippedSpells.get(uuid);
        if (equipped != null) {
            equipped.remove(spell);
            // Reset index if it goes out of bounds
            int index = activeSpellIndex.getOrDefault(uuid, 0);
            if (equipped.isEmpty()) {
                activeSpellIndex.remove(uuid);
            } else if (index >= equipped.size()) {
                activeSpellIndex.put(uuid, 0);
            }
            savePlayer(player);
        }
    }

    public Spell getActiveSpell(Player player) {
        List<Spell> equipped = getEquippedSpells(player);
        if (equipped.isEmpty()) return null;
        int index = activeSpellIndex.getOrDefault(player.getUniqueId(), 0);
        if (index >= equipped.size()) {
            index = 0;
            activeSpellIndex.put(player.getUniqueId(), 0);
        }
        return equipped.get(index);
    }

    public void cycleSpell(Player player) {
        List<Spell> equipped = getEquippedSpells(player);
        if (equipped.isEmpty()) return;
        int nextIndex = (activeSpellIndex.getOrDefault(player.getUniqueId(), 0) + 1) % equipped.size();
        activeSpellIndex.put(player.getUniqueId(), nextIndex);
    }

    public void setSurgeDiscount(Player player) {
        surgeDiscount.put(player.getUniqueId(), System.currentTimeMillis() + 5000);
    }

    public boolean hasSurgeDiscount(Player player) {
        Long expiry = surgeDiscount.get(player.getUniqueId());
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            surgeDiscount.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void consumeSurgeDiscount(Player player) {
        surgeDiscount.remove(player.getUniqueId());
    }

    public void setCooldown(Player player, Spell spell, long seconds) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                 .put(spell, System.currentTimeMillis() + (seconds * 1000));
    }

    public long getRemainingCooldown(Player player, Spell spell) {
        Map<Spell, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;
        Long end = playerCooldowns.get(spell);
        if (end == null) return 0;
        long remaining = end - System.currentTimeMillis();
        return remaining > 0 ? (remaining / 1000) + 1 : 0;
    }

    public boolean isOnCooldown(Player player, Spell spell) {
        return getRemainingCooldown(player, spell) > 0;
    }

    private void savePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        storage.savePlayerSpells(uuid, getLearnedSpells(player), getEquippedSpells(player));
    }

    public boolean canLearn(Player player, Spell spell) {
        SpellTier tier = spell.getTier();
        if (!tier.isLearnable()) return false;

        // Check Wisdom Req
        dev.aurelium.auraskills.api.user.SkillsUser user = dev.aurelium.auraskills.api.AuraSkillsApi.get().getUser(player.getUniqueId());
        if (user == null) return false;
        
        double wisdom = user.getStatLevel(dev.aurelium.auraskills.api.stat.Stats.WISDOM);
        if (wisdom < tier.getWisdomRequirement()) return false;

        // Check XP Cost (Minecraft Levels for now, as it's common for "costs")
        if (player.getLevel() < tier.getXpCost()) return false;

        return true;
    }

    public static class TierConfig {
        public int xpCost;
        public int wisdomRequirement;
        public boolean learnable;
        public boolean usable;
    }
}
