package com.schoolminer;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class PlayerDataManager {
    private final Schoolminer plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final NamespacedKey itemKey;
    
    private final Map<String, Integer> multiBlockLevels = new HashMap<>();

    public PlayerDataManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "multiblock_id");
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
        if (tool == null || tool.getType().isAir() || !tool.hasItemMeta()) return 1;
        
        String id = tool.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (id == null) return 1;
        
        return multiBlockLevels.getOrDefault(id, 1);
    }

    public void setMultiBlockLevel(ItemStack tool, int level) {
        if (tool == null || tool.getType().isAir()) return;
        
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;

        String id = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (id == null) {
            id = UUID.randomUUID().toString();
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
        }

        multiBlockLevels.put(id, level);
        saveData();

        updateLore(tool, level);
    }

    public void removeMultiBlockLevel(ItemStack tool) {
        if (tool == null || tool.getType().isAir() || !tool.hasItemMeta()) return;
        
        ItemMeta meta = tool.getItemMeta();
        String id = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        
        if (id != null) {
            multiBlockLevels.remove(id);
            meta.getPersistentDataContainer().remove(itemKey);
            tool.setItemMeta(meta);
            saveData();
            
            updateLore(tool, 1);
        }
    }

    private void updateLore(ItemStack tool, int level) {
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;

        // Lấy toàn bộ lore hiện tại
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        // Xóa các dòng lore MultiBlock cũ (nếu có)
        lore.removeIf(line -> line.contains("MultiBlock"));
        
        // Xóa các dòng trống thừa ở cuối
        while (!lore.isEmpty() && lore.get(lore.size() - 1).trim().isEmpty()) {
            lore.remove(lore.size() - 1);
        }

        // Thêm lore mới nếu level > 1
        if (level > 1) {
            if (!lore.isEmpty()) lore.add(""); // Thêm dòng trống phân cách nếu đã có lore khác
            lore.add(ChatColor.GOLD + "✦ MultiBlock: " + ChatColor.YELLOW + level + "x");
            lore.add(ChatColor.GRAY + "Đào block nhận x" + level + " vật phẩm");
        }

        meta.setLore(lore);
        tool.setItemMeta(meta);
    }
}