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
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class SpellListener implements Listener {

    private final SpellManager spellManager;
    private final Random random = new Random();

    public SpellListener(SpellManager spellManager) {
        this.spellManager = spellManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
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
        }
    }

    private void castSpell(Player player) {
        Spell spell = spellManager.getActiveSpell(player);
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
        if (user == null) return;

        double manaCost = 20;
        if (spell.getTier() == SpellTier.COMMON) manaCost = 15;
        else if (spell.getTier() == SpellTier.UNCOMMON) manaCost = 30;
        else if (spell.getTier() == SpellTier.RARE) manaCost = 60;
        else if (spell.getTier() == SpellTier.LEGENDARY) manaCost = 150;
        else if (spell.getTier() == SpellTier.MYTHIC) manaCost = 300;

        if (spellManager.hasSurgeDiscount(player)) {
            manaCost *= 0.9;
        }

        if (user.getMana() < manaCost) {
            player.sendMessage(ChatColor.RED + "Not enough Mana! (" + (int)manaCost + " required)");
            return;
        }

        boolean success = false;
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
            case METEOR_SHOWER: success = castMeteorShower(player, user); break;
            case CHAIN_LIGHTNING: success = castChainLightning(player, user); break;
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
        long cd = 5;
        switch (spell) {
            case SPARK: cd = 1; break;
            case KINETIC_SHOVE: cd = 3; break;
            case MAGE_LIGHT: cd = 5; break;
            case NATURES_ROOT: cd = 8; break;
            case FIREBOLT: cd = 5; break;
            case FROST_TOUCH: cd = 8; break;
            case ARCANE_SURGE: cd = 15; break;
            case THUNDERSTORM: cd = 60; break;
            case METEOR: cd = 45; break;
            case METEOR_SHOWER: cd = 60; break;
            case CHAIN_LIGHTNING: cd = 20; break;
        }
        spellManager.setCooldown(player, spell, cd);
    }

    private void spawnFlashyParticles(Location loc, SpellTier tier, Particle particle) {
        int count = 10;
        double speed = 0.1;
        double offset = 0.5;

        if (tier == SpellTier.UNCOMMON) { count = 30; offset = 1.0; }
        else if (tier == SpellTier.RARE) { count = 60; speed = 0.2; offset = 1.5; }
        else if (tier == SpellTier.LEGENDARY) { count = 150; speed = 0.3; offset = 2.0; }
        else if (tier == SpellTier.MYTHIC) { count = 300; speed = 0.5; offset = 3.0; }

        loc.getWorld().spawnParticle(particle, loc, count, offset, offset, offset, speed);
        if (tier == SpellTier.LEGENDARY || tier == SpellTier.MYTHIC) {
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 5, 0.1, 0.1, 0.1, 0.01);
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 2, 0.1, 0.1, 0.1, 0.01);
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
        Entity target = getTarget(player, 25);
        Location targetLoc = target != null ? target.getLocation() : player.getTargetBlockExact(25) != null ? player.getTargetBlockExact(25).getLocation() : null;
        if (targetLoc == null) {
            player.sendMessage(ChatColor.RED + "Target too far away!");
            return false;
        }

        Location spawnLoc = targetLoc.clone().add(0, 20, 0);
        spawnFlashyParticles(spawnLoc, SpellTier.LEGENDARY, Particle.FLAME);
        
        // Spawn multiple magma blocks for a "bigger" look
        final Location fTargetLoc = targetLoc;
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 3) { this.cancel(); return; }
                FallingBlock meteor = spawnLoc.getWorld().spawnFallingBlock(spawnLoc.clone().add(random.nextDouble()-0.5, 0, random.nextDouble()-0.5), Material.MAGMA_BLOCK.createBlockData());
                meteor.setDropItem(false);
                meteor.setHurtEntities(true);
                handleMeteorImpact(meteor, fTargetLoc, 6.0f);
                count++;
            }
        }.runTaskTimer(spellManager.getPlugin(), 0L, 2L);
        
        return true;
    }

    private void handleMeteorImpact(FallingBlock meteor, Location targetLoc, float radius) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (meteor.isDead() || meteor.isOnGround()) {
                    Location hitLoc = meteor.getLocation();
                    hitLoc.getWorld().createExplosion(hitLoc, radius, false, false); // Non-destructive
                    spawnFlashyParticles(hitLoc, SpellTier.LEGENDARY, Particle.FLAME);
                    this.cancel();
                } else {
                    meteor.getWorld().spawnParticle(Particle.LARGE_SMOKE, meteor.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                    meteor.getWorld().spawnParticle(Particle.FLAME, meteor.getLocation(), 5, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }.runTaskTimer(spellManager.getPlugin(), 0L, 1L);
    }

    private boolean castThunderstorm(Player player, SkillsUser user) {
        spawnFlashyParticles(player.getLocation(), SpellTier.LEGENDARY, Particle.ELECTRIC_SPARK);
        new BukkitRunnable() {
            int strikes = 0;
            @Override
            public void run() {
                if (strikes >= 8) { this.cancel(); return; }
                for (Entity entity : player.getNearbyEntities(12, 12, 12)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        entity.getWorld().strikeLightning(entity.getLocation());
                    }
                }
                strikes++;
            }
        }.runTaskTimer(spellManager.getPlugin(), 0L, 8L);
        return true;
    }

    private boolean castMeteorShower(Player player, SkillsUser user) {
        Entity target = getTarget(player, 25);
        Location center = target != null ? target.getLocation() : player.getTargetBlockExact(25) != null ? player.getTargetBlockExact(25).getLocation() : player.getLocation();
        
        spawnFlashyParticles(center, SpellTier.LEGENDARY, Particle.SOUL_FIRE_FLAME);
        
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 12) { this.cancel(); return; }
                Location spawn = center.clone().add(random.nextInt(11)-5, 20 + random.nextInt(5), random.nextInt(11)-5);
                FallingBlock mb = spawn.getWorld().spawnFallingBlock(spawn, Material.GILDED_BLACKSTONE.createBlockData());
                mb.setDropItem(false);
                mb.setHurtEntities(true);
                handleMeteorImpact(mb, spawn.clone().subtract(0, 20, 0), 3.0f);
                count++;
            }
        }.runTaskTimer(spellManager.getPlugin(), 0L, 5L);
        return true;
    }

    private boolean castChainLightning(Player player, SkillsUser user) {
        Entity target = getTarget(player, 15);
        if (!(target instanceof LivingEntity)) {
            player.sendMessage(ChatColor.RED + "No target found!");
            return false;
        }

        spawnChainLightning(player.getEyeLocation(), (LivingEntity) target, 5);
        return true;
    }

    private void spawnChainLightning(Location start, LivingEntity target, int bounces) {
        drawLightningBeam(start, target.getLocation().add(0, 1, 0));
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.damage(8.0);
        
        if (bounces <= 0) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity next = null;
                for (Entity e : target.getNearbyEntities(8, 8, 8)) {
                    if (e instanceof LivingEntity && e != target && e.getType() != org.bukkit.entity.EntityType.PLAYER) {
                        next = (LivingEntity) e;
                        break;
                    }
                }
                if (next != null) {
                    spawnChainLightning(target.getLocation().add(0, 1, 0), next, bounces - 1);
                }
            }
        }.runTaskLater(spellManager.getPlugin(), 5L);
    }

    private void drawLightningBeam(Location start, Location end) {
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();
        for (double i = 0; i < dist; i += 0.5) {
            Location point = start.clone().add(dir.clone().multiply(i));
            // Add zigzag offset
            point.add(random.nextDouble()*0.4 - 0.2, random.nextDouble()*0.4 - 0.2, random.nextDouble()*0.4 - 0.2);
            point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 3, 0.05, 0.05, 0.05, 0.02);
            if (random.nextInt(5) == 0) point.getWorld().spawnParticle(Particle.GLOW, point, 1, 0, 0, 0, 0);
        }
        start.getWorld().playSound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
    }

    private Entity getTarget(Player player, int range) {
        List<Entity> entities = player.getNearbyEntities(range, range, range);
        Entity best = null;
        double bestAngle = 0.9;

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
