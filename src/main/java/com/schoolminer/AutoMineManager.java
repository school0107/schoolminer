package com.schoolminer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
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
        task.runTaskTimer(plugin, 0L, 1L);
        tasks.put(uuid, task);
        
        player.sendMessage(config.getMessage("enabled"));
    }

    public void stopMining(Player player) {
        UUID uuid = player.getUniqueId();
        MineTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
            player.sendBlockDamage(player.getTargetBlockExact(5) != null ? 
                player.getTargetBlockExact(5).getLocation() : null, 0.0f);
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
        private Block currentBlock = null;
        private int breakTicks = 0;
        private int requiredTicks = 0;
        private boolean isBreaking = false;

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

            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool.getType().isAir()) {
                resetBreak();
                return;
            }
            
            String toolName = tool.getType().name();
            if (!toolName.contains("PICKAXE") && 
                !toolName.contains("AXE") && 
                !toolName.contains("SHOVEL") &&
                !toolName.contains("HOE")) {
                resetBreak();
                return;
            }

            Block target = player.getTargetBlockExact(range);
            if (target == null) {
                resetBreak();
                return;
            }

            if (!config.isWhitelisted(target.getType())) {
                resetBreak();
                return;
            }

            if (target.getLocation().distance(player.getLocation()) > range) {
                resetBreak();
                return;
            }

            if (currentBlock == null || !currentBlock.equals(target)) {
                currentBlock = target;
                breakTicks = 0;
                isBreaking = false;
                requiredTicks = calculateRequiredTicks(tool, target);
            }

            if (!isBreaking) {
                isBreaking = true;
                player.sendBlockDamage(target.getLocation(), 1.0f);
            }

            breakTicks++;

            float progress = Math.min((float) breakTicks / requiredTicks, 1.0f);
            
            int stage = (int) (progress * 9);
            if (stage >= 0 && stage <= 9) {
                player.sendBlockDamage(target.getLocation(), stage / 9.0f);
            }

            if (breakTicks >= requiredTicks) {
                target.breakNaturally(tool, true);
                resetBreak();
                
                player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getType());
                player.getWorld().spawnParticle(Particle.BLOCK,
                    target.getLocation().add(0.5, 0.5, 0.5), 15,
                    target.getBlockData());
            }
        }

        private int calculateRequiredTicks(ItemStack tool, Block block) {
            float hardness = block.getType().getHardness();
            if (hardness < 0) return 1;
            
            // Base speed
            float speed = 1.0f;
            
            // Tool speed multiplier
            String toolName = tool.getType().name();
            float toolMultiplier = 1.0f;
            
            if (toolName.contains("PICKAXE")) {
                toolMultiplier = 1.0f;
            } else if (toolName.contains("SHOVEL")) {
                toolMultiplier = 0.5f;
            } else if (toolName.contains("AXE")) {
                toolMultiplier = 0.7f;
            } else if (toolName.contains("HOE")) {
                toolMultiplier = 0.3f;
            }

            // Material multiplier
            float materialMultiplier = 1.0f;
            String material = block.getType().name();
            if (material.contains("WOOD") || material.contains("LOG") || material.contains("PLANK")) {
                materialMultiplier = 2.0f;
            } else if (material.contains("STONE") || material.contains("DEEPSLATE") || 
                       material.contains("GRANITE") || material.contains("DIORITE") || 
                       material.contains("ANDESITE") || material.contains("TUFF")) {
                materialMultiplier = 1.5f;
            } else if (material.contains("IRON") || material.contains("GOLD") || 
                       material.contains("DIAMOND") || material.contains("EMERALD") ||
                       material.contains("REDSTONE") || material.contains("LAPIS") ||
                       material.contains("COAL")) {
                materialMultiplier = 1.0f;
            } else if (material.contains("NETHER") || material.contains("QUARTZ")) {
                materialMultiplier = 1.0f;
            } else if (material.contains("OBSIDIAN") || material.contains("CRYING_OBSIDIAN")) {
                materialMultiplier = 0.5f;
            } else if (material.contains("SAND") || material.contains("GRAVEL") || 
                       material.contains("DIRT") || material.contains("GRASS")) {
                materialMultiplier = 0.5f;
            }

            // Tool speed = toolMultiplier * materialMultiplier
            float toolSpeed = toolMultiplier * materialMultiplier;

            // Enchant Efficiency (hỗ trợ level cao > 40)
            int efficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
            float efficiencyBonus = 0f;
            if (efficiency > 0) {
                // Công thức chuẩn Minecraft: speed += (level^2 + 1)
                // Hỗ trợ level lên đến 40+
                efficiencyBonus = (float) (Math.pow(efficiency, 2) + 1);
                // Giới hạn để không bị quá nhanh (tối thiểu 0.5 ticks)
                efficiencyBonus = Math.min(efficiencyBonus, 2000f);
            }

            // Tổng speed
            speed = toolSpeed + efficiencyBonus;

            // Enchant Haste
            if (player.hasPotionEffect(PotionEffectType.HASTE)) {
                int level = player.getPotionEffect(PotionEffectType.HASTE).getAmplifier() + 1;
                speed *= (1 + (0.2 * level));
            }

            // Enchant Mining Fatigue
            if (player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
                int level = player.getPotionEffect(PotionEffectType.MINING_FATIGUE).getAmplifier() + 1;
                speed /= (1 + (0.3 * level));
            }

            // Aqua Affinity
            if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING) && player.isInWater()) {
                speed *= 1.5f;
            }

            // Công thức tính ticks
            // Ticks = (Hardness * 20) / Speed
            float ticks = (hardness * 20) / speed;
            
            // Giới hạn:
            // - Tối thiểu: 1 tick (0.05s) 
            // - Tối đa: 1200 ticks (60s)
            ticks = Math.max(1, Math.min(ticks, 1200));
            
            return (int) Math.ceil(ticks);
        }

        private void resetBreak() {
            if (currentBlock != null) {
                player.sendBlockDamage(currentBlock.getLocation(), 0.0f);
            }
            currentBlock = null;
            breakTicks = 0;
            requiredTicks = 0;
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
