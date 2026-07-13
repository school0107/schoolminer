package com.schoolminer;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    private boolean autoPickup;
    private boolean doubleDrop;
    private int craftDelay;
    private int craftCooldown;
    private int maxCraftPerTick;
    private Map<String, AutoCraftConfig> craftConfigs;
    
    private int maxExplosionLevel;
    private Map<Integer, Double> explosionChances;
    private Map<Integer, Double> explosionRadii;
    private Map<Integer, Double> upgradeCosts;
    
    // Multi block cho cúp (lưu theo UUID của item)
    private Map<String, Integer> multiBlockLevels = new HashMap<>();
    
    // Block cứng (không thể đào nhanh hơn)
    private Set<Material> hardBlocks = new HashSet<>();
    
    // Custom hardness cho từng block
    private Map<Material, Double> customHardness = new HashMap<>();

    public ConfigManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.craftConfigs = new HashMap<>();
        this.explosionChances = new HashMap<>();
        this.explosionRadii = new HashMap<>();
        this.upgradeCosts = new HashMap<>();
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
        autoPickup = config.getBoolean("automine.auto-pickup", true);
        doubleDrop = config.getBoolean("automine.double-drop", true);
        
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
        
        // Auto Craft
        craftDelay = config.getInt("autocraft.craft-delay", 20);
        craftCooldown = config.getInt("autocraft.cooldown", 2000);
        maxCraftPerTick = config.getInt("autocraft.max-craft-per-tick", 16);
        loadCrafts(config);
        
        // Explosion upgrades
        loadExplosionUpgrades(config);
        
        // Load multi block
        loadMultiBlock(config);
        
        // Load hard blocks
        loadHardBlocks(config);
        
        // Load custom hardness
        loadCustomHardness(config);
        
        plugin.getLogger().info("§a✅ Đã load " + whitelist.size() + " block vào whitelist");
        plugin.getLogger().info("§a✅ Đã load " + craftConfigs.size() + " công thức craft");
        plugin.getLogger().info("§a✅ Đã load " + maxExplosionLevel + " cấp nâng cấp AutoKill");
        plugin.getLogger().info("§a✅ Đã load " + multiBlockLevels.size() + " cấp MultiBlock");
        plugin.getLogger().info("§a✅ Đã load " + hardBlocks.size() + " block cứng");
        plugin.getLogger().info("§a✅ Đã load " + customHardness.size() + " block tùy chỉnh độ bền");
    }

    private void loadMultiBlock(FileConfiguration config) {
        multiBlockLevels.clear();
        ConfigurationSection multiBlockSection = config.getConfigurationSection("multi-block-tools");
        if (multiBlockSection == null) {
            plugin.getLogger().info("§e⚠️ Không tìm thấy multi-block-tools trong config!");
            return;
        }
        
        for (String toolKey : multiBlockSection.getKeys(false)) {
            int level = multiBlockSection.getInt(toolKey, 1);
            multiBlockLevels.put(toolKey, level);
            plugin.getLogger().info("§a✅ Load MultiBlock: " + toolKey + " = " + level + "x");
        }
    }

    private void loadHardBlocks(FileConfiguration config) {
        hardBlocks.clear();
        List<String> hardBlockNames = config.getStringList("hard-blocks");
        if (hardBlockNames.isEmpty()) {
            plugin.getLogger().info("§e⚠️ Không tìm thấy hard-blocks trong config!");
            return;
        }
        
        for (String name : hardBlockNames) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                hardBlocks.add(mat);
                plugin.getLogger().info("§a✅ Load hard block: " + name);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("§c⚠️ Hard block không hợp lệ: " + name);
            }
        }
    }

    private void loadCustomHardness(FileConfiguration config) {
        customHardness.clear();
        ConfigurationSection hardnessSection = config.getConfigurationSection("custom-hardness");
        if (hardnessSection == null) {
            plugin.getLogger().info("§e⚠️ Không tìm thấy custom-hardness trong config!");
            return;
        }
        
        for (String key : hardnessSection.getKeys(false)) {
            try {
                Material mat = Material.valueOf(key.toUpperCase());
                double hardness = hardnessSection.getDouble(key, 1.0);
                customHardness.put(mat, hardness);
                plugin.getLogger().info("§a✅ Load custom hardness: " + key + " = " + hardness);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("§c⚠️ Custom hardness không hợp lệ: " + key);
            }
        }
    }

    public void saveMultiBlock(String toolKey, int level) {
        multiBlockLevels.put(toolKey, level);
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection multiBlockSection = config.createSection("multi-block-tools");
        for (Map.Entry<String, Integer> entry : multiBlockLevels.entrySet()) {
            multiBlockSection.set(entry.getKey(), entry.getValue());
        }
        plugin.saveConfig();
        plugin.getLogger().info("§a✅ Đã lưu MultiBlock: " + toolKey + " = " + level + "x");
    }

    public int getMultiBlockLevel(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return 1;
        
        String toolKey = getItemKey(tool);
        int level = multiBlockLevels.getOrDefault(toolKey, 1);
        
        if (level > 1) {
            plugin.getLogger().info("§a[MultiBlock] " + tool.getType().name() + " [" + toolKey + "] = " + level + "x");
        }
        
        return level;
    }

    public void setMultiBlockLevel(ItemStack tool, int level) {
        if (tool == null || tool.getType().isAir()) return;
        
        String toolKey = getItemKey(tool);
        saveMultiBlock(toolKey, level);
        updateToolLore(tool, level);
    }

    public String getItemKey(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return "";
        
        String baseKey = tool.getType().name();
        
        if (tool.hasItemMeta()) {
            ItemMeta meta = tool.getItemMeta();
            if (meta.hasDisplayName()) {
                baseKey += "_" + meta.getDisplayName().hashCode();
            }
            if (meta.hasLore()) {
                baseKey += "_" + meta.getLore().hashCode();
            }
        }
        
        if (!multiBlockLevels.containsKey(baseKey)) {
            baseKey += "_" + System.currentTimeMillis();
        }
        
        return baseKey;
    }

    private void updateToolLore(ItemStack tool, int level) {
        if (tool == null || tool.getType().isAir()) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        lore.removeIf(line -> line.contains("MultiBlock"));
        lore.removeIf(line -> line.trim().isEmpty() && !lore.isEmpty() && lore.get(lore.size() - 1).trim().isEmpty());
        
        if (level > 1) {
            if (!lore.isEmpty() && !lore.get(lore.size() - 1).trim().isEmpty()) {
                lore.add("");
            }
            lore.add("§6✦ MultiBlock: §e" + level + "x");
            lore.add("§7Nhân vật phẩm khi đào lên §e" + level + "§7 lần");
        }
        
        meta.setLore(lore);
        tool.setItemMeta(meta);
        plugin.getLogger().info("§a✅ Đã cập nhật lore cho " + tool.getType().name());
    }

    public boolean isHardBlock(Material material) {
        return hardBlocks.contains(material);
    }

    public double getBlockHardness(Material material) {
        // Kiểm tra custom hardness trước
        if (customHardness.containsKey(material)) {
            return customHardness.get(material);
        }
        // Nếu không có, lấy hardness mặc định của block
        return material.getHardness();
    }

    private void loadExplosionUpgrades(FileConfiguration config) {
        explosionChances.clear();
        explosionRadii.clear();
        upgradeCosts.clear();
        
        ConfigurationSection upgrades = config.getConfigurationSection("autokill.upgrades");
        if (upgrades == null) {
            maxExplosionLevel = 10;
            for (int i = 0; i <= maxExplosionLevel; i++) {
                explosionChances.put(i, 0.0 + (i * 0.05));
                explosionRadii.put(i, 0.0 + (i * 0.5));
                upgradeCosts.put(i, i * 1000.0);
            }
            plugin.getLogger().info("§e⚠️ Không tìm thấy autokill.upgrades, dùng giá trị mặc định!");
            return;
        }
        
        maxExplosionLevel = upgrades.getInt("max-level", 10);
        
        ConfigurationSection levels = upgrades.getConfigurationSection("levels");
        if (levels != null) {
            for (String key : levels.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    ConfigurationSection levelData = levels.getConfigurationSection(key);
                    if (levelData != null) {
                        explosionChances.put(level, levelData.getDouble("chance", 0.0));
                        explosionRadii.put(level, levelData.getDouble("radius", 0.0));
                        upgradeCosts.put(level, levelData.getDouble("cost", 0.0));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        for (int i = 0; i <= maxExplosionLevel; i++) {
            if (!explosionChances.containsKey(i)) {
                explosionChances.put(i, 0.0 + (i * 0.05));
            }
            if (!explosionRadii.containsKey(i)) {
                explosionRadii.put(i, 0.0 + (i * 0.5));
            }
            if (!upgradeCosts.containsKey(i)) {
                upgradeCosts.put(i, i * 1000.0);
            }
        }
        
        plugin.getLogger().info("§a✅ Đã load " + maxExplosionLevel + " cấp nâng cấp AutoKill");
    }

    private void loadCrafts(FileConfiguration config) {
        craftConfigs.clear();
        ConfigurationSection crafts = config.getConfigurationSection("autocraft.crafts");
        if (crafts == null) {
            plugin.getLogger().info("§e⚠️ Không tìm thấy autocraft.crafts trong config!");
            return;
        }
        
        for (String craftId : crafts.getKeys(false)) {
            ConfigurationSection craft = crafts.getConfigurationSection(craftId);
            if (craft == null) continue;
            
            String displayName = craft.getString("display-name", craftId);
            boolean glow = craft.getBoolean("glow", false);
            
            List<ItemStack> materials = new ArrayList<>();
            ConfigurationSection materialsSection = craft.getConfigurationSection("materials");
            if (materialsSection != null) {
                for (String key : materialsSection.getKeys(false)) {
                    String matName = materialsSection.getString(key + ".material");
                    int amount = materialsSection.getInt(key + ".amount", 1);
                    
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase());
                        materials.add(new ItemStack(mat, amount));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("§c⚠️ Material không hợp lệ trong autocraft." + craftId + ": " + matName);
                    }
                }
            }
            
            ItemStack result = null;
            ConfigurationSection resultSection = craft.getConfigurationSection("result");
            if (resultSection != null) {
                String matName = resultSection.getString("material");
                int amount = resultSection.getInt("amount", 1);
                String displayNameResult = resultSection.getString("display-name", null);
                List<String> lore = resultSection.getStringList("lore");
                
                try {
                    Material mat = Material.valueOf(matName.toUpperCase());
                    result = new ItemStack(mat, amount);
                    
                    if (displayNameResult != null || !lore.isEmpty()) {
                        ItemMeta meta = result.getItemMeta();
                        if (displayNameResult != null) {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayNameResult));
                        }
                        if (!lore.isEmpty()) {
                            List<String> coloredLore = new ArrayList<>();
                            for (String line : lore) {
                                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                            }
                            meta.setLore(coloredLore);
                        }
                        result.setItemMeta(meta);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("§c⚠️ Material không hợp lệ trong autocraft." + craftId + ".result: " + matName);
                }
            }
            
            if (materials.isEmpty() || result == null) {
                plugin.getLogger().warning("§c⚠️ AutoCraft " + craftId + " không có nguyên liệu hoặc sản phẩm!");
                continue;
            }
            
            craftConfigs.put(craftId, new AutoCraftConfig(craftId, displayName, materials, result, glow));
            plugin.getLogger().info("§a✅ Đã load AutoCraft: " + craftId);
        }
    }

    public int getWhitelistCount() {
        return whitelist.size();
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

    public boolean isAutoPickup() {
        return autoPickup;
    }

    public boolean isDoubleDrop() {
        return doubleDrop;
    }

    public int getCraftDelay() {
        return craftDelay;
    }

    public int getCraftCooldown() {
        return craftCooldown;
    }

    public int getMaxCraftPerTick() {
        return maxCraftPerTick;
    }

    public AutoCraftConfig getCraftConfig(String id) {
        return craftConfigs.get(id);
    }

    public Set<String> getCraftTypes() {
        return craftConfigs.keySet();
    }

    public int getMaxExplosionLevel() {
        return maxExplosionLevel;
    }

    public double getExplosionChanceAtLevel(int level) {
        return explosionChances.getOrDefault(level, 0.0);
    }

    public double getExplosionRadiusAtLevel(int level) {
        return explosionRadii.getOrDefault(level, 0.0);
    }

    public double getUpgradeCost(int level) {
        return upgradeCosts.getOrDefault(level, 0.0);
    }

    public String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "&c⚠️ Không tìm thấy message: " + key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}