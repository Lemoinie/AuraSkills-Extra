package com.test.commands;

import com.test.MyPlugin;
import com.test.origins.Origin;
import com.test.origins.OriginManager;
import com.test.spells.Spell;
import com.test.spells.SpellManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SkeCommand implements CommandExecutor, TabCompleter {

    private final MyPlugin plugin;

    public SkeCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) return false;

        // --- /ske origins ... ---
        if (args[0].equalsIgnoreCase("origins")) {
            if (!sender.hasPermission("ske.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command!");
                return true;
            }

            if (args.length < 2) {
                if (sender instanceof Player) {
                    dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu((Player) sender, "origins");
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                }
                return true;
            }

            String sub = args[1].toLowerCase();
            OriginManager manager = plugin.getOriginManager();

            switch (sub) {
                case "reload":
                    plugin.reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "Origins configuration reloaded!");
                    return true;

                case "enable":
                    manager.setEnabled(true);
                    sender.sendMessage(ChatColor.GREEN + "Origins system enabled!");
                    return true;

                case "disable":
                    manager.setEnabled(false);
                    sender.sendMessage(ChatColor.RED + "Origins system disabled!");
                    return true;

                case "set":
                    if (args.length < 4) {
                        sender.sendMessage(ChatColor.RED + "Usage: /ske origins set <player> <origin>");
                        return true;
                    }
                    Player targetSet = plugin.getServer().getPlayer(args[2]);
                    if (targetSet == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found!");
                        return true;
                    }
                    try {
                        Origin origin = Origin.valueOf(args[3].toUpperCase());
                        manager.setOrigin(targetSet, origin);
                        sender.sendMessage(ChatColor.GREEN + "Set " + targetSet.getName() + "'s origin to " + origin.getDisplayName());
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid origin!");
                    }
                    return true;

                case "remove":
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /ske origins remove <player>");
                        return true;
                    }
                    Player targetRem = plugin.getServer().getPlayer(args[2]);
                    if (targetRem == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found!");
                        return true;
                    }
                    manager.removeOrigin(targetRem);
                    sender.sendMessage(ChatColor.GREEN + "Removed origin from " + targetRem.getName());
                    return true;

                default:
                    sender.sendMessage(ChatColor.RED + "Unknown sub-command!");
                    return true;
            }
        }

        // --- /ske spells ---
        if (args[0].equalsIgnoreCase("spells")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }
            Player p = (Player) sender;
            if (args.length > 1 && args[1].equalsIgnoreCase("getwand")) {
                if (!p.hasPermission("ske.admin")) {
                    p.sendMessage(ChatColor.RED + "You do not have permission to get the Spell Wand!");
                    return true;
                }
                ItemStack wand = new ItemStack(Material.BLAZE_ROD);
                ItemMeta wandMeta = wand.getItemMeta();
                if (wandMeta != null) {
                    wandMeta.setDisplayName(ChatColor.GOLD + "Spell Wand");
                    wandMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Left-Click to Cycle",
                        ChatColor.GRAY + "Right-Click to Cast"
                    ));
                    wand.setItemMeta(wandMeta);
                }
                p.getInventory().addItem(wand);
                p.sendMessage(ChatColor.GREEN + "You received a Spell Wand!");
                return true;
            }
            dev.aurelium.auraskills.api.AuraSkillsBukkit.get().getMenuManager().openMenu(p, "spells_main");
            return true;
        }

        // --- /ske admin spell <equip/unequip> <player> <spell> ---
        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("ske.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command!");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /ske admin <spell>");
                return true;
            }
            if (args[1].equalsIgnoreCase("spell")) {
                if (args.length < 5) {
                    sender.sendMessage(ChatColor.RED + "Usage: /ske admin spell <equip/unequip> <player> <spell>");
                    return true;
                }
                String action = args[2].toLowerCase();
                Player target = plugin.getServer().getPlayer(args[3]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                try {
                    Spell spell = Spell.valueOf(args[4].toUpperCase());
                    SpellManager spellManager = plugin.getSpellManager();
                    if (action.equals("equip")) {
                        spellManager.equipSpell(target, spell);
                        sender.sendMessage(ChatColor.GREEN + "Equipped " + spell.getDisplayName() + " for " + target.getName());
                    } else if (action.equals("unequip")) {
                        spellManager.unequipSpell(target, spell);
                        sender.sendMessage(ChatColor.GREEN + "Unequipped " + spell.getDisplayName() + " from " + target.getName());
                    } else {
                        sender.sendMessage(ChatColor.RED + "Invalid action! Use equip or unequip.");
                    }
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid spell!");
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("spells"));
            if (sender.hasPermission("ske.admin")) {
                subs.addAll(Arrays.asList("origins", "reload", "admin"));
            }
            return filter(subs, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("origins")) {
            return filter(Arrays.asList("set", "remove", "enable", "disable", "reload"), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("origins")) {
            String sub = args[1].toLowerCase();
            if (sub.equals("set") || sub.equals("remove")) {
                return null; // Standard player name completion
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("origins") && args[1].equalsIgnoreCase("set")) {
            return filter(Arrays.stream(Origin.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.toList()), args[3]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spells")) {
            return filter(Arrays.asList("getwand"), args[1]);
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) return filter(Arrays.asList("spell"), args[1]);
            if (args[1].equalsIgnoreCase("spell")) {
                if (args.length == 3) return filter(Arrays.asList("equip", "unequip"), args[2]);
                if (args.length == 4) return null; // Player names
                if (args.length == 5) return filter(Arrays.stream(Spell.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.toList()), args[4]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        if (input == null || input.isEmpty()) return list;
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
