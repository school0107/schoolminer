package com.schoolminer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
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

            Block target = player.getTargetBlockExact(range);
            if (target == null) return;
            
            if (!config.isWhitelisted(target.getType())) return;
            
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool.getType().isAir()) return;
            if (!tool.getType().name().contains("PICKAXE") && 
                !tool.getType().name().contains("AXE") && 
                !tool.getType().name().contains("SHOVEL")) return;

            int efficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
            float speed = 1.0f + (efficiency * 0.3f);
            
            target.breakNaturally(tool, true);
            
            player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getType());
            // Sửa dòng này
            player.getWorld().spawnParticle(Particle.BLOCK, 
                target.getLocation().add(0.5, 0.5, 0.5), 10, 
                target.getBlockData());
        }
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
