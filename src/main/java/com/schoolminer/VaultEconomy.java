package com.schoolminer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomy {
    private Object economy; // Dùng Object để tránh lỗi compile nếu không có Vault

    public VaultEconomy(Schoolminer plugin) {
        if (!setupEconomy()) {
            plugin.getLogger().warning("§c⚠️ Không tìm thấy Vault! Tính năng kinh tế sẽ không hoạt động!");
        } else {
            plugin.getLogger().info("§a✅ Đã kết nối Vault Economy!");
        }
    }

    private boolean setupEconomy() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                return false;
            }
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(
                Class.forName("net.milkbowl.vault.economy.Economy")
            );
            if (rsp == null) {
                return false;
            }
            economy = rsp.getProvider();
            return economy != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean has(Player player, double amount) {
        try {
            if (economy == null) return false;
            java.lang.reflect.Method method = economy.getClass().getMethod("has", Player.class, double.class);
            return (boolean) method.invoke(economy, player, amount);
        } catch (Exception e) {
            return false;
        }
    }

    public void withdraw(Player player, double amount) {
        try {
            if (economy == null) return;
            java.lang.reflect.Method method = economy.getClass().getMethod("withdrawPlayer", Player.class, double.class);
            method.invoke(economy, player, amount);
        } catch (Exception ignored) {}
    }

    public void deposit(Player player, double amount) {
        try {
            if (economy == null) return;
            java.lang.reflect.Method method = economy.getClass().getMethod("depositPlayer", Player.class, double.class);
            method.invoke(economy, player, amount);
        } catch (Exception ignored) {}
    }

    public boolean isEnabled() {
        return economy != null;
    }
}