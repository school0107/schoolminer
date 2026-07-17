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
    
    private final Map<UUID, Integer> explosionLevels = new HashMap<>();
    private final Map<String, Integer> multiBlockLevels = new HashMap<>();

    public PlayerDataManager(Schoolminer plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().warning("§c⚠️ Không thể tạo playerdata.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        loadExplosionLevels();
        loadMultiBlockLevels();
        
        plugin.getLogger().info("§a✅ Đã load dữ liệu người chơi!");
        plugin.getLogger().info("§a✅ Đã load " + explosionLevels.size() + " cấp AutoKill");
        plugin.getLogger().info("§a✅ Đã load " + multiBlockLevels.size() + " cấp MultiBlock");
    }

    public void saveData() {
        try {
            dataConfig.set("autokill.levels", null);
            for (Map.Entry<UUID, Integer> entry : explosionLevels.entrySet()) {
                dataConfig.set("autokill.levels." + entry.getKey().toString(), entry.getValue());
            }
            
            dataConfig.set("multi-block-tools", null);
            for (Map.Entry<String, Integer> entry : multiBlockLevels.entrySet()) {
                dataConfig.set("multi-block-tools." + entry.getKey(), entry.getValue());
            }
            
            dataConfig.save(dataFile);
            plugin.getLogger().info("§a✅ Đã lưu dữ liệu người chơi!");
        } catch (Exception e) {
            plugin.getLogger().warning("§c⚠️ Lỗi khi lưu dữ liệu người chơi: " + e.getMessage());
        }
    }

    private void loadExplosionLevels() {
        explosionLevels.clear();
        if (dataConfig.contains("autokill.levels")) {
            for (String key : dataConfig.getConfigurationSection("autokill.levels").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int level = dataConfig.getInt("autokill.levels." + key, 0);
                    explosionLevels.put(uuid, level);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public int getExplosionLevel(Player player) {
        return explosionLevels.getOrDefault(player.getUniqueId(), 0);
    }

    public void setExplosionLevel(Player player, int level) {
        explosionLevels.put(player.getUniqueId(), level);
        saveData();
    }

    private void loadMultiBlockLevels() {
        multiBlockLevels.clear();
        if (dataConfig.contains("multi-block-tools")) {
            for (String key : dataConfig.getConfigurationSection("multi-block-tools").getKeys(false)) {
                int level = dataConfig.getInt("multi-block-tools." + key, 1);
                multiBlockLevels.put(key, level);
            }
        }
    }

    public int getMultiBlockLevel(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return 1;
        String toolKey = getItemKey(tool);
        return multiBlockLevels.getOrDefault(toolKey, 1);
    }

    public void setMultiBlockLevel(ItemStack tool, int level) {
        if (tool == null || tool.getType().isAir()) return;
        String toolKey = getItemKey(tool);
        multiBlockLevels.put(toolKey, level);
        saveData();
    }

    public void removeMultiBlockLevel(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return;
        String toolKey = getItemKey(tool);
        multiBlockLevels.remove(toolKey);
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

    public void removePlayerData(Player player) {
        explosionLevels.remove(player.getUniqueId());
        saveData();
    }
}