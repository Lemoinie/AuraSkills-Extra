package com.test.origins;

import dev.aurelium.auraskills.api.event.skill.XpGainEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import com.destroystokyo.paper.event.entity.CreeperIgniteEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class OriginListener implements Listener {

    private final OriginManager originManager;
    private final Map<UUID, Long> attackedCreepers = new HashMap<>(); // UUID, Time
    private final Map<UUID, Long> bowChargeStart = new HashMap<>();
    private final Map<UUID, Long> sirenWaterTimer = new HashMap<>();
    private final Map<UUID, Long> bloodSuckCooldown = new HashMap<>();
    private final Map<UUID, Long> lastRightClick = new HashMap<>();
    private final Map<UUID, Long> creeperIgniteStart = new HashMap<>();
    private final Map<UUID, Integer> creeperNotificationStage = new HashMap<>(); // UUID, Stage (0: None, 1: Priming, 2:
                                                                                 // Mana, 3: Suicide)

    public OriginListener(OriginManager originManager) {
        this.plugin = originManager.getPlugin();
        this.originManager = originManager;
        startGlobalPerkTask();
        startDragonFireTask();
        startHeatStealTask();
        startCreeperIgniteTask();
        startWolfPackTask();
    }

    private final org.bukkit.plugin.Plugin plugin;

    private void startGlobalPerkTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(originManager.getPlugin(), () -> {
            if (!originManager.isEnabled())
                return;
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                Origin origin = originManager.getOrigin(player);
                if (origin == null || origin == Origin.NONE)
                    continue;

                // --- UNDEAD: Burning ---
                if (origin == Origin.UNDEAD) {
                    if (player.getWorld().getTime() < 12000 && player.getLocation().getBlock().getLightFromSky() > 10) {
                        if (player.getInventory().getHelmet() == null) {
                            player.setFireTicks(40);
                        }
                    }
                }

                // --- ORC: Battle Rage ---
                if (origin == Origin.ORC) {
                    org.bukkit.attribute.AttributeInstance health = player
                            .getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                    if (health != null && player.getHealth() / health.getValue() <= 0.4) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false, true));
                    }
                }

                // --- DRAGONBLOOD: Water Debuffs ---
                if (origin == Origin.DRAGONBLOOD) {
                    if (player.isInWater() || (player.getWorld().hasStorm()
                            && player.getLocation().getBlock().getLightFromSky() > 10)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false, false));
                        // Damage every 2 seconds (since task runs every 1s, we check if time is even)
                        if (player.getWorld().getTime() % 2 == 0) {
                            player.damage(1.0);
                        }
                    }
                }

                // --- FAE-TOUCHED: Night Speed & Regen ---
                if (origin == Origin.FAE_TOUCHED) {
                    if (player.getWorld().getTime() >= 12000) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, false));
                    }
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false, false));

                    double weight = getArmorWeight(player);
                    if (weight > 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, (int) (weight * 2),
                                false, false, false));
                    }
                }

                // --- DWARF: Sunlight & Night Vision ---
                if (origin == Origin.DWARF) {
                    if (player.getWorld().getTime() < 12000 && player.getLocation().getBlock().getLightFromSky() > 10) {
                        if (player.getInventory().getHelmet() == null) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false));
                        }
                    }
                    if (player.getWorld().getTime() >= 12000
                            || player.getLocation().getBlock().getLightFromSky() <= 10) {
                        player.addPotionEffect(
                                new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false, false));
                    } else {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    }
                }

                // --- HUMAN: Nocturnal Fatigue ---
                if (origin == Origin.HUMAN) {
                    if (player.getWorld().getTime() >= 12000) {
                        updateTransientModifier(
                                player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE),
                                originManager.getModifierKey(), -0.1,
                                org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
                        updateTransientModifier(
                                player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED),
                                originManager.getModifierKey(), -0.1,
                                org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
                    } else {
                        updateTransientModifier(
                                player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE),
                                originManager.getModifierKey(), 0.0,
                                org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
                        updateTransientModifier(
                                player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED),
                                originManager.getModifierKey(), 0.0,
                                org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
                    }
                }

                // --- CREEPER: Feline Weakness ---
                if (origin == Origin.CREEPER) {
                    boolean nearFeline = false;
                    for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
                        if (entity instanceof Player other && originManager.getOrigin(other) == Origin.FELINE) {
                            nearFeline = true;
                            break;
                        }
                    }
                    if (nearFeline) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1, false, false));
                    }
                }

                // --- WOOD_ELF: Forest Vision ---
                if (origin == Origin.WOOD_ELF) {
                    Biome biome = player.getLocation().getBlock().getBiome();
                    if (biome.name().contains("FOREST")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
                    }
                }

                // --- VAMPIRE: Regen & Sunlight & Beetroot ---
                if (origin == Origin.VAMPIRE) {
                    if (player.getWorld().getTime() < 12000 && player.getLocation().getBlock().getLightFromSky() > 10) {
                        if (player.getInventory().getHelmet() == null) {
                            player.damage(4.0);
                            player.setFireTicks(40);
                        }
                    }
                    // Removed constant Regeneration I

                    // Beetroot Weakness
                    for (Player nearby : player.getWorld().getPlayers()) {
                        if (nearby.getLocation().distanceSquared(player.getLocation()) <= 25) {
                            if (hasItem(nearby, Material.BEETROOT)) {
                                player.addPotionEffect(
                                        new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, false, false));
                                break;
                            }
                        }
                    }
                }

                // --- GOBLIN: Scamper ---
                if (origin == Origin.GOBLIN) {
                    int nearbyCount = 0;
                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            nearbyCount++;
                        }
                    }
                    nearbyCount = Math.min(nearbyCount, 10);
                    updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED),
                            originManager.getDynamicModifierKey(), nearbyCount * 0.05,
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
                }

                // --- WEREWOLF: Mutations & Iron Armor ---
                if (origin == Origin.WEREWOLF) {
                    updateWerewolfState(player);
                    if (isWearingIron(player)) {
                        player.damage(1.0);
                        player.sendMessage("§cThe iron burns your skin!");
                    }
                }

                // --- SIREN: Aquatic & Thermal ---
                if (origin == Origin.SIREN) {
                    updateSirenState(player);
                }
            }
        }, 20L, 20L);
    }

    private boolean hasItem(Player player, Material material) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material)
                return true;
        }
        return false;
    }

    private boolean isWearingIron(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType().name().contains("IRON"))
                return true;
        }
        return false;
    }

    private boolean isUndead(LivingEntity entity) {
        return entity.getType().name().contains("ZOMBIE") ||
                entity.getType().name().contains("SKELETON") ||
                entity.getType().name().contains("WITHER") ||
                entity.getType().name().contains("PHANTOM") ||
                entity.getType().name().contains("STRAY") ||
                entity.getType().name().contains("HURK"); // Generic catch for undead variants
    }

    private void updateWerewolfState(Player player) {
        long time = player.getWorld().getTime();
        boolean isNight = time >= 12000;
        boolean isFullMoon = (player.getWorld().getFullTime() / 24000) % 8 == 0;

        double scale = 1.0;
        double health = -0.5;
        double speed = 0.9;
        double damage = 0.7;
        double attackSpeed = 0.0;

        if (isNight) {
            if (isFullMoon) {
                scale = 2.0;
                health = 0.5;
                speed = 1.2;
                damage = 1.2;
                attackSpeed = 0.75;
            } else {
                scale = 1.5;
                health = 0.3;
                speed = 1.1;
                damage = 1.1;
                attackSpeed = 0.6;
            }
        }

        applyDynamicAttributes(player, scale, health, speed, damage, attackSpeed);
    }

    private void updateSirenState(Player player) {
        // Water Perks
        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 40, 0, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 40, 0, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, false, false, false)); // 20%
                                                                                                                   // faster
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                player.setAllowFlight(true);
            }
        } else if (originManager.getOrigin(player) == Origin.SIREN
                && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            player.setAllowFlight(false);
            player.setFlying(false);
            // Land slowness
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false, false));
        }

        // Reverse Air Bubble
        if (!player.isInWater()) {
            int respiration = player.getInventory().getHelmet() != null ? player.getInventory().getHelmet()
                    .getEnchantmentLevel(org.bukkit.enchantments.Enchantment.RESPIRATION) : 0;

            // On land, bubble pops every tick roughly, but we are in a 1s task.
            // Air is usually 300. Max is 300.
            int currentAir = player.getRemainingAir();
            int decrease = 30 / (respiration + 1); // Vanilla respiration formula roughly
            player.setRemainingAir(Math.max(-20, currentAir - decrease));

            if (player.getRemainingAir() <= 0) {
                player.damage(6.0); // 3x normal drowning (usually 2.0)
            }
        } else {
            player.setRemainingAir(player.getMaximumAir());
        }

        // Dehydration
        if (!player.isInWater()) {
            long now = System.currentTimeMillis();
            long lastWater = sirenWaterTimer.getOrDefault(player.getUniqueId(), now);
            long threshold = (player.getWorld().getTime() >= 12000) ? 15000 : 10000;
            if (now - lastWater > threshold) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
            }
        } else {
            sirenWaterTimer.put(player.getUniqueId(), System.currentTimeMillis());
        }

        // Nether
        if (player.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
            player.setFireTicks(40);
        }
    }

    private void applyDynamicAttributes(Player player, double scaleVal, double healthVal, double speedVal,
            double damageVal, double attackSpeedVal) {
        org.bukkit.attribute.AttributeInstance scale = player
                .getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
        if (scale != null)
            scale.setBaseValue(scaleVal);

        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH),
                originManager.getModifierKey(), healthVal, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED),
                originManager.getModifierKey(), speedVal - 1.0,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE),
                originManager.getModifierKey(), damageVal - 1.0,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED),
                originManager.getModifierKey(), attackSpeedVal,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
    }

    private void updateTransientModifier(org.bukkit.attribute.AttributeInstance instance, org.bukkit.NamespacedKey key,
            double value, org.bukkit.attribute.AttributeModifier.Operation op) {
        if (instance == null)
            return;
        org.bukkit.attribute.AttributeModifier mod = instance.getModifier(key);
        if (mod != null && mod.getAmount() == value)
            return; // No change

        if (mod != null)
            instance.removeModifier(mod);
        if (value != 0) {
            instance.addTransientModifier(new org.bukkit.attribute.AttributeModifier(key, value, op));
        }
    }

    private double getArmorWeight(Player player) {
        double weight = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item == null)
                continue;
            String type = item.getType().name();
            if (type.contains("NETHERITE"))
                weight += 0.5;
            else if (type.contains("DIAMOND"))
                weight += 0.3;
            else if (type.contains("IRON"))
                weight += 0.2;
            else if (type.contains("CHAINMAIL"))
                weight += 0.1;
        }
        return weight;
    }

    private void startCreeperAvoidanceTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(originManager.getPlugin(), () -> {
            if (!originManager.isEnabled())
                return;
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (originManager.getOrigin(player) == Origin.FELINE) {
                    for (org.bukkit.entity.Entity entity : player.getNearbyEntities(10, 5, 10)) {
                        if (entity instanceof Creeper creeper) {
                            if (!attackedCreepers.containsKey(creeper.getUniqueId())) {
                                // Make creeper flee
                                org.bukkit.util.Vector fleeVec = creeper.getLocation().toVector()
                                        .subtract(player.getLocation().toVector()).normalize().multiply(8);
                                org.bukkit.Location fleeLoc = creeper.getLocation().add(fleeVec);
                                creeper.getPathfinder().moveTo(fleeLoc, 1.5);
                            }
                        }
                    }
                }
            }
        }, 10L, 10L);
    }

    private void startDragonFireTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (originManager.getOrigin(player) != Origin.DRAGONBLOOD)
                    continue;
                if (!player.isSneaking() || now - lastRightClick.getOrDefault(player.getUniqueId(), 0L) > 500)
                    continue;

                dev.aurelium.auraskills.api.user.SkillsUser user = dev.aurelium.auraskills.api.AuraSkillsApi.get()
                        .getUser(player.getUniqueId());
                if (user.consumeMana(4.0)) { // 4 mana per 5 ticks (~16 mana/sec)
                    org.bukkit.Location eye = player.getEyeLocation();
                    org.bukkit.util.Vector dir = eye.getDirection().normalize();

                    // Width Logic: 3 blocks wide
                    org.bukkit.util.Vector left = new org.bukkit.util.Vector(-dir.getZ(), 0, dir.getX()).normalize()
                            .multiply(0.8);
                    org.bukkit.util.Vector right = left.clone().multiply(-1);

                    // Blazing Strike scaling
                    double damage = 4.0;
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand != null && hand.containsEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT)) {
                        damage *= 1.35;
                    }

                    for (double d = 1; d <= 6; d += 0.5) {
                        for (org.bukkit.util.Vector offset : Arrays.asList(new org.bukkit.util.Vector(0, 0, 0), left,
                                right)) {
                            org.bukkit.Location point = eye.clone().add(dir.clone().multiply(d)).add(offset);
                            player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, point, 5, 0.1, 0.1, 0.1, 0.05);

                            for (Entity entity : player.getWorld().getNearbyEntities(point, 1.0, 1.0, 1.0)) {
                                if (entity instanceof LivingEntity living && entity != player) {
                                    living.damage(damage, player);
                                    living.setFireTicks(60);
                                }
                            }
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You ran out of mana!");
                }
            }
        }, 5L, 5L);
    }

    private void startCreeperIgniteTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (originManager.getOrigin(player) != Origin.CREEPER)
                    continue;

                UUID uuid = player.getUniqueId();
                // Check if BOTH Shift (Sneaking) and Right-Click are held
                boolean isHolding = player.isSneaking() && now - lastRightClick.getOrDefault(uuid, 0L) < 500;

                if (isHolding) {
                    long startTime = creeperIgniteStart.getOrDefault(uuid, 0L);
                    if (startTime == 0) {
                        creeperIgniteStart.put(uuid, now);
                        creeperNotificationStage.put(uuid, 1);
                        player.sendMessage("§a§l[!] §7Priming explosion...");
                    } else {
                        long elapsed = now - startTime;
                        int currentStage = creeperNotificationStage.getOrDefault(uuid, 1);

                        // Visual/Audio Feedback based on Stage
                        if (elapsed < 5000) {
                            // Stage 1: Smoke
                            player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE,
                                    player.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.02);
                            if (elapsed % 1000 < 50)
                                player.getWorld().playSound(player.getLocation(),
                                        org.bukkit.Sound.ENTITY_CREEPER_PRIMED, 0.5f, 0.5f);
                        } else if (elapsed < 10000) {
                            // Stage 2: Flame
                            player.getWorld().spawnParticle(org.bukkit.Particle.FLAME,
                                    player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.05);
                            if (currentStage < 2) {
                                creeperNotificationStage.put(uuid, 2);
                                player.sendMessage("§e§l[!] MANA OVERCHARGE ACTIVE");
                                player.getWorld().playSound(player.getLocation(),
                                        org.bukkit.Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.2f);
                            } else if (elapsed % 1000 < 50) {
                                // Sound pulse still nice without message spam
                                player.getWorld().playSound(player.getLocation(),
                                        org.bukkit.Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.2f);
                            }
                        } else {
                            // Stage 3: Soul Fire
                            player.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME,
                                    player.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.08);
                            if (currentStage < 3) {
                                creeperNotificationStage.put(uuid, 3);
                                player.sendMessage("§b§l[!] SUICIDE CHARGE ACTIVE");
                                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_AMBIENT,
                                        1.0f, 2.0f);
                            } else if (elapsed % 1000 < 50) {
                                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_AMBIENT,
                                        1.0f, 2.0f);
                            }

                            // Safety Auto-explode at 15s to prevent infinite soul fire
                            if (elapsed >= 15000) {
                                creeperIgniteStart.remove(uuid);
                                creeperNotificationStage.remove(uuid);
                                triggerCreeperExplosion(player, elapsed);
                            }
                        }
                    }
                } else {
                    // Trigger on Release
                    long startTime = creeperIgniteStart.getOrDefault(uuid, 0L);
                    if (startTime != 0) {
                        long elapsed = now - startTime;
                        creeperIgniteStart.remove(uuid);
                        creeperNotificationStage.remove(uuid);
                        triggerCreeperExplosion(player, elapsed);
                    }
                }
            }
        }, 5L, 5L);
    }

    private void startHeatStealTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (originManager.getOrigin(player) != Origin.SIREN)
                    continue;
                if (!player.isSneaking() || now - lastRightClick.getOrDefault(player.getUniqueId(), 0L) > 500)
                    continue;

                dev.aurelium.auraskills.api.user.SkillsUser user = dev.aurelium.auraskills.api.AuraSkillsApi.get()
                        .getUser(player.getUniqueId());
                if (user.consumeMana(2.0)) {
                    org.bukkit.Location loc = player.getLocation().add(0, 1, 0);
                    org.bukkit.Particle.DustOptions dust = new org.bukkit.Particle.DustOptions(
                            org.bukkit.Color.fromRGB(173, 216, 230), 1.5f);
                    for (int i = 0; i < 20; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double r = 3.0 * Math.random();
                        double x = r * Math.cos(angle);
                        double z = r * Math.sin(angle);
                        player.getWorld().spawnParticle(org.bukkit.Particle.DUST,
                                loc.clone().add(x, Math.random() * 2 - 1, z), 1, dust);
                    }

                    for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
                        if (entity instanceof LivingEntity living && entity != player) {
                            if (living instanceof Player target
                                    && originManager.getOrigin(target) == Origin.DRAGONBLOOD) {
                                continue;
                            }
                            living.setFreezeTicks(Math.min(living.getMaxFreezeTicks(), living.getFreezeTicks() + 40));
                            living.damage(1.0, player);
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You ran out of mana!");
                }
            }
        }, 10L, 10L);
    }

    private void triggerCreeperExplosion(Player player, long duration) {
        dev.aurelium.auraskills.api.user.SkillsUser user = dev.aurelium.auraskills.api.AuraSkillsApi.get()
                .getUser(player.getUniqueId());
        double manaMultiplier = 1.0;
        double hpMultiplier = 1.0;
        double baseDamage = 12.0;

        String message;
        if (duration < 5000) {
            message = "§7§lBOOM!";
        } else if (duration < 10000) {
            double mana = user.getMana();
            manaMultiplier = 1.0 + (mana / 100.0);
            user.consumeMana(mana);
            message = "§e§lMANA BLAST! §6(+" + (int) mana + "%)";
        } else {
            double mana = user.getMana();
            double hp = player.getHealth();
            manaMultiplier = 1.0 + (mana / 100.0);
            hpMultiplier = 1.0 + (hp * 0.05); // 5% per HP
            user.consumeMana(mana);
            player.setHealth(0); // Suicide
            message = "§b§lSUICIDE SUPERNOVA! §3(+" + (int) (mana + hp * 5) + "%)";
        }

        player.getWorld().createExplosion(player.getLocation(), 0.0f, false, false);
        player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, player.getLocation(), 2);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        double totalDamage = baseDamage * manaMultiplier * hpMultiplier;

        // Radius: 8 blocks
        for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof LivingEntity living && entity != player) {
                double dist = player.getLocation().distance(living.getLocation());
                double falloff = 1.0 - (dist / 8.0);
                if (falloff > 0) {
                    living.damage(totalDamage * falloff, player);
                }
            }
        }

        player.sendMessage(message);
    }

    private void startWolfPackTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (originManager.getOrigin(player) != Origin.WOLF)
                    continue;

                for (Entity entity : player.getWorld().getEntitiesByClass(org.bukkit.entity.Wolf.class)) {
                    org.bukkit.entity.Wolf wolf = (org.bukkit.entity.Wolf) entity;
                    if (wolf.isTamed() && player.getUniqueId().equals(wolf.getOwnerUniqueId())) {
                        applyWolfBuffs(wolf);
                    }
                }
            }
        }, 100L, 100L);
    }

    private void applyWolfBuffs(org.bukkit.entity.Wolf wolf) {
        org.bukkit.attribute.AttributeInstance health = wolf
                .getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (health != null && health.getModifier(originManager.getModifierKey()) == null) {
            health.addModifier(new org.bukkit.attribute.AttributeModifier(originManager.getModifierKey(), 0.3,
                    org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR));
            wolf.setHealth(health.getValue());
        }
        org.bukkit.attribute.AttributeInstance damage = wolf
                .getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
        if (damage != null && damage.getModifier(originManager.getModifierKey()) == null) {
            damage.addModifier(new org.bukkit.attribute.AttributeModifier(originManager.getModifierKey(), 0.1,
                    org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    // --- HUMAN PERKS ---

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!originManager.isEnabled())
            return;

        Origin origin = originManager.getOrigin(player);
        if (origin != null && origin != Origin.NONE) {
            originManager.applyAttributes(player, origin);
        } else {
            // No origin set, prompt selection on the next tick to ensure menus are loaded
            org.bukkit.Bukkit.getScheduler().runTaskLater(originManager.getPlugin(), () -> {
                dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu(player, "origins");
            }, 1L);
        }
    }

    @EventHandler
    public void onXpGain(XpGainEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        Origin origin = originManager.getOrigin(player);
        if (origin == null)
            return;

        String skillName = event.getSkill().getId().getKey();

        if (origin == Origin.HUMAN) {
            double wisdom = dev.aurelium.auraskills.api.AuraSkillsApi.get().getUser(player.getUniqueId())
                    .getStatLevel(dev.aurelium.auraskills.api.stat.Stats.WISDOM);
            double bonus = 1.0; // Base bonus
            // Human: Wisdom XP bonus & Nocturnal Fatigue
            bonus += wisdom * 0.001; // 0.1% per wisdom
            if (player.getWorld().getTime() >= 12000) {
                bonus -= 0.15; // -15% XP at night
            }
            event.setAmount(event.getAmount() * bonus);
        } else if (origin == Origin.ORC) {
            if (skillName.equals("fighting") || skillName.equals("defense")) {
                event.setAmount(event.getAmount() * 1.1);
            } else {
                event.setAmount(event.getAmount() * 0.9);
            }
        } else if (origin == Origin.FELINE && skillName.equals("agility")) {
            event.setAmount(event.getAmount() * 1.1);
        } else if (origin == Origin.DWARF && skillName.equals("mining")) {
            event.setAmount(event.getAmount() * 1.1);
        } else if (origin == Origin.WOOD_ELF && skillName.equals("archery")) {
            event.setAmount(event.getAmount() * 1.1);
        } else if (origin == Origin.FAE_TOUCHED && skillName.equals("enchanting")) {
            event.setAmount(event.getAmount() * 1.1);
        }
    }

    // --- FELINE PERKS ---

    @EventHandler
    public void onCreeperTarget(EntityTargetLivingEntityEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getTarget() instanceof Player player && event.getEntity() instanceof LivingEntity mob) {
            if (originManager.getOrigin(player) == Origin.FELINE) {
                if (event.getEntity() instanceof Creeper creeper
                        && !attackedCreepers.containsKey(creeper.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }

                // Stealth: Reduced detection range
                double distance = player.getLocation().distance(mob.getLocation());
                org.bukkit.attribute.AttributeInstance followRange = mob
                        .getAttribute(org.bukkit.attribute.Attribute.GENERIC_FOLLOW_RANGE);
                double range = followRange != null ? followRange.getValue() : 16.0;
                if (distance > range * 0.5) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onCreeperDamage(EntityDamageByEntityEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof Creeper creeper) {
            if (originManager.getOrigin(player) == Origin.FELINE) {
                attackedCreepers.put(creeper.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onCreeperIgnite(CreeperIgniteEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getEntity() instanceof Creeper creeper) {
            if (creeper.getTarget() instanceof Player player) {
                if (originManager.getOrigin(player) == Origin.FELINE) {
                    if (!attackedCreepers.containsKey(creeper.getUniqueId())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFelineFall(EntityDamageEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (originManager.getOrigin(player) == Origin.FELINE) {
                if (player.isSneaking()) {
                    event.setCancelled(true);
                } else {
                    event.setDamage(event.getDamage() * 0.8);
                }
            }
        }
    }

    @EventHandler
    public void onFelineSleep(PlayerBedLeaveEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) == Origin.FELINE) {
            LootTable loot = originManager.getFelineSleepGifts();
            // Chance for a gift like a cat
            if (new Random().nextDouble() < loot.getGiftChance()) {
                ItemStack gift = loot.getRandomItem();
                if (gift != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), gift);
                    player.sendMessage(ChatColor.GOLD + "You found a gift by your bed!");
                }
            }
        }
    }

    @EventHandler
    public void onFelineJump(PlayerInteractEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) == Origin.FELINE && player.isSprinting()) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Simple jump boost logic could be complex without proper jump event,
                // but we can apply a vector if they are on ground and moving up.
            }
        }
    }

    // --- DWARF PERKS ---

    @EventHandler
    public void onDwarfMine(BlockBreakEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) == Origin.DWARF) {
            // Speed handled by giving Haste effect
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 0, false, false));
        }
    }

    @EventHandler
    public void onDwarfMove(PlayerMoveEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        Origin origin = originManager.getOrigin(player);

        if (origin == Origin.DWARF) {
            // Sinking - Guard against Riptide boost
            if (player.isInWater() && !player.isRiptiding()) {
                player.setVelocity(player.getVelocity().add(new Vector(0, -0.05, 0)));
            }
        }
    }

    // --- ELF PERKS ---

    @EventHandler
    public void onWoodElfCharge(PlayerInteractEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() != null && event.getItem().getType() == Material.BOW) {
                if (originManager.getOrigin(event.getPlayer()) == Origin.WOOD_ELF) {
                    bowChargeStart.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
                }
            }
        }
    }

    @EventHandler
    public void onWoodElfShoot(EntityShootBowEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getEntity() instanceof Player player && originManager.getOrigin(player) == Origin.WOOD_ELF) {
            Long start = bowChargeStart.remove(player.getUniqueId());
            if (start != null && (System.currentTimeMillis() - start) > 2000) { // 2 seconds
                if (event.getProjectile() instanceof Arrow arrow) {
                    arrow.setDamage(arrow.getDamage() * 1.2);
                    player.sendMessage(ChatColor.GOLD + "FOCUSED!");
                }
            }
        }
    }

    @EventHandler
    public void onWoodElfForest(PlayerMoveEvent event) {
        // Logic moved to startGlobalPerkTask for performance
    }

    // --- GOBLIN PERKS ---

    @EventHandler
    public void onMerchantOpen(InventoryOpenEvent event) {
        if (!originManager.isEnabled())
            return;
        if (!(event.getPlayer() instanceof Player player))
            return;
        Origin origin = originManager.getOrigin(player);

        if (event.getInventory().getType() == InventoryType.MERCHANT) {
            if (origin == Origin.UNDEAD) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Villagers are terrified of your undead presence!");
                return;
            }

            Merchant merchant = (Merchant) event.getInventory().getHolder();
            if (merchant == null)
                return;
            List<MerchantRecipe> recipes = new ArrayList<>(merchant.getRecipes());

            if (origin == Origin.ORC || origin == Origin.DWARF) {
                for (MerchantRecipe recipe : recipes) {
                    float multiplier = origin == Origin.ORC ? 1.5f : 1.1f;
                    recipe.setPriceMultiplier(recipe.getPriceMultiplier() * multiplier);
                }
            } else if (origin == Origin.GOBLIN && isWearingGold(player)) {
                for (MerchantRecipe recipe : recipes) {
                    recipe.setPriceMultiplier(recipe.getPriceMultiplier() * 0.6f);
                }
            }
            merchant.setRecipes(recipes);
        }
    }

    // --- UNDEAD PERKS ---

    @EventHandler
    public void onUndeadTarget(EntityTargetLivingEntityEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getTarget() instanceof Player player && isUndead(event.getEntity())) {
            if (originManager.getOrigin(player) == Origin.UNDEAD) {
                if (event.getReason() != EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // --- UNDEAD: Rotten Flesh ---
        if (originManager.getOrigin(player) == Origin.UNDEAD && item.getType() == Material.ROTTEN_FLESH) {
            if (player.getFoodLevel() >= 20) {
                org.bukkit.attribute.AttributeInstance maxHealth = player
                        .getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                if (maxHealth != null) {
                    player.setHealth(Math.min(maxHealth.getValue(), player.getHealth() + 2.0));
                    player.sendMessage("§dThe rotten flesh mends your undead body...");
                }
            }
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + 8));
            player.setSaturation(player.getSaturation() + 10);
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 0));
            // Remove hunger effect if it was added
            org.bukkit.Bukkit.getScheduler().runTaskLater(originManager.getPlugin(), () -> {
                player.removePotionEffect(PotionEffectType.HUNGER);
            }, 1L);
            return;
        }

        if (item.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
                if (player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.DIAMOND_BLOCK) {
                    originManager.removeOrigin(player);
                    player.removePotionEffect(PotionEffectType.WEAKNESS);
                    player.sendMessage("§d§lThe ritual is complete. You are reborn!");

                    // Open GUI on the next tick
                    org.bukkit.Bukkit.getScheduler().runTaskLater(originManager.getPlugin(), () -> {
                        dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu(player, "origins");
                    }, 1L);
                }
            }
        }
    }

    @EventHandler
    public void onVampireDrink(PlayerInteractEntityEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) != Origin.VAMPIRE)
            return;
        if (!player.isSneaking() || player.getInventory().getItemInMainHand().getType() != Material.AIR)
            return;

        if (event.getRightClicked() instanceof LivingEntity victim) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            if (now - bloodSuckCooldown.getOrDefault(player.getUniqueId(), 0L) < 1000) {
                player.sendMessage("§cYou must wait to feed again!");
                return;
            }

            dev.aurelium.auraskills.api.user.SkillsUser user = dev.aurelium.auraskills.api.AuraSkillsApi.get()
                    .getUser(player.getUniqueId());
            if (user.getMana() < 5.0) {
                player.sendMessage("§cNot enough mana to feed!");
                return;
            }

            if (isUndead(victim)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                player.sendMessage("§cThis blood is foul and dead!");
            } else {
                user.consumeMana(5.0);
                bloodSuckCooldown.put(player.getUniqueId(), now);
                // Enhanced Blood Suck: True Damage
                double damage = 2.0;
                double newHealth = Math.max(0, victim.getHealth() - damage);
                victim.setHealth(newHealth);
                if (newHealth <= 0) {
                    victim.damage(0.1, player); // Trigger death credit
                }

                org.bukkit.attribute.AttributeInstance maxHealth = player
                        .getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                if (maxHealth != null) {
                    player.setHealth(Math.min(maxHealth.getValue(), player.getHealth() + 1.0));
                    // Add Regeneration I for 10 seconds (200 ticks) after feeding
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0, false, false, true));
                    player.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR,
                            victim.getLocation().add(0, 1, 0), 5);
                    player.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, victim.getLocation().add(0, 1, 0), 15,
                            0.2, 0.2, 0.2, 0.1, Material.REDSTONE_BLOCK.createBlockData());
                    player.sendMessage("§4You feed on the living...");
                }
            }
        }
    }

    @EventHandler
    public void onUndeadPotion(PotionSplashEvent event) {
        if (!originManager.isEnabled())
            return;
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player player && originManager.getOrigin(player) == Origin.UNDEAD) {
                for (PotionEffect effect : event.getPotion().getEffects()) {
                    if (effect.getType().equals(PotionEffectType.INSTANT_HEALTH)) {
                        event.setIntensity(player, 0);
                        player.damage(effect.getAmplifier() * 6.0);
                    } else if (effect.getType().equals(PotionEffectType.INSTANT_DAMAGE)) {
                        event.setIntensity(player, 0);
                        org.bukkit.attribute.AttributeInstance maxHealth = player
                                .getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                        if (maxHealth != null) {
                            player.setHealth(
                                    Math.min(maxHealth.getValue(), player.getHealth() + (effect.getAmplifier() * 6.0)));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onUndeadCloud(AreaEffectCloudApplyEvent event) {
        if (!originManager.isEnabled())
            return;
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player player && originManager.getOrigin(player) == Origin.UNDEAD) {
                PotionEffectType type = event.getEntity().getBasePotionType().getPotionEffects().get(0).getType();
                if (type.equals(PotionEffectType.INSTANT_HEALTH)) {
                    player.damage(4.0);
                } else if (type.equals(PotionEffectType.INSTANT_DAMAGE)) {
                    org.bukkit.attribute.AttributeInstance maxHealth = player
                            .getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealth != null) {
                        player.setHealth(Math.min(maxHealth.getValue(), player.getHealth() + 4.0));
                    }
                }
            }
        }
    }

    private boolean isUndead(Entity entity) {
        return entity instanceof Zombie || entity instanceof Skeleton || entity instanceof Phantom ||
                entity instanceof WitherSkeleton || entity instanceof Drowned || entity instanceof Husk ||
                entity instanceof Stray || entity instanceof Phantom;
    }

    // --- ORC PERKS ---

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!originManager.isEnabled())
            return;

        // Damage Dealt
        if (event.getDamager() instanceof Player player) {
            Origin origin = originManager.getOrigin(player);
            long time = player.getWorld().getTime();
            boolean isNight = time >= 12000;
            boolean isFullMoon = (player.getWorld().getFullTime() / 24000) % 8 == 0;

            if (origin == Origin.ORC) {
                event.setDamage(event.getDamage() * 1.15);
            } else if (origin == Origin.DRAGONBLOOD) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand != null && hand.containsEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT)) {
                    event.setDamage(event.getDamage() * 1.35);
                }
                if (player.isInWater()
                        || (player.getWorld().hasStorm() && player.getLocation().getBlock().getLightFromSky() > 10)) {
                    event.setDamage(event.getDamage() * 0.9);
                }
            } else if (origin == Origin.VAMPIRE) {
                if (isNight)
                    event.setDamage(event.getDamage() * 1.2);
                else
                    event.setDamage(event.getDamage() * 0.5);
            } else if (origin == Origin.WEREWOLF
                    && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                if (isNight) {
                    double multiplier = isFullMoon ? 2.0 : 1.7;
                    event.setDamage(event.getDamage() * multiplier);
                }
            } else if (origin == Origin.WOLF) {
                int count = 0;
                for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                    if (entity instanceof org.bukkit.entity.Wolf wolf && wolf.isTamed()
                            && player.getUniqueId().equals(wolf.getOwnerUniqueId())) {
                        count++;
                    }
                }
                event.setDamage(event.getDamage() * (1.0 + (0.02 * count)));
            }
        }

        // Damage Taken (Specific Weaknesses)
        if (event.getEntity() instanceof Player player) {
            Origin origin = originManager.getOrigin(player);
            if (origin == Origin.WEREWOLF) {
                if (event.getDamager() instanceof LivingEntity damager) {
                    ItemStack weapon = damager.getEquipment() != null ? damager.getEquipment().getItemInMainHand()
                            : null;
                    if (weapon != null && (weapon.getType().name().contains("IRON_SWORD")
                            || weapon.getType().name().contains("IRON_AXE"))) {
                        event.setDamage(event.getDamage() * 2.0);
                        player.sendMessage("§cThe cold iron pierces your hide!");
                    }
                }
            } else if (origin == Origin.SIREN) {
                if (event.getDamager() instanceof LivingEntity damager) {
                    ItemStack weapon = damager.getEquipment() != null ? damager.getEquipment().getItemInMainHand()
                            : null;
                    if (weapon != null && weapon.getType() == Material.TRIDENT) {
                        event.setDamage(event.getDamage() * 2.0);
                        player.sendMessage("§cThe trident pierces your aquatic flesh!");
                    }
                }
            } else if (origin == Origin.UNDEAD) {
                if (event.getDamager() instanceof LivingEntity damager) {
                    ItemStack weapon = damager.getEquipment() != null ? damager.getEquipment().getItemInMainHand()
                            : null;
                    if (weapon != null && weapon.hasItemMeta()
                            && weapon.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.SMITE)) {
                        event.setDamage(event.getDamage() * 1.7);
                        player.sendMessage("§cThe holy light of Smite burns your unholy skin!");
                    }
                }
            }
        }
    }

    // --- DRAGONBLOOD PERKS ---

    @EventHandler
    public void onGeneralDamageReduction(EntityDamageEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getEntity() instanceof Player player) {
            Origin origin = originManager.getOrigin(player);
            if (origin == Origin.ORC) {
                event.setDamage(event.getDamage() * 0.8);
            }
            if (origin == Origin.DRAGONBLOOD) {
                if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                        event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                        event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    event.setCancelled(true);
                }
            }
            if (origin == Origin.CREEPER) {
                if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                        event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                    event.setCancelled(true);
                }
            }
            if (origin == Origin.SIREN) {
                if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                        event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                        event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    event.setDamage(event.getDamage() * 3.0);
                    player.sendMessage("§cThe heat is unbearable!");
                }
            }
        }
    }

    // --- FAE-TOUCHED PERKS ---

    @EventHandler
    public void onSirenJump(PlayerToggleFlightEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        Origin origin = originManager.getOrigin(player);
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE)
            return;

        if (origin == Origin.FAE_TOUCHED) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            Vector v = player.getLocation().getDirection().multiply(0.5).setY(0.5);
            player.setVelocity(v);
        } else if (origin == Origin.SIREN && player.isInWater()) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            Vector v = player.getLocation().getDirection().multiply(1.2).setY(0.8);
            player.setVelocity(v);
            player.sendMessage("§bSwoosh!");
        }
    }

    @EventHandler
    public void onFaeMove(PlayerMoveEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) == Origin.FAE_TOUCHED
                && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            if (((org.bukkit.entity.Entity) player).isOnGround()) {
                player.setAllowFlight(true);
            }
        }
    }

    private boolean isWearingGold(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType().name().contains("GOLDEN"))
                return true;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType().name().contains("GOLDEN"))
            return true;
        return false;
    }

    // --- SIREN HANDLERS ---

    @EventHandler
    public void onWolfTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (!originManager.isEnabled())
            return;
        if (!(event.getTarget() instanceof Player player))
            return;

        Origin origin = originManager.getOrigin(player);
        if (origin == Origin.WOLF) {
            Entity entity = event.getEntity();
            // Wild Wolves passive
            if (entity instanceof org.bukkit.entity.Wolf wolf && !wolf.isTamed()) {
                event.setCancelled(true);
            }
            // Skeletons scared
            if (entity instanceof org.bukkit.entity.Skeleton || entity instanceof org.bukkit.entity.Bogged
                    || entity instanceof org.bukkit.entity.Stray) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onOriginInteract(PlayerInteractEvent event) {
        if (!originManager.isEnabled())
            return;
        Player player = event.getPlayer();
        Origin origin = originManager.getOrigin(player);
        if (origin == null)
            return;

        // --- CHANNELING PERKS (SIREN, DRAGONBLOOD, CREEPER) ---
        if ((origin == Origin.SIREN || origin == Origin.DRAGONBLOOD || origin == Origin.CREEPER)
                && player.isSneaking()) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                lastRightClick.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }

        // --- WOLF: Alpha Howl ---
        if (origin == Origin.WOLF && player.isSneaking()) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                dev.aurelium.auraskills.api.user.SkillsUser user = dev.aurelium.auraskills.api.AuraSkillsApi.get()
                        .getUser(player.getUniqueId());
                int healed = 0;
                for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                    if (entity instanceof org.bukkit.entity.Wolf wolf && wolf.isTamed()
                            && player.getUniqueId().equals(wolf.getOwnerUniqueId())) {
                        if (user.getMana() >= 10.0) {
                            user.consumeMana(10.0);
                            wolf.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                            healed++;
                        } else {
                            break;
                        }
                    }
                }
                if (healed > 0) {
                    player.sendMessage("§bThe pack is revitalized! (" + healed + " wolves healed)");
                    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler
    public void onSirenShield(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getWhoClicked() instanceof Player player) {
            if (originManager.getOrigin(player) == Origin.SIREN) {
                ItemStack item = event.getCursor();
                if (item != null && item.getType() == Material.SHIELD) {
                    if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR
                            || event.getRawSlot() == 45) { // Offhand slot is usually 45
                        event.setCancelled(true);
                        player.sendMessage("§cYour grip is too weak for a shield.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSirenAirRegen(org.bukkit.event.entity.EntityAirChangeEvent event) {
        if (!originManager.isEnabled())
            return;
        if (event.getEntity() instanceof Player player) {
            if (originManager.getOrigin(player) == Origin.SIREN && !player.isInWater()) {
                // If on land, prevent air from increasing (regeneration)
                if (event.getAmount() > player.getRemainingAir()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
