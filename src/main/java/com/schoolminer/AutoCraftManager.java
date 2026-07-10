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
        
        // Kiểm tra permission
        String permission = "schoolminer.autocraft." + craftType;
        if (!player.hasPermission(permission)) {
            player.sendMessage("§c❌ Bạn không có quyền sử dụng autocraft " + craftType + "!");
            return;
        }
        
        // Kiểm tra config
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
        task.runTaskTimer(plugin, 0L, 20L); // Mỗi giây craft 1 lần
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
            
            // Kiểm tra đủ nguyên liệu
            boolean hasMaterials = true;
            Map<Integer, Integer> materialSlots = new HashMap<>();
            
            for (ItemStack material : craftConfig.getMaterials()) {
                if (material == null || material.getType().isAir()) continue;
                
                int needed = material.getAmount();
                int found = 0;
                
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item != null && item.isSimilar(material)) {
                        found += item.getAmount();
                        if (found >= needed) {
                            materialSlots.put(i, needed);
                            break;
                        }
                    }
                }
                
                if (found < needed) {
                    hasMaterials = false;
                    break;
                }
            }
            
            if (!hasMaterials) {
                // Không đủ nguyên liệu
                return;
            }
            
            // Xóa nguyên liệu
            for (Map.Entry<Integer, Integer> entry : materialSlots.entrySet()) {
                int slot = entry.getKey();
                int amount = entry.getValue();
                ItemStack item = inventory.getItem(slot);
                if (item != null) {
                    if (item.getAmount() <= amount) {
                        inventory.setItem(slot, null);
                    } else {
                        item.setAmount(item.getAmount() - amount);
                    }
                }
            }
            
            // Tạo sản phẩm
            ItemStack result = craftConfig.getResult().clone();
            
            // Thêm glow nếu có
            if (craftConfig.isGlow()) {
                ItemMeta meta = result.getItemMeta();
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                result.setItemMeta(meta);
            }
            
            // Thêm vào túi
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(result);
                // Hiệu ứng
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                player.getWorld().spawnParticle(Particle.ENCHANT, 
                    player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
            } else {
                player.sendMessage("§c⚠️ Túi đồ đầy! Không thể craft!");
                stopCraft(player);
            }
        }
    }
}
