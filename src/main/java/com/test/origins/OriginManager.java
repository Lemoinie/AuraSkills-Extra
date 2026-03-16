package com.test.origins;

import com.test.MyPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OriginManager {

    private final Map<UUID, Origin> playerOrigins = new HashMap<>();
    private boolean enabled = true;
    private final org.bukkit.NamespacedKey modifierKey;
    private final org.bukkit.plugin.Plugin plugin;
    private final OriginDataStorage storage;

    public OriginManager(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
        this.modifierKey = new org.bukkit.NamespacedKey(plugin, "origin_modifier");
        this.storage = new OriginDataStorage(plugin);
        this.playerOrigins.putAll(storage.loadAll());
    }

    public org.bukkit.plugin.Plugin getPlugin() {
        return plugin;
    }

    public org.bukkit.NamespacedKey getModifierKey() {
        return modifierKey;
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
