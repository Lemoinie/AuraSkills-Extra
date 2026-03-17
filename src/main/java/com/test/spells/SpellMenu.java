package com.test.spells;

import dev.aurelium.slate.builder.MenuBuilder;
import dev.aurelium.slate.inv.content.SlotPos;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SpellMenu {

    private final SpellManager spellManager;

    public SpellMenu(SpellManager spellManager) {
        this.spellManager = spellManager;
    }

    public void buildMain(MenuBuilder menu) {
        menu.item("learn", item -> {
            item.onClick(c -> {
                dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_learn");
            });
        });

        menu.item("equip", item -> {
            item.onClick(c -> {
                dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_equip");
            });
        });

        menu.item("close", item -> item.onClick(c -> c.player().closeInventory()));
    }

    public void buildLearn(MenuBuilder menu) {
        menu.template("spell", Spell.class, template -> {
            template.definedContexts(m -> {
                return new LinkedHashSet<>(Arrays.asList(Spell.values()));
            });

            template.replace("name", p -> p.value().getTier().getColor() + p.value().getDisplayName());
            template.replace("tier", p -> p.value().getTier().getDisplayName());
            template.replace("cost", p -> String.valueOf(p.value().getTier().getXpCost()));
            template.replace("wisdom", p -> String.valueOf(p.value().getTier().getWisdomRequirement()));
            template.replace("description", p -> p.value().getDescription());

            template.slotPos(t -> {
                int index = Arrays.asList(Spell.values()).indexOf(t.value());
                return SlotPos.of(1 + (index / 7), 1 + (index % 7));
            });

            template.modify(t -> new ItemStack(Material.BOOK));

            template.onClick(c -> {
                Player player = c.player();
                Spell spell = c.value();
                
                if (spellManager.isLearned(player, spell)) {
                    player.sendMessage(ChatColor.RED + "You already know this spell!");
                    return;
                }

                if (spellManager.canLearn(player, spell)) {
                    player.setLevel(player.getLevel() - spell.getTier().getXpCost());
                    spellManager.learnSpell(player, spell);
                    player.sendMessage(ChatColor.GREEN + "You learned " + spell.getTier().getColor() + spell.getDisplayName() + ChatColor.GREEN + "!");
                    dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu(player, "spells_learn");
                } else {
                    player.sendMessage(ChatColor.RED + "You do not meet the requirements to learn this spell!");
                }
            });
        });

        menu.item("back", item -> item.onClick(c -> {
            dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_main");
        }));
    }

    public void buildEquip(MenuBuilder menu) {
        menu.template("learned_spell", Spell.class, template -> {
            template.definedContexts(m -> spellManager.getLearnedSpells(m.player()));

            template.replace("name", p -> p.value().getTier().getColor() + p.value().getDisplayName());

            template.replace("status", p -> {
                if (spellManager.isEquipped(p.player(), p.value())) {
                    return ChatColor.GREEN + "[EQUIPPED]";
                } else {
                    return ChatColor.GRAY + "[CLICK TO EQUIP]";
                }
            });

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
                dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu(player, "spells_equip");
            });
        });

        menu.item("back", item -> item.onClick(c -> {
            dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu(c.player(), "spells_main");
        }));
    }
}
