package com.schoolminer;

import org.bukkit.ChatColor;

public class ConfigManager {
    private final Schoolminer plugin;

    public ConfigManager(Schoolminer plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        plugin.getLogger().info("§a✅ Đã reload config!");
    }

    public String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "&c⚠️ Không tìm thấy message: " + key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}