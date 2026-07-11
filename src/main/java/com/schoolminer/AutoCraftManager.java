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
    private final Map<UUID, Long> cooldownMap = new HashMap<>();

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
        
        // KIỂM TRA COOLDOWN - CHỐNG SPAM
        long now = System.currentTimeMillis();
        long cooldown = config.getCraftCooldown();
        if (cooldownMap.containsKey(uuid) && now - cooldownMap.get(uuid) < cooldown) {
            long remaining = (cooldown - (now - cooldownMap.get(uuid))) / 1000;
            player.sendMessage("§c⚠️ Vui lòng đợi §e" + remaining + " §cgiây trước khi bật craft mới!");
            return;
        }
        
        // KHÔNG GIỚI HẠN SỐ LƯỢNG CRAFT - Cho phép nhiều craft cùng lúc
        CraftTask task = new CraftTask(player, craftConfig);
        int craftDelay = config.getCraftDelay();
        task.runTaskTimer(plugin, 0L, craftDelay);
        
        // Lưu task
        tasks.computeIfAbsent(uuid, k -> new HashMap<>()).put(craftType, task);
        playerCrafts.computeIfAbsent(uuid, k -> new HashSet<>()).add(craftType);
        
        cooldownMap.put(uuid, now);
        
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
            player.sendMessage("§c⛔ Đã tắt AutoCraft: " + craftType);
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
        cooldownMap.clear();
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
        private int failCount = 0;

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
                failCount++;
                // Nếu fail 5 lần liên tiếp thì thông báo và dừng
                if (failCount >= 5) {
                    if (craftedCount > 0) {
                        player.sendMessage("§e⚠️ Đã hết nguyên liệu! Đã craft §a" + craftedCount + " §e" + craftConfig.getDisplayName());
                    }
                    stopCraft(player, craftConfig.getId());
                }
                return;
            }
            
            failCount = 0;
            
            int emptySlots = 0;
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                    emptySlots++;
                }
            }
            
            if (emptySlots == 0) {
                player.sendMessage("§c⚠️ Túi đồ đầy! Dừng craft " + craftConfig.getDisplayName());
                stopCraft(player, craftConfig.getId());
                return;
            }
            
            // GIỚI HẠN SỐ LƯỢNG CRAFT MỖI LẦN ĐỂ TRÁNH LAG
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
            
            // Thêm sản phẩm vào túi (giới hạn số lượng mỗi lần để tránh lag)
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
            
            // Hiệu ứng - chỉ phát khi có craft
            if (addedCount > 0) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1);
                player.getWorld().spawnParticle(Particle.ENCHANT, 
                    player.getLocation().add(0, 1, 0), Math.min(addedCount * 2, 20), 0.3, 0.3, 0.3);
            }
        }
    }
}