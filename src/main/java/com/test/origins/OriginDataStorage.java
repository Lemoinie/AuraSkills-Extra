package com.test.origins;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class OriginDataStorage {

    private final Plugin plugin;
    private final File file;
    private FileConfiguration config;

    public OriginDataStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/player_origins.yml");
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
            plugin.getLogger().log(Level.SEVERE, "Could not save player origins to " + file.getName(), e);
        }
    }

    public void setOrigin(UUID uuid, Origin origin) {
        if (origin == null) {
            config.set(uuid.toString(), null);
        } else {
            config.set(uuid.toString(), origin.name());
        }
        save();
    }

    public Origin getOrigin(UUID uuid) {
        String name = config.getString(uuid.toString());
        if (name == null) return null;
        try {
            return Origin.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Map<UUID, Origin> loadAll() {
        Map<UUID, Origin> origins = new HashMap<>();
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Origin origin = getOrigin(uuid);
                if (origin != null) {
                    origins.put(uuid, origin);
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return origins;
    }
}
