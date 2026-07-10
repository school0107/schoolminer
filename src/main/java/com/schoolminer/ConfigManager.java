package com.schoolminer;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;
import java.util.*;

public class ConfigManager {
    private final Schoolminer plugin;
    private Set<Material> whitelist;
    private int mineDelay;
    private int attackDelay;
    private int maxRange;
    private int killRange;
    private double baseDamage;
    private boolean killMonster;
    private boolean killAnimal;
    private boolean killMob;
    private boolean dropItems;
    private boolean dropXp;
    private int xpMonster;
    private int xpAnimal;
    private int xpMob;

    public ConfigManager(Schoolminer plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        // Auto Mine
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
        
        mineDelay = config.getInt("automine.delay", 20);
        maxRange = config.getInt("automine.max-range", 5);
        
        // Auto Kill
        attackDelay = config.getInt("autokill.attack-delay", 40);
        killRange = config.getInt("autokill.range", 3);
        baseDamage = config.getDouble("autokill.base-damage", 4.0);
        killMonster = config.getBoolean("autokill.kill-monster", true);
        killAnimal = config.getBoolean("autokill.kill-animal", false);
        killMob = config.getBoolean("autokill.kill-mob", true);
        dropItems = config.getBoolean("autokill.drop-items", true);
        dropXp = config.getBoolean("autokill.drop-xp", true);
        xpMonster = config.getInt("autokill.xp-monster", 5);
        xpAnimal = config.getInt("autokill.xp-animal", 1);
        xpMob = config.getInt("autokill.xp-mob", 3);
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

    public int getKillRange() {
        return killRange;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public boolean isKillMonster() {
        return killMonster;
    }

    public boolean isKillAnimal() {
        return killAnimal;
    }

    public boolean isKillMob() {
        return killMob;
    }

    public boolean isDropItems() {
        return dropItems;
    }

    public boolean isDropXp() {
        return dropXp;
    }

    public int getXpMonster() {
        return xpMonster;
    }

    public int getXpAnimal() {
        return xpAnimal;
    }

    public int getXpMob() {
        return xpMob;
    }

    public String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "&c⚠️ Không tìm thấy message: " + key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
