package com.schoolminer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.PlayerInventory;
import java.util.*;

public class AutoMineManager {
    private final Schoolminer plugin;
    private final Map<UUID, MineTask> tasks = new HashMap<>();
    private final ConfigManager config;

    public AutoMineManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void startMining(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) return;
        
        MineTask task = new MineTask(player);
        task.runTaskTimer(plugin, 0L, config.getMineDelay());
        tasks.put(uuid, task);
        
        player.sendMessage(config.getMessage("enabled"));
    }

    public void stopMining(Player player) {
        UUID uuid = player.getUniqueId();
        MineTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
            player.sendMessage(config.getMessage("disabled"));
        }
    }

    public void stopAll() {
        for (MineTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }

    public boolean isMining(Player player) {
        return tasks.containsKey(player.getUniqueId());
    }

    private class MineTask extends BukkitRunnable {
        private final Player player;
        private final int range;
        private boolean isBreaking = false;
        private Block currentBlock = null;
        private int breakProgress = 0;

        public MineTask(Player player) {
            this.player = player;
            this.range = getRange(player);
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) {
                stopMining(player);
                return;
            }

            // Kiểm tra công cụ trên tay
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool.getType().isAir()) return;
            
            // Chỉ cho phép các công cụ đào
            String toolName = tool.getType().name();
            if (!toolName.contains("PICKAXE") && 
                !toolName.contains("AXE") && 
                !toolName.contains("SHOVEL") &&
                !toolName.contains("HOE")) return;

            // Lấy block đang nhìn
            Block target = player.getTargetBlockExact(range);
            if (target == null) {
                resetBreak();
                return;
            }

            // Kiểm tra whitelist
            if (!config.isWhitelisted(target.getType())) {
                resetBreak();
                return;
            }

            // Kiểm tra khoảng cách
            if (target.getLocation().distance(player.getLocation()) > range) {
                resetBreak();
                return;
            }

            // Nếu block khác với block đang đào, reset
            if (currentBlock == null || !currentBlock.equals(target)) {
                currentBlock = target;
                breakProgress = 0;
                isBreaking = false;
            }

            // Mô phỏng click chuột trái
            if (!isBreaking) {
                isBreaking = true;
                // Gửi gói tin bắt đầu đào
                player.sendBlockDamage(target.getLocation(), 1.0f);
            }

            // Tính tốc độ đào dựa trên công cụ và enchant
            float breakSpeed = calculateBreakSpeed(tool, target);
            
            // Tăng tiến độ đào
            breakProgress += breakSpeed * 2;

            // Hiển thị tiến độ (vết nứt trên block)
            int stage = Math.min((int)(breakProgress / 100), 9);
            if (stage >= 0 && stage <= 9) {
                player.sendBlockDamage(target.getLocation(), stage / 9.0f);
            }

            // Khi block bị phá
            if (breakProgress >= 100) {
                // Break block và drop items như vanilla
                target.breakNaturally(tool, true);
                
                // Reset state
                resetBreak();
                
                // Hiệu ứng
                player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getType());
                player.getWorld().spawnParticle(Particle.BLOCK,
                    target.getLocation().add(0.5, 0.5, 0.5), 20,
                    target.getBlockData());
            }
        }

        private float calculateBreakSpeed(ItemStack tool, Block block) {
            float speed = 1.0f;
            
            // Base speed theo loại công cụ
            String toolName = tool.getType().name();
            if (toolName.contains("PICKAXE")) {
                speed = 1.0f;
            } else if (toolName.contains("SHOVEL")) {
                speed = 0.5f;
            } else if (toolName.contains("AXE")) {
                speed = 0.7f;
            } else if (toolName.contains("HOE")) {
                speed = 0.3f;
            }

            // Enchant Efficiency
            int efficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
            speed += efficiency * 0.8f;

            // Enchant Haste
            if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.HASTE)) {
                speed *= 1.5f;
            }

            // Độ cứng của block
            float hardness = block.getType().getHardness();
            if (hardness > 0) {
                speed = speed / (hardness * 1.5f);
            }

            // Giới hạn tốc độ tối thiểu
            speed = Math.max(0.1f, speed);

            return speed;
        }

        private void resetBreak() {
            if (currentBlock != null) {
                player.sendBlockDamage(currentBlock.getLocation(), 0.0f);
            }
            currentBlock = null;
            breakProgress = 0;
            isBreaking = false;
        }

        private int getRange(Player player) {
            for (int i = config.getMaxRange(); i >= 1; i--) {
                if (player.hasPermission("schoolminer.range." + i)) {
                    return i;
                }
            }
            return 1;
        }
    }
}
