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
    private Map<String, AutoCraftConfig> craftConfigs;

    public ConfigManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.craftConfigs = new HashMap<>();
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
        loadCrafts(config);
        
        plugin.getLogger().info("§a✅ Đã load " + whitelist.size() + " block vào whitelist");
    }

    private void loadCrafts(FileConfiguration config) {
        craftConfigs.clear();
        ConfigurationSection crafts = config.getConfigurationSection("autocraft.crafts");
        if (crafts == null) return;
        
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

    public AutoCraftConfig getCraftConfig(String id) {
        return craftConfigs.get(id);
    }

    public Set<String> getCraftTypes() {
        return craftConfigs.keySet();
    }

    public String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "&c⚠️ Không tìm thấy message: " + key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}