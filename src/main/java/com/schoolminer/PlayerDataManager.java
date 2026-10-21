package com.schoolminer;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.util.*;

public class PlayerDataManager {
    private final Schoolminer plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    private final Map<String, Integer> multiBlockLevels = new HashMap<>();

    public PlayerDataManager(Schoolminer plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (Exception ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadMultiBlockLevels();
        plugin.getLogger().info("§a✅ Đã load " + multiBlockLevels.size() + " cấp MultiBlock");
    }

    public void saveData() {
        try {
            dataConfig.set("multi-block-tools", null);
            for (Map.Entry<String, Integer> entry : multiBlockLevels.entrySet()) {
                dataConfig.set("multi-block-tools." + entry.getKey(), entry.getValue());
            }
            dataConfig.save(dataFile);
        } catch (Exception e) {
            plugin.getLogger().warning("§c⚠️ Lỗi khi lưu dữ liệu: " + e.getMessage());
        }
    }

    private void loadMultiBlockLevels() {
        multiBlockLevels.clear();
        if (dataConfig.contains("multi-block-tools")) {
            for (String key : dataConfig.getConfigurationSection("multi-block-tools").getKeys(false)) {
                multiBlockLevels.put(key, dataConfig.getInt("multi-block-tools." + key, 1));
            }
        }
    }

    public int getMultiBlockLevel(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return 1;
        return multiBlockLevels.getOrDefault(getItemKey(tool), 1);
    }

    public void setMultiBlockLevel(ItemStack tool, int level) {
        if (tool == null || tool.getType().isAir()) return;
        multiBlockLevels.put(getItemKey(tool), level);
        saveData();
    }

    public void removeMultiBlockLevel(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return;
        multiBlockLevels.remove(getItemKey(tool));
        saveData();
    }

    public String getItemKey(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return "";
        String baseKey = tool.getType().name();
        if (tool.hasItemMeta()) {
            if (tool.getItemMeta().hasDisplayName()) {
                baseKey += "_" + tool.getItemMeta().getDisplayName().hashCode();
            }
            if (tool.getItemMeta().hasLore()) {
                baseKey += "_" + tool.getItemMeta().getLore().hashCode();
            }
        }
        if (!multiBlockLevels.containsKey(baseKey)) {
            baseKey += "_" + System.currentTimeMillis();
        }
        return baseKey;
    }
}