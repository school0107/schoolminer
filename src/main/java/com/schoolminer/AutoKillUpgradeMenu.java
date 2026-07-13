package com.schoolminer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import java.util.*;

public class AutoKillUpgradeMenu {
    private final Schoolminer plugin;
    private final AutoKillManager killManager;
    private final ConfigManager configManager;
    private Economy economy;

    public AutoKillUpgradeMenu(Schoolminer plugin) {
        this.plugin = plugin;
        this.killManager = plugin.getAutoKillManager();
        this.configManager = plugin.getConfigManager();
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, "§6§l⚔️ Nâng Cấp AutoKill");

        int currentLevel = killManager.getExplosionLevel(player);
        double currentChance = killManager.getExplosionChance(player);
        double currentRadius = killManager.getExplosionRadius(player);
        int maxLevel = configManager.getMaxExplosionLevel();

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§l⚔️ Nâng Cấp AutoKill");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Level hiện tại: §e" + currentLevel + "/" + maxLevel);
        infoLore.add("");
        infoLore.add("§6✦ Sát thương nổ:");
        infoLore.add("  §7Tỉ lệ: §e" + String.format("%.1f", currentChance * 100) + "%");
        infoLore.add("  §7Bán kính: §e" + String.format("%.1f", currentRadius) + " block");
        infoLore.add("");
        infoLore.add("§6✦ Nâng cấp tiếp theo:");
        if (currentLevel < maxLevel) {
            double nextChance = configManager.getExplosionChanceAtLevel(currentLevel + 1);
            double nextRadius = configManager.getExplosionRadiusAtLevel(currentLevel + 1);
            double cost = configManager.getUpgradeCost(currentLevel + 1);
            infoLore.add("  §7Tỉ lệ: §e" + String.format("%.1f", nextChance * 100) + "%");
            infoLore.add("  §7Bán kính: §e" + String.format("%.1f", nextRadius) + " block");
            infoLore.add("  §7Giá: §6$" + String.format("%,.0f", cost));
        } else {
            infoLore.add("  §c✦ Đã đạt cấp tối đa!");
        }
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        menu.setItem(4, info);

        ItemStack upgrade = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta upgradeMeta = upgrade.getItemMeta();
        if (currentLevel < maxLevel) {
            double cost = configManager.getUpgradeCost(currentLevel + 1);
            upgradeMeta.setDisplayName("§a§l🔼 NÂNG CẤP");
            if (economy != null) {
                double balance = economy.getBalance(player);
                upgradeMeta.setLore(Arrays.asList(
                    "§7Giá: §6$" + String.format("%,.0f", cost),
                    "§7Số dư: §e$" + String.format("%,.0f", balance),
                    "",
                    (balance >= cost ? "§a✅ Đủ tiền!" : "§c❌ Không đủ tiền!"),
                    "",
                    "§eClick để nâng cấp!"
                ));
            } else {
                upgradeMeta.setLore(Arrays.asList(
                    "§7Giá: §6$" + String.format("%,.0f", cost),
                    "",
                    "§eClick để nâng cấp!"
                ));
            }
        } else {
            upgradeMeta.setDisplayName("§c§l✅ ĐÃ ĐẠT CẤP TỐI ĐA");
            upgradeMeta.setLore(Arrays.asList(
                "§7Bạn đã đạt cấp cao nhất!"
            ));
        }
        upgrade.setItemMeta(upgradeMeta);
        menu.setItem(13, upgrade);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§c§l◀ QUAY LẠI");
        backMeta.setLore(Arrays.asList("§7Quay lại menu chính"));
        back.setItemMeta(backMeta);
        menu.setItem(22, back);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, glass);
            }
        }

        player.openInventory(menu);
    }

    public void handleClick(Player player, int slot) {
        if (slot == 13) {
            int currentLevel = killManager.getExplosionLevel(player);
            int maxLevel = configManager.getMaxExplosionLevel();
            
            if (currentLevel >= maxLevel) {
                player.sendMessage("§c❌ Bạn đã đạt cấp tối đa!");
                return;
            }

            double cost = configManager.getUpgradeCost(currentLevel + 1);
            
            if (economy == null) {
                player.sendMessage("§c❌ Hệ thống kinh tế chưa được kích hoạt!");
                return;
            }

            // KIỂM TRA ĐỦ TIỀN
            if (!economy.has(player, cost)) {
                player.sendMessage("§c❌ Bạn không đủ tiền! Cần §6$" + String.format("%,.0f", cost));
                player.sendMessage("§7Số dư hiện tại: §e$" + String.format("%,.0f", economy.getBalance(player)));
                openMenu(player);
                return;
            }

            // TRỪ TIỀN
            economy.withdrawPlayer(player, cost);
            
            // NÂNG CẤP - LƯU NGAY LẬP TỨC
            killManager.setExplosionLevel(player, currentLevel + 1);
            
            player.sendMessage("§a✅ Nâng cấp thành công lên level §e" + (currentLevel + 1) + "§a!");
            player.sendMessage("§7Tỉ lệ nổ: §e" + String.format("%.1f", killManager.getExplosionChance(player) * 100) + "%");
            player.sendMessage("§7Bán kính: §e" + String.format("%.1f", killManager.getExplosionRadius(player)) + " block");
            player.sendMessage("§7Số dư còn lại: §e$" + String.format("%,.0f", economy.getBalance(player)));
            
            openMenu(player);
        } else if (slot == 22) {
            player.performCommand("autokill");
        }
    }
}