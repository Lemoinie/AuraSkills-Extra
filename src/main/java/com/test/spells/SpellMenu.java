package com.test.spells;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.AuraSkillsBukkit;
import dev.aurelium.auraskills.api.user.SkillsUser;
import dev.aurelium.slate.builder.MenuBuilder;
import dev.aurelium.slate.inv.content.SlotPos;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SpellMenu {

    private final SpellManager spellManager;
    private final Map<UUID, UUID> adminInspectMap = new HashMap<>();

    public SpellMenu(SpellManager spellManager) {
        this.spellManager = spellManager;
    }

    public void buildMain(MenuBuilder menu) {
        menu.item("learn", item -> {
            item.onClick(c -> {
                AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_learn");
            });
        });

        menu.item("equip", item -> {
            item.onClick(c -> {
                AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_equip");
            });
        });

        menu.item("close", item -> item.onClick(c -> c.player().closeInventory()));
    }

    public void buildLearn(MenuBuilder menu) {
        menu.template("spell", Spell.class, template -> {
            org.bukkit.Bukkit.getLogger().info("[SKE] Building spell template in buildLearn");
            template.replace("name", m -> m.value().getDisplayName());
            template.replace("tier", m -> m.value().getTier().name());
            template.replace("cost", m -> String.valueOf(m.value().getXpCost()));
            template.replace("wisdom", m -> String.valueOf(m.value().getWisdomRequirement()));
            template.replace("description", m -> m.value().getDescription());

            template.definedContexts(m -> {
                Set<Spell> values = new LinkedHashSet<>(Arrays.asList(Spell.values()));
                org.bukkit.Bukkit.getLogger().info("[SKE] spells_learn: found " + values.size() + " spells");
                return values;
            });

            template.modify(m -> {
                Spell spell = m.value();
                org.bukkit.Bukkit.getLogger().info("[SKE] Modifying spell item for " + (spell != null ? spell.name() : "NULL"));
                ItemStack item = new ItemStack(Material.BOOK);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Tier: " + ChatColor.WHITE + spell.getTier());
                    lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + spell.getXpCost() + " XP Levels");
                    lore.add(ChatColor.GRAY + "Wisdom Req: " + ChatColor.AQUA + spell.getWisdomRequirement());
                    lore.add("");
                    lore.add(ChatColor.GRAY + spell.getDescription());
                    lore.add("");

                    SkillsUser user = AuraSkillsApi.get().getUserManager().getUser(m.player().getUniqueId());
                    double wisdom = user != null ? user.getStatLevel(dev.aurelium.auraskills.api.stat.Stats.WISDOM) : 0;

                    if (spellManager.isLearned(m.player(), spell)) {
                        lore.add(ChatColor.GREEN + "Already Learnt");
                    } else if (m.player().getLevel() < spell.getXpCost() || wisdom < spell.getWisdomRequirement()) {
                        lore.add(ChatColor.RED + "Insufficient Requirements");
                    } else {
                        lore.add(ChatColor.YELLOW + "Click to learn!");
                    }
                    meta.setLore(lore);
                    meta.setDisplayName(ChatColor.GOLD + spell.getDisplayName());
                    item.setItemMeta(meta);
                }
                return item;
            });

            template.slotPos(t -> {
                int index = Arrays.asList(Spell.values()).indexOf(t.value());
                int slot;
                if (index < 7) slot = 10 + index;
                else if (index < 14) slot = 19 + (index - 7);
                else slot = 28 + (index - 14);
                return SlotPos.of(slot / 9, slot % 9);
            });

            template.onClick(c -> {
                Player player = c.player();
                Spell spell = c.value();
                
                if (spellManager.isLearned(player, spell)) {
                    player.sendMessage(ChatColor.RED + "You already know this spell!");
                    return;
                }

                if (spellManager.canLearn(player, spell)) {
                    player.setLevel(player.getLevel() - spell.getXpCost());
                    spellManager.learnSpell(player, spell);
                    player.sendMessage(ChatColor.GREEN + "You learned " + spell.getDisplayName() + "!");
                    AuraSkillsBukkit.get().getMenuManager().openMenu(player, "spells_learn");
                } else {
                    player.sendMessage(ChatColor.RED + "You do not meet the requirements to learn this spell!");
                }
            });
        });

        menu.item("back", item -> item.onClick(c -> {
            AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_main");
        }));
    }

    public void buildEquip(MenuBuilder menu) {
        menu.template("learned_spell", Spell.class, template -> {
            template.definedContexts(m -> spellManager.getLearnedSpells(m.player()));
            template.replace("name", p -> p.value().getDisplayName());
            template.replace("status", p -> spellManager.isEquipped(p.player(), p.value()) ? ChatColor.GREEN + "[EQUIPPED]" : ChatColor.GRAY + "[CLICK TO EQUIP]");

            template.slotPos(t -> {
                List<Spell> list = new ArrayList<>(spellManager.getLearnedSpells(t.player()));
                int index = list.indexOf(t.value());
                return SlotPos.of(1 + (index / 7), 1 + (index % 7));
            });

            template.modify(t -> {
                Material mat = spellManager.isEquipped(t.player(), t.value()) ? Material.ENCHANTED_BOOK : Material.BOOK;
                return new ItemStack(mat);
            });

            template.onClick(c -> {
                Player player = c.player();
                Spell spell = c.value();
                if (spellManager.isEquipped(player, spell)) {
                    spellManager.unequipSpell(player, spell);
                    player.sendMessage(ChatColor.YELLOW + "Unequipped " + spell.getDisplayName());
                } else {
                    if (spellManager.getEquippedSpells(player).size() >= 6) {
                        player.sendMessage(ChatColor.RED + "You can only equip up to 6 spells!");
                    } else {
                        spellManager.equipSpell(player, spell);
                        player.sendMessage(ChatColor.GREEN + "Equipped " + spell.getDisplayName());
                    }
                }
                AuraSkillsBukkit.get().getMenuManager().openMenu(player, "spells_equip");
            });
        });

        menu.item("back", item -> item.onClick(c -> {
            AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_main");
        }));
    }

    public void buildAdminMain(MenuBuilder menu) {
        menu.item("give_spell", item -> {
            item.onClick(c -> {
                AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_admin_players");
            });
        });

        menu.item("back", item -> item.onClick(c -> {
            AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_main");
        }));
    }

    public void buildAdminPlayerList(MenuBuilder menu) {
        org.bukkit.Bukkit.getLogger().info("[SKE] Building Admin Player List menu");
        menu.template("player", Player.class, template -> {
            template.replace("player_name", p -> p.value().getName());
            template.definedContexts(m -> {
                Set<Player> players = new LinkedHashSet<>(org.bukkit.Bukkit.getOnlinePlayers());
                org.bukkit.Bukkit.getLogger().info("[SKE] spells_admin_players: found " + players.size() + " online players");
                return players;
            });

            template.slotPos(t -> {
                List<Player> players = new ArrayList<>(t.player().getServer().getOnlinePlayers());
                int index = players.indexOf(t.value());
                return SlotPos.of(index / 9, index % 9);
            });

            template.onClick(c -> {
                Player target = c.value();
                adminInspectMap.put(c.player().getUniqueId(), target.getUniqueId());
                AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_admin_spells");
            });
        });

        menu.item("back", item -> item.onClick(c -> {
            AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_admin");
        }));
    }

    public void buildAdminSpellList(MenuBuilder menu) {
        menu.template("spell", Spell.class, template -> {
            template.definedContexts(m -> new LinkedHashSet<>(Arrays.asList(Spell.values())));
            template.replace("name", p -> p.value().getDisplayName());
            template.replace("tier", p -> p.value().getTier().name());
            
            template.replace("status", p -> {
                UUID targetId = adminInspectMap.get(p.player().getUniqueId());
                if (targetId == null) return ChatColor.RED + "Error: No player selected";
                Player target = p.player().getServer().getPlayer(targetId);
                if (target == null) return ChatColor.RED + "Player offline";
                return spellManager.isLearned(target, p.value()) ? ChatColor.GREEN + "[LEARNED]" : ChatColor.GRAY + "[NOT LEARNED]";
            });

            template.replace("target_name", p -> {
                UUID targetId = adminInspectMap.get(p.player().getUniqueId());
                if (targetId == null) return "Unknown";
                Player target = p.player().getServer().getPlayer(targetId);
                return target != null ? target.getName() : "Unknown";
            });

            template.modify(t -> {
                UUID targetId = adminInspectMap.get(t.player().getUniqueId());
                boolean learned = false;
                if (targetId != null) {
                    Player target = t.player().getServer().getPlayer(targetId);
                    if (target != null) {
                        learned = spellManager.isLearned(target, t.value());
                    }
                }
                return new ItemStack(learned ? Material.ENCHANTED_BOOK : Material.BOOK);
            });

            template.onClick(c -> {
                UUID targetId = adminInspectMap.get(c.player().getUniqueId());
                if (targetId == null) return;
                Player target = c.player().getServer().getPlayer(targetId);
                if (target == null) return;

                Spell spell = c.value();
                if (spellManager.isLearned(target, spell)) {
                    c.player().sendMessage(ChatColor.YELLOW + target.getName() + " already knows this spell.");
                } else {
                    spellManager.learnSpell(target, spell);
                    c.player().sendMessage(ChatColor.GREEN + "Gave " + spell.getDisplayName() + " to " + target.getName());
                    AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_admin_spells");
                }
            });
        });

        menu.item("back", item -> item.onClick(c -> {
            AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_admin_players");
        }));
    }
}
