package com.test.origins;

import dev.aurelium.slate.builder.MenuBuilder;
import dev.aurelium.slate.inv.content.SlotPos;
import dev.aurelium.slate.item.provider.ListBuilder;
import dev.aurelium.slate.position.FixedPosition;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class OriginMenu {

    private final OriginManager originManager;

    public OriginMenu(OriginManager originManager) {
        this.originManager = originManager;
    }

    public void build(MenuBuilder menu) {
        menu.template("origin", Origin.class, template -> {
            template.replace("name", p -> p.value().getDisplayName());
            template.replace("pros", p -> {
                List<String> list = new ArrayList<>();
                for (String pro : p.value().getPros()) {
                    list.add(ChatColor.GRAY + " + " + pro);
                }
                return String.join("\n", list);
            });
            template.replace("cons", p -> {
                List<String> list = new ArrayList<>();
                for (String con : p.value().getCons()) {
                    list.add(ChatColor.GRAY + " - " + con);
                }
                return String.join("\n", list);
            });

            template.definedContexts(m -> {
                Set<Origin> origins = new LinkedHashSet<>();
                for (Origin origin : Origin.values()) {
                    if (origin != Origin.NONE) {
                        origins.add(origin);
                    }
                }
                org.bukkit.Bukkit.getLogger().info("[SKE] Populating origins menu with " + origins.size() + " contexts");
                return origins;
            });

            template.slotPos(t -> {
                List<Origin> values = new ArrayList<>();
                for (Origin origin : Origin.values()) {
                    if (origin != Origin.NONE) {
                        values.add(origin);
                    }
                }
                int index = values.indexOf(t.value());
                int slot = 11 + index;
                if (index >= 5) slot = 20 + (index - 5);
                return SlotPos.of(slot / 9, slot % 9);
            });

            template.modify(t -> new ItemStack(t.value().getIcon()));

            template.onClick(c -> {
                originManager.setOrigin(c.player(), c.value());
                c.player().sendMessage(ChatColor.GREEN + "You are now a " + c.value().getDisplayName() + "!");
                c.player().closeInventory();
            });
        });

        menu.item("close", item -> item.onClick(c -> c.player().closeInventory()));
    }
}
