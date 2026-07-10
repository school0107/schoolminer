package com.schoolminer;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;
import java.util.*;

@SuppressWarnings("deprecation")
public class ConfigManager {
    private final Schoolminer plugin;
    private Set<Material> whitelist;
    private int mineDelay;
    private int attackDelay;
    private int maxRange;

    public ConfigManager(Schoolminer plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        whitelist = new HashSet<>();
        List<String> blockNames = config.getStringList("whitelist");
        for (String name : blockNames) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                whitelist.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("§c⚠️ Block không hợp lệ: " + name);
            }
        }
        
        mineDelay = config.getInt("settings.mine-delay", 5);
        attackDelay = config.getInt("settings.attack-delay", 10);
        maxRange = config.getInt("settings.max-range", 5);
    }

    public boolean isWhitelisted(Material material) {
        return whitelist.contains(material);
    }

    public int getMineDelay() {
        return mineDelay;
    }

    public int getAttackDelay() {
        return attackDelay;
    }

    public int getMaxRange() {
        return maxRange;
    }

    public String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "&c⚠️ Không tìm thấy message: " + key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
