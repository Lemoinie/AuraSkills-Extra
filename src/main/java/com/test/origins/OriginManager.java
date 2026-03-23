package com.test.origins;

import com.test.MyPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OriginManager {

    private final Map<UUID, Origin> playerOrigins = new HashMap<>();
    private boolean enabled = true;
    private final org.bukkit.NamespacedKey modifierKey;
    private final org.bukkit.NamespacedKey dynamicModifierKey;
    private final org.bukkit.plugin.Plugin plugin;
    private final OriginDataStorage storage;
    private final LootTable felineSleepGifts = new LootTable();

    public OriginManager(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
        this.modifierKey = new org.bukkit.NamespacedKey(plugin, "origin_modifier");
        this.dynamicModifierKey = new org.bukkit.NamespacedKey(plugin, "origin_dynamic_modifier");
        this.storage = new OriginDataStorage(plugin);
        this.playerOrigins.putAll(storage.loadAll());
        loadConfigs();
        loadLootTables();
    }

    public void loadConfigs() {
        File configDir = new File(plugin.getDataFolder(), "config/origins");
        if (!configDir.exists()) configDir.mkdirs();

        // Master Config
        File masterFile = new File(configDir, "master.yml");
        if (masterFile.exists()) {
            YamlConfiguration master = YamlConfiguration.loadConfiguration(masterFile);
            this.enabled = master.getBoolean("enabled", true);
        }

        // Detailed Configs
        for (Origin origin : Origin.values()) {
            if (origin == Origin.NONE) continue;
            File originFile = new File(configDir, origin.name().toLowerCase() + ".yml");
            if (!originFile.exists()) {
                saveDefaultOriginConfig(origin, originFile);
            } else {
                loadOriginConfig(origin, originFile);
            }
        }
    }

    private void saveDefaultOriginConfig(Origin origin, File file) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("display_name", origin.getDisplayName());
        config.set("icon", origin.getIcon().name());
        config.set("scale", origin.getScale());
        config.set("health_modifier", origin.getHealthModifier());
        config.set("speed_modifier", origin.getSpeedModifier());
        config.set("damage_modifier", origin.getDamageModifier());
        config.set("luck_modifier", origin.getLuckModifier());
        config.set("pros", origin.getPros());
        config.set("cons", origin.getCons());
        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save default config for origin " + origin.name());
        }
    }

    private void loadOriginConfig(Origin origin, File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try {
            String name = config.getString("display_name", origin.getDisplayName());
            Material icon = Material.valueOf(config.getString("icon", origin.getIcon().name()));
            double scale = config.getDouble("scale", origin.getScale());
            double hp = config.getDouble("health_modifier", origin.getHealthModifier());
            double speed = config.getDouble("speed_modifier", origin.getSpeedModifier());
            double dmg = config.getDouble("damage_modifier", origin.getDamageModifier());
            double luck = config.getDouble("luck_modifier", origin.getLuckModifier());
            List<String> pros = config.getStringList("pros");
            List<String> cons = config.getStringList("cons");
            
            origin.update(name, icon, scale, hp, speed, dmg, luck, pros.isEmpty() ? origin.getPros() : pros, cons.isEmpty() ? origin.getCons() : cons);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load config for origin " + origin.name());
        }
    }

    private void loadLootTables() {
        File lootDir = new File(plugin.getDataFolder(), "loot");
        if (!lootDir.exists()) lootDir.mkdirs();
        
        File giftFile = new File(lootDir, "sleep_gifts.yml");
        if (!giftFile.exists()) {
            plugin.saveResource("loot/sleep_gifts.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(giftFile);
        felineSleepGifts.setGiftChance(config.getDouble("gift_chance", 0.7));
        
        List<Map<?, ?>> gifts = config.getMapList("gifts");
        for (Map<?, ?> entry : gifts) {
            try {
                Material type = Material.valueOf((String) entry.get("type"));
                Object amountObj = entry.get("amount");
                int min = 1;
                int max = 1;
                
                if (amountObj instanceof Number) {
                    min = ((Number) amountObj).intValue();
                    max = min;
                } else if (amountObj instanceof String str) {
                    if (str.contains("-")) {
                        String[] parts = str.split("-");
                        min = Integer.parseInt(parts[0].trim());
                        max = Integer.parseInt(parts[1].trim());
                    } else {
                        min = Integer.parseInt(str.trim());
                        max = min;
                    }
                }
                
                double weight = ((Number) entry.get("weight")).doubleValue();
                felineSleepGifts.addEntry(type, min, max, weight);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load loot entry: " + entry);
            }
        }
    }

    public LootTable getFelineSleepGifts() {
        return felineSleepGifts;
    }

    public org.bukkit.plugin.Plugin getPlugin() {
        return plugin;
    }

    public org.bukkit.NamespacedKey getModifierKey() {
        return modifierKey;
    }

    public org.bukkit.NamespacedKey getDynamicModifierKey() {
        return dynamicModifierKey;
    }

    public void setOrigin(Player player, Origin origin) {
        if (origin == null) {
            removeOrigin(player);
            return;
        }
        playerOrigins.put(player.getUniqueId(), origin);
        storage.setOrigin(player.getUniqueId(), origin);
        if (enabled) {
            applyAttributes(player, origin);
        }
    }

    public void removeOrigin(Player player) {
        playerOrigins.remove(player.getUniqueId());
        storage.setOrigin(player.getUniqueId(), null);
        removeAttributes(player);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            plugin.getServer().getOnlinePlayers().forEach(p -> {
                Origin o = getOrigin(p);
                if (o != null) applyAttributes(p, o);
            });
        } else {
            cleanupAllPlayers();
        }
    }

    public Origin getOrigin(Player player) {
        return playerOrigins.get(player.getUniqueId());
    }

    public boolean hasOrigin(Player player) {
        return playerOrigins.containsKey(player.getUniqueId());
    }

    public void applyAttributes(Player player, Origin origin) {
        removeAttributes(player); // Clear old first

        // Apply Scale
        AttributeInstance scale = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scale != null) {
             scale.setBaseValue(origin.getScale());
        }

        // Apply Health
        AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null && origin.getHealthModifier() != 0) {
            health.addTransientModifier(new AttributeModifier(modifierKey, origin.getHealthModifier(), AttributeModifier.Operation.ADD_SCALAR));
        }

        // Apply Speed
        AttributeInstance speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null && origin.getSpeedModifier() != 1.0) {
            speed.addTransientModifier(new AttributeModifier(modifierKey, origin.getSpeedModifier() - 1.0, AttributeModifier.Operation.ADD_SCALAR));
        }

        // Apply Damage
        AttributeInstance damage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damage != null && origin.getDamageModifier() != 1.0) {
            damage.addTransientModifier(new AttributeModifier(modifierKey, origin.getDamageModifier() - 1.0, AttributeModifier.Operation.ADD_SCALAR));
        }

        // Luck
        AttributeInstance luck = player.getAttribute(Attribute.GENERIC_LUCK);
        if (luck != null && origin.getLuckModifier() != 0) {
            luck.addTransientModifier(new AttributeModifier(modifierKey, origin.getLuckModifier(), AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    public void removeAttributes(Player player) {
        // Reset scale to default
        AttributeInstance scale = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scale != null) scale.setBaseValue(1.0);

        // Remove our specific namespaced modifiers
        removeModifier(player.getAttribute(Attribute.GENERIC_MAX_HEALTH));
        removeModifier(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED));
        removeModifier(player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE));
        removeModifier(player.getAttribute(Attribute.GENERIC_LUCK));

        // Reset flight state for Fae-Touched (non-creative)
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    private void removeModifier(AttributeInstance instance) {
        if (instance == null) return;
        AttributeModifier mod = instance.getModifier(modifierKey);
        if (mod != null) {
            instance.removeModifier(mod);
        }
    }

    public void cleanupAllPlayers() {
        plugin.getServer().getOnlinePlayers().forEach(this::removeAttributes);
    }
}
