package com.test.spells;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class SpellDataStorage {

    private final Plugin plugin;
    private final File file;
    private FileConfiguration config;

    public SpellDataStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/player_spells.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        load();
    }

    public void load() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player spells to " + file.getName(), e);
        }
    }

    public void savePlayerSpells(UUID uuid, Set<Spell> learned, List<Spell> equipped) {
        String path = uuid.toString();
        
        List<String> learnedNames = new ArrayList<>();
        for (Spell spell : learned) {
            learnedNames.add(spell.name());
        }
        config.set(path + ".learned", learnedNames);

        List<String> equippedNames = new ArrayList<>();
        for (Spell spell : equipped) {
            equippedNames.add(spell.name());
        }
        config.set(path + ".equipped", equippedNames);
        
        save();
    }

    public Set<Spell> getLearnedSpells(UUID uuid) {
        List<String> names = config.getStringList(uuid.toString() + ".learned");
        Set<Spell> spells = new HashSet<>();
        for (String name : names) {
            try {
                spells.add(Spell.valueOf(name));
            } catch (IllegalArgumentException ignored) {}
        }
        return spells;
    }

    public List<Spell> getEquippedSpells(UUID uuid) {
        List<String> names = config.getStringList(uuid.toString() + ".equipped");
        List<Spell> spells = new ArrayList<>();
        for (String name : names) {
            try {
                spells.add(Spell.valueOf(name));
            } catch (IllegalArgumentException ignored) {}
        }
        return spells;
    }

    public Map<UUID, Set<Spell>> loadAllLearned() {
        Map<UUID, Set<Spell>> allLearned = new HashMap<>();
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                allLearned.put(uuid, getLearnedSpells(uuid));
            } catch (IllegalArgumentException ignored) {}
        }
        return allLearned;
    }

    public Map<UUID, List<Spell>> loadAllEquipped() {
        Map<UUID, List<Spell>> allEquipped = new HashMap<>();
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                allEquipped.put(uuid, getEquippedSpells(uuid));
            } catch (IllegalArgumentException ignored) {}
        }
        return allEquipped;
    }
}
