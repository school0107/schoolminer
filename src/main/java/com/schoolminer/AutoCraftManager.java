package com.schoolminer;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class AutoCraftManager {
    private final Schoolminer plugin;
    private final Map<UUID, Set<String>> playerCrafts = new HashMap<>();
    private final Map<UUID, Map<String, CraftTask>> tasks = new HashMap<>();
    private final ConfigManager config;

    public AutoCraftManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void startCraft(Player player, String craftType) {
        UUID uuid = player.getUniqueId();
        
        String permission = "schoolminer.autocraft." + craftType;
        if (!player.hasPermission(permission)) {
            player.sendMessage("§c❌ Bạn không có quyền sử dụng autocraft " + craftType + "!");
            return;
        }
        
        AutoCraftConfig craftConfig = config.getCraftConfig(craftType);
        if (craftConfig == null) {
            player.sendMessage("§c❌ Không tìm thấy autocraft: " + craftType);
            return;
        }
        
        if (isCrafting(player, craftType)) {
            player.sendMessage("§e⚠️ " + craftConfig.getDisplayName() + " đã được bật rồi!");
            return;
        }
        
        CraftTask task = new CraftTask(player, craftConfig);
        int craftDelay = config.getCraftDelay();
        task.runTaskTimer(plugin, 0L, craftDelay);
        
        tasks.computeIfAbsent(uuid, k -> new HashMap<>()).put(craftType, task);
        playerCrafts.computeIfAbsent(uuid, k -> new HashSet<>()).add(craftType);
        
        player.sendMessage("§a✅ Đã bật AutoCraft: " + craftConfig.getDisplayName() + "!");
        player.sendMessage("§7Đang chạy: §e" + getActiveCrafts(player).size() + " §7craft");
    }

    public void stopCraft(Player player, String craftType) {
        UUID uuid = player.getUniqueId();
        
        Map<String, CraftTask> playerTasks = tasks.get(uuid);
        if (playerTasks == null) return;
        
        CraftTask task = playerTasks.remove(craftType);
        if (task != null) {
            task.cancel();
            Set<String> crafts = playerCrafts.get(uuid);
            if (crafts != null) {
                crafts.remove(craftType);
                if (crafts.isEmpty()) {
                    playerCrafts.remove(uuid);
                }
            }
            if (playerTasks.isEmpty()) {
                tasks.remove(uuid);
            }
            
            AutoCraftConfig craftConfig = config.getCraftConfig(craftType);
            String displayName = craftConfig != null ? craftConfig.getDisplayName() : craftType;
            player.sendMessage("§c⛔ Đã tắt AutoCraft: " + displayName);
        }
    }

    public void stopAllCraft(Player player) {
        UUID uuid = player.getUniqueId();
        
        Map<String, CraftTask> playerTasks = tasks.remove(uuid);
        if (playerTasks != null) {
            for (CraftTask task : playerTasks.values()) {
                task.cancel();
            }
            playerCrafts.remove(uuid);
            player.sendMessage("§c⛔ Đã tắt tất cả AutoCraft!");
        }
    }

    public void stopAll() {
        for (Map<String, CraftTask> playerTasks : tasks.values()) {
            for (CraftTask task : playerTasks.values()) {
                task.cancel();
            }
        }
        tasks.clear();
        playerCrafts.clear();
    }

    public boolean isCrafting(Player player, String craftType) {
        UUID uuid = player.getUniqueId();
        Map<String, CraftTask> playerTasks = tasks.get(uuid);
        return playerTasks != null && playerTasks.containsKey(craftType);
    }

    public Set<String> getActiveCrafts(Player player) {
        UUID uuid = player.getUniqueId();
        return playerCrafts.getOrDefault(uuid, new HashSet<>());
    }

    public List<String> getAvailableCrafts(Player player) {
        List<String> available = new ArrayList<>();
        for (String craftType : config.getCraftTypes()) {
            if (player.hasPermission("schoolminer.autocraft." + craftType)) {
                available.add(craftType);
            }
        }
        return available;
    }

    private class CraftTask extends BukkitRunnable {
        private final Player player;
        private final AutoCraftConfig craftConfig;
        private int craftedCount = 0;
        private boolean hasNotifiedEmpty = false;

        public CraftTask(Player player, AutoCraftConfig craftConfig) {
            this.player = player;
            this.craftConfig = craftConfig;
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) {
                stopCraft(player, craftConfig.getId());
                return;
            }

            PlayerInventory inventory = player.getInventory();
            
            int maxCrafts = Integer.MAX_VALUE;
            
            for (ItemStack material : craftConfig.getMaterials()) {
                if (material == null || material.getType().isAir()) continue;
                
                int needed = material.getAmount();
                int totalFound = 0;
                
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item != null && item.isSimilar(material)) {
                        totalFound += item.getAmount();
                    }
                }
                
                int possibleCrafts = totalFound / needed;
                if (possibleCrafts < maxCrafts) {
                    maxCrafts = possibleCrafts;
                }
            }
            
            if (maxCrafts <= 0) {
                if (!hasNotifiedEmpty && craftedCount > 0) {
                    player.sendMessage("§e⚠️ Đã hết nguyên liệu cho §f" + craftConfig.getDisplayName());
                    player.sendMessage("§7Đã craft §a" + craftedCount + " §7lần, thêm nguyên liệu để tiếp tục!");
                    hasNotifiedEmpty = true;
                }
                return;
            }
            
            hasNotifiedEmpty = false;
            
            int emptySlots = 0;
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                    emptySlots++;
                }
            }
            
            if (emptySlots == 0) {
                if (!hasNotifiedEmpty) {
                    player.sendMessage("§c⚠️ Túi đồ đầy! Dừng craft §f" + craftConfig.getDisplayName());
                    hasNotifiedEmpty = true;
                }
                return;
            }
            
            int maxCraftPerTick = config.getMaxCraftPerTick();
            int actualCrafts = Math.min(Math.min(maxCrafts, emptySlots), maxCraftPerTick);
            
            for (ItemStack material : craftConfig.getMaterials()) {
                if (material == null || material.getType().isAir()) continue;
                
                int needed = material.getAmount() * actualCrafts;
                int toRemove = needed;
                
                for (int i = 0; i < inventory.getSize() && toRemove > 0; i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item != null && item.isSimilar(material)) {
                        if (item.getAmount() <= toRemove) {
                            toRemove -= item.getAmount();
                            inventory.setItem(i, null);
                        } else {
                            item.setAmount(item.getAmount() - toRemove);
                            toRemove = 0;
                        }
                    }
                }
            }
            
            ItemStack result = craftConfig.getResult().clone();
            
            if (craftConfig.isGlow()) {
                ItemMeta meta = result.getItemMeta();
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                result.setItemMeta(meta);
            }
            
            int addedCount = 0;
            for (int i = 0; i < actualCrafts; i++) {
                if (inventory.firstEmpty() != -1) {
                    inventory.addItem(result.clone());
                    craftedCount++;
                    addedCount++;
                } else {
                    break;
                }
            }
            
            if (addedCount > 0) {
                // Chỉ play sound, không particle
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1);
                
                if (craftedCount % 10 == 0 && craftedCount > 0) {
                    player.sendMessage("§7Đã craft §a" + craftedCount + " §7" + craftConfig.getDisplayName());
                }
            }
        }
    }
}