package com.test;

import com.test.commands.SkeCommand;
import com.test.origins.OriginListener;
import com.test.origins.OriginManager;
import com.test.origins.OriginMenu;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.AuraSkillsBukkit;
import dev.aurelium.slate.inv.content.SlotPos;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MyPlugin extends JavaPlugin {

    private OriginManager originManager;

    @Override
    public void onEnable() {
        this.originManager = new OriginManager(this);

        // Save resources
        saveDefaultFiles();

        // Register Commands
        if (getCommand("ske") != null) {
            SkeCommand skeCommand = new SkeCommand(this);
            getCommand("ske").setExecutor(skeCommand);
            getCommand("ske").setTabCompleter(skeCommand);
        }

        // Register Listeners
        getServer().getPluginManager().registerEvents(new OriginListener(originManager), this);

        // Register Menus
        AuraSkillsApi commonApi = AuraSkillsApi.get();
        AuraSkillsBukkit bukkitApi = AuraSkillsBukkit.get();

        // Register a NamespacedRegistry to allow AuraSkills to discover our menu folder
        dev.aurelium.auraskills.api.registry.NamespacedRegistry registry = commonApi.useRegistry("ske-extra", getDataFolder());
        registry.setMenuDirectory(new File(getDataFolder(), "menus"));

        // Register Origin as a context type so Slate's template system can handle it
        bukkitApi.getMenuManager().registerContext("Origin", com.test.origins.Origin.class, (menuName, input) -> {
            try {
                return com.test.origins.Origin.valueOf(input.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        });

        // Add our data folder to Slate's merge directories so it can find origins.yml
        bukkitApi.getMenuManager().buildMenu("origins", new OriginMenu(originManager)::build);

        // Hook into skills menu
        bukkitApi.getMenuManager().buildMenu("skills", menu -> {
            menu.template("origins_button", String.class, template -> {
                template.definedContexts(m -> java.util.Collections.singleton("default"));
                template.slotPos(t -> SlotPos.of(4, 4)); // Row 4, Col 4 = Slot 40 (Center bottom of 5x9)
                template.modify(t -> {
                    com.test.origins.Origin origin = originManager.getOrigin(t.player());
                    if (origin == null || origin == com.test.origins.Origin.NONE) {
                        // No origin – show Compass with prompt
                        ItemStack stack = new ItemStack(Material.COMPASS);
                        ItemMeta meta = stack.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName("§6§lOrigins");
                            meta.setLore(java.util.Arrays.asList(
                                "§7You haven't chosen an origin yet!",
                                "",
                                "§eClick to pick your origin."
                            ));
                            stack.setItemMeta(meta);
                        }
                        return stack;
                    }

                    // Origin chosen – show its icon with name and perks
                    ItemStack stack = new ItemStack(origin.getIcon());
                    ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§6§l" + origin.getDisplayName() + " §7(Your Origin)");
                        java.util.List<String> lore = new java.util.ArrayList<>();
                        lore.add("§8§m─────────────────────────");
                        lore.add("§a✦ Pros:");
                        for (String pro : origin.getPros()) {
                            lore.add("  §7+ " + pro);
                        }
                        lore.add("");
                        lore.add("§c✦ Cons:");
                        for (String con : origin.getCons()) {
                            lore.add("  §7- " + con);
                        }
                        lore.add("§8§m─────────────────────────");
                        lore.add("§eClick to change your origin.");
                        meta.setLore(lore);
                        stack.setItemMeta(meta);
                    }
                    return stack;
                });
                template.onClick(c -> bukkitApi.getMenuManager().openMenu(c.player(), "origins"));
            });
        });

        // Plugin startup logic
        if (getServer().getPluginManager().isPluginEnabled("AuraSkills")) {
            getLogger().info("AuraSkills found and enabled!");
        } else {
            getLogger().warning("AuraSkills not found! Some features may be disabled.");
        }
        getLogger().info("AuraSkills-Extra has been enabled!");
    }

    private void saveDefaultFiles() {
        File folder = new File(getDataFolder(), "menus");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File originsFile = new File(folder, "origins.yml");
        if (!originsFile.exists()) {
            saveResource("menus/origins.yml", false);
        }
    }

    @Override
    public void onDisable() {
        if (originManager != null) {
            originManager.cleanupAllPlayers();
        }
        // Plugin shutdown logic
        getLogger().info("AuraSkills-Extra has been disabled!");
    }

    public OriginManager getOriginManager() {
        return originManager;
    }
}
