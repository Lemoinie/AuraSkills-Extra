package com.test.commands;

import com.test.MyPlugin;
import com.test.origins.Origin;
import com.test.origins.OriginManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        // Backward compatibility for /ske reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ske.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission!");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("ske.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return filter(Arrays.asList("origins", "reload"), args[0]);
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

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        if (input == null || input.isEmpty()) return list;
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
