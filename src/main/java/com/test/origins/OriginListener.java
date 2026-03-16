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
    private final Set<UUID> attackedCreepers = new HashSet<>();
    private final Map<UUID, Long> bowChargeStart = new HashMap<>();

    public OriginListener(OriginManager originManager) {
        this.originManager = originManager;
        startCreeperAvoidanceTask();
        startGlobalPerkTask();
    }

    private void startGlobalPerkTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(originManager.getPlugin(), () -> {
            if (!originManager.isEnabled()) return;
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                Origin origin = originManager.getOrigin(player);
                if (origin == null) continue;

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
                    org.bukkit.attribute.AttributeInstance health = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                    if (health != null && player.getHealth() / health.getValue() <= 0.4) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false, true));
                    }
                }

                // --- DRAGONBLOOD: Water Debuffs ---
                if (origin == Origin.DRAGONBLOOD) {
                    if (player.isInWater() || player.getWorld().hasStorm()) {
                         // Check if actually wet (outside if raining)
                         if (player.isInWater() || (player.getWorld().hasStorm() && player.getLocation().getBlock().getLightFromSky() > 10)) {
                             player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false, false));
                             // Damage reduction handled in EntityDamageByEntityEvent
                         }
                    }
                }

                // --- FAE-TOUCHED: Night Speed & Regen ---
                if (origin == Origin.FAE_TOUCHED) {
                    if (player.getWorld().getTime() >= 12000) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, false));
                    }
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false, false));
                    
                    // Armor Weight
                    double weight = getArmorWeight(player);
                    if (weight > 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, (int) (weight * 2), false, false, false));
                    }
                }

                // --- DWARF: Sunlight & Night Vision ---
                if (origin == Origin.DWARF) {
                    // Sunlight Sensitivity
                    if (player.getWorld().getTime() < 12000 && player.getLocation().getBlock().getLightFromSky() > 10) {
                        if (player.getInventory().getHelmet() == null) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false));
                        }
                    }
                    
                    // Night Vision
                    if (player.getWorld().getTime() >= 12000 || player.getLocation().getBlock().getLightFromSky() <= 10) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false, false));
                    } else {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    }
                
                // --- HUMAN: Nocturnal Fatigue ---
                if (origin == Origin.HUMAN) {
                    if (player.getWorld().getTime() >= 12000) {
                        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE), -0.1, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
                        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED), -0.1, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
                    } else {
                        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE), 0.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
                        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED), 0.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
                    }
                }
                }

                // --- WOOD_ELF: Forest Vision ---
                if (origin == Origin.WOOD_ELF) {
                    Biome biome = player.getLocation().getBlock().getBiome();
                    if (biome.name().contains("FOREST")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
                    }
                }

                // --- VAMPIRE: Sunlight & Beetroot ---
                if (origin == Origin.VAMPIRE) {
                    if (player.getWorld().getTime() < 12000 && player.getLocation().getBlock().getLightFromSky() > 10) {
                        if (player.getInventory().getHelmet() == null) {
                            player.damage(4.0); // High sunlight damage
                            player.setFireTicks(40);
                        }
                    }
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, false, false, false));
                    
                    // Beetroot Weakness
                    for (Player nearby : player.getWorld().getPlayers()) {
                        if (nearby.getLocation().distanceSquared(player.getLocation()) <= 25) { // 5 blocks
                            if (hasItem(nearby, Material.BEETROOT)) {
                                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, false, false));
                                break;
                            }
                        }
                    }
                }

                // --- WEREWOLF: Mutations & Iron Armor ---
                if (origin == Origin.WEREWOLF) {
                    updateWerewolfState(player);
                    
                    // Iron Armor Damage
                    if (isWearingIron(player)) {
                        player.damage(1.0);
                        player.sendMessage("§cThe iron burns your skin!");
                    }
                }
            }
        }, 20L, 20L);
    }

    private boolean hasItem(Player player, Material material) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) return true;
        }
        return false;
    }

    private boolean isWearingIron(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType().name().contains("IRON")) return true;
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

    private void applyDynamicAttributes(Player player, double scaleVal, double healthVal, double speedVal, double damageVal, double attackSpeedVal) {
        org.bukkit.attribute.AttributeInstance scale = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
        if (scale != null) scale.setBaseValue(scaleVal);
        
        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH), healthVal, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED), speedVal - 1.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE), damageVal - 1.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
        updateTransientModifier(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED), attackSpeedVal, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR);
    }

    private void updateTransientModifier(org.bukkit.attribute.AttributeInstance instance, double value, org.bukkit.attribute.AttributeModifier.Operation op) {
        if (instance == null) return;
        org.bukkit.attribute.AttributeModifier mod = instance.getModifier(originManager.getModifierKey());
        if (mod != null && mod.getAmount() == value) return; // No change
        
        if (mod != null) instance.removeModifier(mod);
        if (value != 0) {
            instance.addTransientModifier(new org.bukkit.attribute.AttributeModifier(originManager.getModifierKey(), value, op));
        }
    }

    private double getArmorWeight(Player player) {
        double weight = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item == null) continue;
            String type = item.getType().name();
            if (type.contains("NETHERITE")) weight += 0.5;
            else if (type.contains("DIAMOND")) weight += 0.3;
            else if (type.contains("IRON")) weight += 0.2;
            else if (type.contains("CHAINMAIL")) weight += 0.1;
        }
        return weight;
    }

    private void startCreeperAvoidanceTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(originManager.getPlugin(), () -> {
            if (!originManager.isEnabled()) return;
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (originManager.getOrigin(player) == Origin.FELINE) {
                    for (org.bukkit.entity.Entity entity : player.getNearbyEntities(10, 5, 10)) {
                        if (entity instanceof Creeper creeper) {
                            if (!attackedCreepers.contains(creeper.getUniqueId())) {
                                // Make creeper flee
                                org.bukkit.util.Vector fleeVec = creeper.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(8);
                                org.bukkit.Location fleeLoc = creeper.getLocation().add(fleeVec);
                                creeper.getPathfinder().moveTo(fleeLoc, 1.5);
                            }
                        }
                    }
                }
            }
        }, 10L, 10L);
    }

    // --- HUMAN PERKS ---

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!originManager.isEnabled()) return;
        
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
        if (!originManager.isEnabled()) return;
        Player player = event.getPlayer();
        Origin origin = originManager.getOrigin(player);
        if (origin == null) return;

        String skillName = event.getSkill().getId().getKey();

        if (origin == Origin.HUMAN) {
            double wisdom = dev.aurelium.auraskills.api.AuraSkillsApi.get().getUser(player.getUniqueId()).getStatLevel(dev.aurelium.auraskills.api.stat.Stats.WISDOM);
            double bonus = 1.0 + (wisdom * 0.001);
            if (player.getWorld().getTime() >= 12000) {
                bonus *= 0.7; // -30% XP at night
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
        if (!originManager.isEnabled()) return;
        if (event.getTarget() instanceof Player player && event.getEntity() instanceof LivingEntity mob) {
            if (originManager.getOrigin(player) == Origin.FELINE) {
                if (event.getEntity() instanceof Creeper creeper && !attackedCreepers.contains(creeper.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
                
                // Stealth: Reduced detection range
                double distance = player.getLocation().distance(mob.getLocation());
                org.bukkit.attribute.AttributeInstance followRange = mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_FOLLOW_RANGE);
                double range = followRange != null ? followRange.getValue() : 16.0;
                if (distance > range * 0.5) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onCreeperDamage(EntityDamageByEntityEvent event) {
        if (!originManager.isEnabled()) return;
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof Creeper creeper) {
            if (originManager.getOrigin(player) == Origin.FELINE) {
                attackedCreepers.add(creeper.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onCreeperIgnite(CreeperIgniteEvent event) {
        if (!originManager.isEnabled()) return;
        if (event.getEntity() instanceof Creeper creeper) {
            if (creeper.getTarget() instanceof Player player) {
                if (originManager.getOrigin(player) == Origin.FELINE) {
                    if (!attackedCreepers.contains(creeper.getUniqueId())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFelineFall(EntityDamageEvent event) {
        if (!originManager.isEnabled()) return;
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
        if (!originManager.isEnabled()) return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) == Origin.FELINE) {
            // Chance for a gift like a cat
            if (new Random().nextDouble() < 0.7) { // 70% chance
                 ItemStack gift = getRandomFelineGift();
                 player.getWorld().dropItemNaturally(player.getLocation(), gift);
                 player.sendMessage(ChatColor.GOLD + "You found a gift by your bed!");
            }
        }
    }

    private ItemStack getRandomFelineGift() {
        Random rand = new Random();
        double r = rand.nextDouble();
        if (r < 0.05) return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE); // Legendary
        if (r < 0.15) return new ItemStack(Material.DIAMOND_BLOCK); // Epic
        if (r < 0.4) return new ItemStack(Material.IRON_INGOT, 4); // Rare
        return new ItemStack(Material.ROTTEN_FLESH, 2); // Normal
    }

    @EventHandler
    public void onFelineJump(PlayerInteractEvent event) {
        if (!originManager.isEnabled()) return;
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
        if (!originManager.isEnabled()) return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) == Origin.DWARF) {
            // Speed handled by giving Haste effect
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 0, false, false));
        }
    }

    @EventHandler
    public void onDwarfMove(PlayerMoveEvent event) {
        if (!originManager.isEnabled()) return;
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
        if (!originManager.isEnabled()) return;
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
        if (!originManager.isEnabled()) return;
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
        if (!originManager.isEnabled()) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        Origin origin = originManager.getOrigin(player);
        
        if (event.getInventory().getType() == InventoryType.MERCHANT) {
            if (origin == Origin.UNDEAD) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Villagers are terrified of your undead presence!");
                return;
            }
            
            Merchant merchant = (Merchant) event.getInventory().getHolder();
            if (merchant == null) return;
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
        if (!originManager.isEnabled()) return;
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
        if (!originManager.isEnabled()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // --- UNDEAD: Rotten Flesh ---
        if (originManager.getOrigin(player) == Origin.UNDEAD && item.getType() == Material.ROTTEN_FLESH) {
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
        if (!originManager.isEnabled()) return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) != Origin.VAMPIRE) return;
        if (!player.isSneaking() || player.getInventory().getItemInMainHand().getType() != Material.AIR) return;
        
        if (event.getRightClicked() instanceof LivingEntity victim) {
            event.setCancelled(true);
            if (isUndead(victim)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                player.sendMessage("§cThis blood is foul and dead!");
            } else {
                victim.damage(4.0, player);
                player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 2.0));
                player.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 5);
                player.sendMessage("§4You feed on the living...");
            }
        }
    }

    @EventHandler
    public void onUndeadPotion(PotionSplashEvent event) {
        if (!originManager.isEnabled()) return;
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player player && originManager.getOrigin(player) == Origin.UNDEAD) {
                for (PotionEffect effect : event.getPotion().getEffects()) {
                    if (effect.getType().equals(PotionEffectType.INSTANT_HEALTH)) {
                        event.setIntensity(player, 0);
                        player.damage(effect.getAmplifier() * 6.0);
                    } else if (effect.getType().equals(PotionEffectType.INSTANT_DAMAGE)) {
                        event.setIntensity(player, 0);
                        org.bukkit.attribute.AttributeInstance maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                        if (maxHealth != null) {
                            player.setHealth(Math.min(maxHealth.getValue(), player.getHealth() + (effect.getAmplifier() * 6.0)));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onUndeadCloud(AreaEffectCloudApplyEvent event) {
        if (!originManager.isEnabled()) return;
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player player && originManager.getOrigin(player) == Origin.UNDEAD) {
                PotionEffectType type = event.getEntity().getBasePotionType().getPotionEffects().get(0).getType();
                if (type.equals(PotionEffectType.INSTANT_HEALTH)) {
                    player.damage(4.0);
                } else if (type.equals(PotionEffectType.INSTANT_DAMAGE)) {
                    org.bukkit.attribute.AttributeInstance maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
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
        if (!originManager.isEnabled()) return;
        
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
                if (hand.containsEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT)) {
                    event.setDamage(event.getDamage() * 1.35);
                }
                if (player.isInWater() || (player.getWorld().hasStorm() && player.getLocation().getBlock().getLightFromSky() > 10)) {
                    event.setDamage(event.getDamage() * 0.9);
                }
            } else if (origin == Origin.VAMPIRE) {
                if (isNight) event.setDamage(event.getDamage() * 1.2);
                else event.setDamage(event.getDamage() * 0.5);
            } else if (origin == Origin.WEREWOLF && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                if (isNight) {
                    double multiplier = isFullMoon ? 2.0 : 1.7;
                    event.setDamage(event.getDamage() * multiplier);
                }
            }
        }
        
        // Damage Taken
        if (event.getEntity() instanceof Player player) {
            Origin origin = originManager.getOrigin(player);
            if (origin == Origin.WEREWOLF) {
                if (event.getDamager() instanceof LivingEntity damager) {
                    ItemStack weapon = damager.getEquipment() != null ? damager.getEquipment().getItemInMainHand() : null;
                    if (weapon != null && (weapon.getType().name().contains("IRON_SWORD") || weapon.getType().name().contains("IRON_AXE"))) {
                        event.setDamage(event.getDamage() * 2.0);
                        player.sendMessage("§cThe cold iron pierces your hide!");
                    }
                }
            }
        }
    }

    // --- DRAGONBLOOD PERKS ---

    @EventHandler
    public void onGeneralDamageReduction(EntityDamageEvent event) {
        if (!originManager.isEnabled()) return;
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
        }
    }

    // --- FAE-TOUCHED PERKS ---

    @EventHandler
    public void onFaeJump(PlayerToggleFlightEvent event) {
        if (!originManager.isEnabled()) return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) == Origin.FAE_TOUCHED && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            Vector v = player.getLocation().getDirection().multiply(0.5).setY(0.5);
            player.setVelocity(v);
        }
    }

    @EventHandler
    public void onFaeMove(PlayerMoveEvent event) {
        if (!originManager.isEnabled()) return;
        Player player = event.getPlayer();
        if (originManager.getOrigin(player) == Origin.FAE_TOUCHED && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            if (((org.bukkit.entity.Entity) player).isOnGround()) {
                player.setAllowFlight(true);
            }
        }
    }

    private boolean isWearingGold(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType().name().contains("GOLDEN")) return true;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType().name().contains("GOLDEN")) return true;
        return false;
    }
}
