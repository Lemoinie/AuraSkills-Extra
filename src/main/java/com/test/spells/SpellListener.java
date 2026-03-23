package com.test.spells;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.user.SkillsUser;
import dev.aurelium.auraskills.api.stat.Stats;
import dev.aurelium.auraskills.api.skill.Skills;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Entity;
import java.util.Collection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class SpellListener implements Listener {

    private final SpellManager spellManager;
    private final Random random = new Random();

    public SpellListener(SpellManager spellManager) {
        this.spellManager = spellManager;
        startRegenTask();
    }

    private void startRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                double multiplier = spellManager.getManaRegenMultiplier();
                if (multiplier <= 1.0) return;

                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    SkillsUser user = AuraSkillsApi.get().getUser(player.getUniqueId());
                    if (user != null && user.getMana() < user.getMaxMana()) {
                        // AuraSkills base regen is roughly 1% of max mana per 2 seconds (0.5% per sec)
                        // plus wisdom bonus. We'll add roughly 'multiplier - 1' times that base.
                        double bonus = (user.getMaxMana() * 0.005) * (multiplier - 1.0);
                        user.setMana(Math.min(user.getMaxMana(), user.getMana() + bonus));
                    }
                }
            }
        }.runTaskTimer(spellManager.getPlugin(), 20L, 20L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!spellManager.isEnabled()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.BLAZE_ROD) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().contains("Spell Wand")) return;

        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            spellManager.cycleSpell(player);
            Spell active = spellManager.getActiveSpell(player);
            if (active != null) {
                player.sendActionBar(ChatColor.YELLOW + "Selected Spell: " + active.getTier().getColor() + active.getDisplayName());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            } else {
                player.sendActionBar(ChatColor.RED + "No spells equipped!");
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            castSpell(player);
        } else {
            // player.sendMessage("Debug: Action: " + event.getAction());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        double gain = spellManager.getManaGain(entity.getType());
        if (gain == 0) return;

        SkillsUser user = AuraSkillsApi.get().getUser(killer.getUniqueId());
        if (user != null) {
            double current = user.getMana();
            double next = Math.min(user.getMaxMana(), current + gain);
            user.setMana(next);
            
            if (gain > 0) {
                // Minor visual feedback
                killer.spawnParticle(Particle.BUBBLE, entity.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    private void castSpell(Player player) {
        if (!spellManager.isEnabled()) {
            player.sendMessage(ChatColor.RED + "The spell system is currently disabled!");
            return;
        }

        Spell spell = spellManager.getActiveSpell(player);
        spellManager.getPlugin().getLogger().info("[SKE] " + player.getName() + " is attempting to cast " + (spell != null ? spell.name() : "NONE"));
        if (spell == null) {
            player.sendMessage(ChatColor.RED + "You don't have any spells equipped!");
            return;
        }


        if (spellManager.isOnCooldown(player, spell)) {
            player.sendActionBar(ChatColor.RED + "Cooldown: " + spellManager.getRemainingCooldown(player, spell) + "s");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        SkillsUser user = AuraSkillsApi.get().getUser(player.getUniqueId());
        if (user == null) {
            player.sendMessage(ChatColor.RED + "Skills data not loaded yet!");
            return;
        }

        double manaCost = spell.getManaCost();

        if (spellManager.hasSurgeDiscount(player)) {
            manaCost *= 0.9;
        }

        if (user.getMana() < manaCost) {
            player.sendMessage(ChatColor.RED + "Not enough Mana! (" + (int)manaCost + " required)");
            return;
        }

        boolean success = false;
        try {
            switch (spell) {
                case KINETIC_SHOVE: success = castKineticShove(player, user); break;
                case SPARK: success = castSpark(player, user); break;
                case MAGE_LIGHT: success = castMageLight(player, user); break;
                case NATURES_ROOT: success = castNaturesRoot(player, user); break;
                case FIREBOLT: success = castFirebolt(player, user); break;
                case FROST_TOUCH: success = castFrostTouch(player, user); break;
                case ARCANE_SURGE: success = castArcaneSurge(player, user); break;
                case METEOR: success = castMeteor(player, user); break;
                case THUNDERSTORM: success = castThunderstorm(player, user); break;
                case STORM_CALLING: success = castStormCalling(player, user); break;
                case METEOR_SHOWER: success = castMeteorShower(player, user); break;
                case CHAIN_LIGHTNING: success = castChainLightning(player, user); break;
                case MALEVOLENT_SHRINE: success = castMalevolentShrine(player, user); break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (success) {
            user.setMana(user.getMana() - manaCost);
            if (spellManager.hasSurgeDiscount(player)) {
                spellManager.consumeSurgeDiscount(player);
            }
            applyCooldown(player, spell);
            player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.2f);
        }
    }

    private void applyCooldown(Player player, Spell spell) {
        spellManager.setCooldown(player, spell, spell.getCooldown());
    }

    private void spawnFlashyParticles(Location loc, SpellTier tier, Particle particle) {
        try {
            int count = 10;
            double speed = 0.1;
            double offset = 0.5;

            if (tier == SpellTier.UNCOMMON) { count = 30; offset = 1.0; }
            else if (tier == SpellTier.RARE) { count = 60; speed = 0.2; offset = 1.5; }
            else if (tier == SpellTier.LEGENDARY) { count = 150; speed = 0.3; offset = 2.0; }
            else if (tier == SpellTier.MYTHIC) { count = 300; speed = 0.5; offset = 3.0; }

            loc.getWorld().spawnParticle(particle, loc, count, offset, offset, offset, speed);
            if (tier == SpellTier.LEGENDARY || tier == SpellTier.MYTHIC) {
                // Use extremely safe particles
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 5, 0.1, 0.1, 0.1, 0.01);
                loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 2, 0.1, 0.1, 0.1, 0.01);
            }
        } catch (Exception e) {
            spellManager.getPlugin().getLogger().warning("Failed to spawn particles: " + e.getMessage());
        }
    }

    private boolean castKineticShove(Player player, SkillsUser user) {
        double strength = user.getStatLevel(Stats.STRENGTH);
        double knockback = 1.0 + (strength / 50.0);
        
        Location loc = player.getLocation();
        spawnFlashyParticles(loc, SpellTier.COMMON, Particle.CLOUD);
        loc.getWorld().playSound(loc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.0f);

        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (entity instanceof LivingEntity && entity != player) {
                Vector dir = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                dir.setY(0.3);
                entity.setVelocity(dir.multiply(knockback));
            }
        }
        return true;
    }

    private boolean castSpark(Player player, SkillsUser user) {
        SmallFireball fireball = player.launchProjectile(SmallFireball.class);
        fireball.setYield(0);
        fireball.setIsIncendiary(false); // Non-destructive
        fireball.setVelocity(player.getLocation().getDirection().multiply(1.5));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);
        spawnFlashyParticles(player.getLocation(), SpellTier.COMMON, Particle.FLAME);
        return true;
    }

    private boolean castMageLight(Player player, SkillsUser user) {
        Location loc = player.getLocation().add(0, 2, 0);
        if (loc.getBlock().getType() != Material.AIR) loc = player.getLocation();
        
        final Location targetLoc = loc;
        targetLoc.getBlock().setType(Material.LIGHT);
        
        player.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2.0f);
        spawnFlashyParticles(targetLoc, SpellTier.COMMON, Particle.END_ROD);
        
        org.bukkit.Bukkit.getScheduler().runTaskLater(spellManager.getPlugin(), () -> {
            if (targetLoc.getBlock().getType() == Material.LIGHT) {
                targetLoc.getBlock().setType(Material.AIR);
                targetLoc.getWorld().spawnParticle(Particle.SMOKE, targetLoc.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.02);
            }
        }, 1200L);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 1200 || targetLoc.getBlock().getType() != Material.LIGHT) {
                    this.cancel();
                    return;
                }
                targetLoc.getWorld().spawnParticle(Particle.END_ROD, targetLoc.clone().add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0.01);
                ticks += 5;
            }
        }.runTaskTimer(spellManager.getPlugin(), 0L, 5L);

        return true;
    }

    private boolean castNaturesRoot(Player player, SkillsUser user) {
        Entity target = getTarget(player, 10);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage(ChatColor.RED + "No target found in range!");
            return false;
        }

        double foraging = user.getSkillLevel(Skills.FORAGING);
        double wisdom = user.getStatLevel(Stats.WISDOM);
        double extraTicks = ((foraging + wisdom) / 10.0) * 10;
        double durationTicks = 60 + extraTicks;

        LivingEntity living = (LivingEntity) target;
        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)durationTicks, 9));
        spawnFlashyParticles(living.getLocation(), SpellTier.UNCOMMON, Particle.HAPPY_VILLAGER);
        living.getWorld().playSound(living.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, 1.0f, 0.8f);
        
        player.sendMessage(ChatColor.GREEN + "Rooted " + living.getName() + "!");
        return true;
    }

    private boolean castFirebolt(Player player, SkillsUser user) {
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(0); // Non-destructive
        fireball.setIsIncendiary(false); // Non-destructive
        fireball.setVelocity(player.getLocation().getDirection().multiply(2.0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.2f);
        spawnFlashyParticles(player.getLocation(), SpellTier.UNCOMMON, Particle.LARGE_SMOKE);
        return true;
    }

    private boolean castFrostTouch(Player player, SkillsUser user) {
        Entity target = getTarget(player, 5);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage(ChatColor.RED + "No target found in range!");
            return false;
        }
        LivingEntity living = (LivingEntity) target;
        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
        spawnFlashyParticles(living.getLocation(), SpellTier.UNCOMMON, Particle.SNOWFLAKE);
        living.getWorld().playSound(living.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
        return true;
    }

    private boolean castArcaneSurge(Player player, SkillsUser user) {
        Vector dir = player.getLocation().getDirection().normalize();
        player.setVelocity(dir.multiply(1.5));
        
        Location loc = player.getLocation();
        spawnFlashyParticles(loc, SpellTier.RARE, Particle.WITCH);
        
        spellManager.setSurgeDiscount(player);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Arcane Surge! Next spell costs 10% less.");
        return true;
    }

    private boolean castMeteor(Player player, SkillsUser user) {
        Location targetLoc = null;
        org.bukkit.util.RayTraceResult ray = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 50, org.bukkit.FluidCollisionMode.NEVER);
        if (ray != null && ray.getHitBlock() != null) {
            targetLoc = ray.getHitBlock().getLocation();
        } else {
            targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(25));
        }

        double combat = user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.FIGHTING);
        double multiplier = 1.0 + (combat * 0.02);
        
        spawnPointToPointMeteor(player, targetLoc, 15.0 * multiplier, 1.5);
        player.sendMessage(ChatColor.GOLD + "Meteor incanting...");
        return true;
    }

    private void spawnPointToPointMeteor(LivingEntity caster, Location landingLoc, double damage, double knockback) {
        Location startLoc = landingLoc.clone().add(random.nextInt(10) - 5, 30, random.nextInt(10) - 5);
        Vector direction = landingLoc.toVector().subtract(startLoc.toVector()).normalize();
        double speed = 1.5;

        new BukkitRunnable() {
            Location current = startLoc.clone();
            int steps = 0;

            @Override
            public void run() {
                // If it hits a block or goes on too long or reaches target height
                if (steps > 60 || current.getY() <= landingLoc.getY() || (steps > 5 && current.getBlock().getType().isSolid())) {
                    handleMeteorImpact(current, damage, knockback, caster);
                    this.cancel();
                    return;
                }

                // Meteor Visuals
                current.getWorld().spawnParticle(Particle.LARGE_SMOKE, current, 12, 0.2, 0.2, 0.2, 0.05);
                current.getWorld().spawnParticle(Particle.FLAME, current, 20, 0.3, 0.3, 0.3, 0.1);
                current.getWorld().spawnParticle(Particle.LAVA, current, 3, 0.1, 0.1, 0.1, 0.05);
                
                if (steps % 3 == 0) {
                    current.getWorld().playSound(current, Sound.ENTITY_GHAST_SHOOT, 0.6f, 0.5f);
                }

                current.add(direction.clone().multiply(speed));
                steps++;
            }
        }.runTaskTimer(spellManager.getPlugin(), 0, 1);
    }

    private void handleMeteorImpact(Location loc, double damage, double kb, LivingEntity caster) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 1, 1, 1, 0.1);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 100, 2, 2, 2, 0.2);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 6, 6, 6)) {
            if (e instanceof LivingEntity victim && !e.equals(caster)) {
                victim.damage(damage, caster);
                victim.setFireTicks(80);
                
                Vector dir = victim.getLocation().toVector().subtract(loc.toVector()).normalize();
                dir.setY(0.5);
                victim.setVelocity(dir.multiply(kb));
            }
        }
    }


    private boolean castThunderstorm(Player player, SkillsUser user) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
        
        boolean isThundering = player.getWorld().isThundering();
        double multiplier = isThundering ? 1.5 : 1.0;
        double combat = user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.FIGHTING);
        double damage = (8.0 + (combat * 0.1)) * multiplier;

        player.sendMessage(ChatColor.AQUA + "The storm heeds your call!");

        new BukkitRunnable() {
            int strikes = 0;
            @Override
            public void run() {
                if (strikes >= 8 || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                List<Entity> nearby = player.getNearbyEntities(18, 12, 18);
                boolean hitAny = false;
                for (Entity entity : nearby) {
                    if (entity instanceof LivingEntity && entity != player && entity.getType() != org.bukkit.entity.EntityType.ARMOR_STAND) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        // Fake lightning effect (no fire/damage to blocks)
                        target.getWorld().strikeLightningEffect(target.getLocation());
                        
                        // Manual damage
                        target.setNoDamageTicks(0);
                        target.damage(damage, player);
                        
                        // Extra electric particles
                        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                        target.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                        hitAny = true;
                    }
                }
                
                if (hitAny) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.2f);
                }

                strikes++;
            }
        }.runTaskTimer(spellManager.getPlugin(), 0L, 15L);
        return true;
    }

    private boolean castStormCalling(Player player, SkillsUser user) {
        World world = player.getWorld();
        world.setStorm(true);
        world.setThundering(true);
        world.setThunderDuration(72000); // 1 hour
        
        spawnFlashyParticles(player.getLocation(), SpellTier.RARE, Particle.CLOUD);
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
        player.sendMessage(ChatColor.BLUE + "The sky darkens as you call upon the storm...");
        
        return true;
    }

    private boolean castMeteorShower(Player player, SkillsUser user) {
        Location targetLoc = null;
        org.bukkit.util.RayTraceResult ray = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 50, org.bukkit.FluidCollisionMode.NEVER);
        if (ray != null && ray.getHitBlock() != null) {
            targetLoc = ray.getHitBlock().getLocation();
        } else {
            targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(25));
        }

        double combat = user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.FIGHTING);
        double multiplier = 1.0 + (combat * 0.02);
        
        final Location center = targetLoc;
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        player.sendMessage(ChatColor.RED + "Meteor Shower descending!");
        
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 12) {
                    this.cancel();
                    return;
                }
                
                double xOff = random.nextDouble() * 14 - 7;
                double zOff = random.nextDouble() * 14 - 7;
                Location impactLoc = center.clone().add(xOff, 0, zOff);
                impactLoc.setY(impactLoc.getWorld().getHighestBlockYAt(impactLoc) + 0.1);

                spawnPointToPointMeteor(player, impactLoc, 8.0 * multiplier, 1.0);
                count++;
            }
        }.runTaskTimer(spellManager.getPlugin(), 10L, 5L);
        
        return true;
    }

    private boolean castChainLightning(Player player, SkillsUser user) {
        Entity target = getTarget(player, 15);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage(ChatColor.RED + "No target found!");
            return false;
        }

        boolean isThundering = player.getWorld().isThundering();
        double multiplier = isThundering ? 1.2 : 1.0;
        
        // Calculate staff tip position (approximate)
        Vector direction = player.getEyeLocation().getDirection();
        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        Location wandLoc = player.getEyeLocation().add(direction.multiply(0.6)).add(right.multiply(0.4)).subtract(0, 0.2, 0);
        
        spawnChainLightning(wandLoc, (LivingEntity) target, 5, multiplier, player);
        return true;
    }

    private void spawnChainLightning(Location start, LivingEntity target, int bounces, double multiplier, Player caster) {
        drawLightningBeam(start, target.getLocation().add(0, 1, 0));
        
        // Impact effect instead of sky lightning
        target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 3, 0.1, 0.1, 0.1, 0.01);
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 20, 0.2, 0.2, 0.2, 0.1);
        
        target.setNoDamageTicks(0);
        target.damage(8.0 * multiplier, caster);
        
        if (bounces <= 0) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity next = null;
                for (Entity e : target.getNearbyEntities(8, 8, 8)) {
                    if (e instanceof LivingEntity && e != target && e != caster && e.getType() != org.bukkit.entity.EntityType.PLAYER) {
                        next = (LivingEntity) e;
                        break;
                    }
                }
                if (next != null) {
                    spawnChainLightning(target.getLocation().add(0, 1, 0), next, bounces - 1, multiplier, caster);
                }
            }
        }.runTaskLater(spellManager.getPlugin(), 5L);
    }

    private void drawLightningBeam(Location start, Location end) {
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();
        for (double i = 0; i < dist; i += 0.3) { // Increased density
            Location point = start.clone().add(dir.clone().multiply(i));
            // Enhanced zigzag offset
            point.add(random.nextDouble()*0.5 - 0.25, random.nextDouble()*0.5 - 0.25, random.nextDouble()*0.5 - 0.25);
            point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 5, 0.05, 0.05, 0.05, 0.02);
            if (random.nextInt(3) == 0) point.getWorld().spawnParticle(Particle.GLOW, point, 1, 0, 0, 0, 0);
            if (random.nextInt(5) == 0) point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0.01);
        }
        start.getWorld().playSound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 2.0f);
    }

    private boolean castMalevolentShrine(Player player, SkillsUser user) {
        Location playerLoc = player.getLocation();
        final Location center = playerLoc.getWorld().getHighestBlockAt(playerLoc).getLocation().add(0, 0.1, 0);
        
        spawnFlashyParticles(center, SpellTier.MYTHIC, Particle.SOUL);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.5f);
        
        player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "DOMAIN EXPANSION: MALEVOLENT SHRINE");
        spellManager.getPlugin().getLogger().info("[SKE] Malevolent Shrine cast at " + center.getX() + ", " + center.getY() + ", " + center.getZ());

        new BukkitRunnable() {
            int strikes = 0;
            final int totalStrikes = 200; // 20 seconds at 10hz

            @Override
            public void run() {
                if (strikes >= totalStrikes || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Domain Boundary Visual (Soul particles)
                if (strikes % 5 == 0) {
                    for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i + (strikes * 2));
                    double x = Math.cos(rad) * 15;
                    double z = Math.sin(rad) * 15;
                    
                        center.getWorld().spawnParticle(Particle.SOUL, center.clone().add(x, 0.2, z), 3, 0.1, 0.1, 0.1, 0.02);
                        if (strikes % 20 == 0) {
                            center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, 0.5, z), 1, 0, 0.1, 0, 0.01);
                        }
                    }
                }

                // Use stationary center instead of player location for damage
                Collection<Entity> targets = center.getWorld().getNearbyEntities(center, 15, 10, 15);
                for (Entity entity : targets) {
                    if (entity instanceof LivingEntity && entity != player && entity.getType() != org.bukkit.entity.EntityType.ARMOR_STAND) {
                        LivingEntity target = (LivingEntity) entity;
                        if (!target.isDead() && target.isValid()) {
                            // Multiple slashes per entity
                            spawnSlashParticle(target.getLocation());
                            target.setNoDamageTicks(0);
                            target.damage(3.5, player); // Heavy damage
                            
                            // Visual feedback on impact
                            target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1, 0.2, 0.2, 0.2, 0);
                        }
                    }
                }

                if (strikes % 3 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 0.5f);
                    center.getWorld().playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.4f, 0.6f);
                }

                strikes++;
            }
        }.runTaskTimer(spellManager.getPlugin(), 0L, 2L);

        return true;
    }

    private void spawnSlashParticle(Location loc) {
        double offsetX = random.nextDouble() * 2 - 1;
        double offsetY = random.nextDouble() * 2;
        double offsetZ = random.nextDouble() * 2 - 1;
        Location pLoc = loc.clone().add(offsetX, offsetY, offsetZ);
        
        // Use CRIT and SWEEP_ATTACK instead of DUST to avoid "Missing Color data" errors in 1.21
        pLoc.getWorld().spawnParticle(Particle.CRIT, pLoc, 10, 0.2, 0.2, 0.2, 0.1);
        pLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, pLoc, 5, 0.1, 0.1, 0.1, 0.05);
        
        // Tiny line effect to look like a slash
        Vector dir = new Vector(random.nextDouble()-0.5, random.nextDouble()-0.5, random.nextDouble()-0.5).normalize();
        for (double d = 0; d < 1.0; d += 0.2) {
            pLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pLoc.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0);
        }
    }

    private Entity getTarget(Player player, int range) {
        List<Entity> entities = player.getNearbyEntities(range, range, range);
        Entity best = null;
        double bestAngle = 0.7; // Changed from 0.9 to 0.7 for easier targeting

        for (Entity e : entities) {
            if (!(e instanceof LivingEntity)) continue;
            Vector toEntity = e.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
            double dot = toEntity.dot(player.getLocation().getDirection());
            if (dot > bestAngle) {
                bestAngle = dot;
                best = e;
            }
        }
        return best;
    }
}
