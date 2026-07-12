package com.schoolminer;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomy {
    private Economy economy;

    public VaultEconomy(Schoolminer plugin) {
        if (!setupEconomy()) {
            plugin.getLogger().warning("§c⚠️ Không tìm thấy Vault! Tính năng kinh tế sẽ không hoạt động!");
        } else {
            plugin.getLogger().info("§a✅ Đã kết nối Vault Economy!");
        }
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

    public boolean has(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    public void withdraw(Player player, double amount) {
        if (economy != null) {
            economy.withdrawPlayer(player, amount);
        }
    }

    public void deposit(Player player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }
}