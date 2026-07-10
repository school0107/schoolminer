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
    private final Map<UUID, CraftTask> tasks = new HashMap<>();
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
            player.sendMessage("§c❌ Không tìm thấy cấu hình autocraft cho " + craftType + "!");
            return;
        }
        
        if (tasks.containsKey(uuid)) {
            player.sendMessage("§c⚠️ Bạn đang craft, hãy tắt craft hiện tại trước!");
            return;
        }
        
        CraftTask task = new CraftTask(player, craftConfig);
        int craftDelay = config.getCraftDelay();
        task.runTaskTimer(plugin, 0L, craftDelay);
        tasks.put(uuid, task);
        
        player.sendMessage("§a✅ Đã bật AutoCraft: " + craftConfig.getDisplayName() + "!");
    }

    public void stopCraft(Player player) {
        UUID uuid = player.getUniqueId();
        CraftTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
            player.sendMessage("§c⛔ Đã tắt AutoCraft!");
        }
    }

    public void stopAll() {
        for (CraftTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }

    public boolean isCrafting(Player player) {
        return tasks.containsKey(player.getUniqueId());
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

        public CraftTask(Player player, AutoCraftConfig craftConfig) {
            this.player = player;
            this.craftConfig = craftConfig;
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) {
                stopCraft(player);
                return;
            }

            PlayerInventory inventory = player.getInventory();
            
            // TÍNH SỐ LẦN CRAFT TỐI ĐA DỰA TRÊN NGUYÊN LIỆU CÓ TRONG TÚI
            int maxCrafts = Integer.MAX_VALUE;
            Map<ItemStack, Integer> materialCounts = new HashMap<>();
            
            // Đếm số lượng nguyên liệu có trong túi
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
                
                // Tính số lần craft có thể làm dựa trên nguyên liệu này
                int possibleCrafts = totalFound / needed;
                if (possibleCrafts < maxCrafts) {
                    maxCrafts = possibleCrafts;
                }
                
                // Lưu lại số lượng cần lấy
                materialCounts.put(material, needed);
            }
            
            // Nếu không đủ nguyên liệu
            if (maxCrafts <= 0) {
                if (craftedCount > 0) {
                    player.sendMessage("§e⚠️ Đã hết nguyên liệu! Đã craft §a" + craftedCount + " §elần!");
                }
                // KHÔNG TỰ ĐỘNG TẮT, chỉ thông báo và đợi nguyên liệu mới
                craftedCount = 0;
                return;
            }
            
            // Kiểm tra túi đồ còn chỗ không
            int emptySlots = 0;
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                    emptySlots++;
                }
            }
            
            // Tính số lần craft dựa trên số chỗ trống trong túi
            int craftLimit = maxCrafts;
            
            // Nếu túi đầy, chỉ craft 1 lần
            if (emptySlots == 0) {
                player.sendMessage("§c⚠️ Túi đồ đầy! Không thể craft thêm!");
                return;
            }
            
            // Craft nhiều lần cùng lúc
            int actualCrafts = Math.min(craftLimit, emptySlots);
            
            // Xóa nguyên liệu
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
            
            // Tạo sản phẩm
            ItemStack result = craftConfig.getResult().clone();
            
            if (craftConfig.isGlow()) {
                ItemMeta meta = result.getItemMeta();
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                result.setItemMeta(meta);
            }
            
            // Thêm sản phẩm vào túi
            for (int i = 0; i < actualCrafts; i++) {
                if (inventory.firstEmpty() != -1) {
                    inventory.addItem(result.clone());
                    craftedCount++;
                }
            }
            
            // Hiệu ứng
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1);
            player.getWorld().spawnParticle(Particle.ENCHANT, 
                player.getLocation().add(0, 1, 0), 10 * actualCrafts, 0.3, 0.3, 0.3);
            
            // Thông báo số lượng đã craft
            if (actualCrafts > 0) {
                player.sendMessage("§a✅ Đã craft §e" + actualCrafts + " §ax §f" + craftConfig.getDisplayName());
                player.sendMessage("§7Tổng đã craft: §a" + craftedCount + " §7lần");
            }
        }
    }
}