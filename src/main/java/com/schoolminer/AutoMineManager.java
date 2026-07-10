package com.schoolminer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Location;
import org.bukkit.inventory.PlayerInventory;
import java.util.*;

public class AutoMineManager {
    private final Schoolminer plugin;
    private final Map<UUID, MineTask> tasks = new HashMap<>();
    private final ConfigManager config;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Random random = new Random();

    public AutoMineManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void startMining(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) return;
        
        lastLocations.put(uuid, player.getLocation().clone());
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
            try {
                Block target = player.getTargetBlockExact(5);
                if (target != null && target.getLocation() != null) {
                    player.sendBlockDamage(target.getLocation(), 0.0f);
                }
            } catch (Exception ignored) {}
            player.sendMessage(config.getMessage("disabled"));
        }
        lastLocations.remove(uuid);
    }

    public void stopAll() {
        for (MineTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        lastLocations.clear();
    }

    public boolean isMining(Player player) {
        return tasks.containsKey(player.getUniqueId());
    }

    private class MineTask extends BukkitRunnable {
        private final Player player;
        private final int range;
        private Block currentBlock = null;
        private int breakTicks = 0;
        private int requiredTicks;
        private boolean isBreaking = false;
        private final UUID playerUUID;

        public MineTask(Player player) {
            this.player = player;
            this.playerUUID = player.getUniqueId();
            this.range = getRange(player);
            this.requiredTicks = config.getMineDelay();
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) {
                stopMining(player);
                return;
            }

            Location lastLoc = lastLocations.get(playerUUID);
            if (lastLoc != null) {
                Location currentLoc = player.getLocation();
                if (lastLoc.distance(currentLoc) > 0.1) {
                    player.sendMessage("§c⚠️ Bạn đã di chuyển, Auto Mine đã tắt!");
                    stopMining(player);
                    return;
                }
            }

            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool.getType().isAir()) {
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
            }

            if (!isBreaking) {
                isBreaking = true;
                try {
                    player.sendBlockDamage(target.getLocation(), 1.0f);
                } catch (Exception ignored) {}
            }

            breakTicks++;

            float progress = Math.min((float) breakTicks / requiredTicks, 1.0f);
            
            int stage = (int) (progress * 9);
            if (stage >= 0 && stage <= 9) {
                try {
                    player.sendBlockDamage(target.getLocation(), stage / 9.0f);
                } catch (Exception ignored) {}
            }

            if (breakTicks >= requiredTicks) {
                Collection<ItemStack> drops = target.getDrops(tool);
                
                int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
                if (fortune > 0 && isFortuneable(target.getType())) {
                    for (ItemStack drop : drops) {
                        int bonus = getFortuneBonus(fortune);
                        drop.setAmount(drop.getAmount() * (1 + bonus));
                    }
                }
                
                // X2 ITEM
                boolean doubleDrop = config.isDoubleDrop();
                PlayerInventory inventory = player.getInventory();
                
                for (ItemStack drop : drops) {
                    if (drop != null && !drop.getType().isAir()) {
                        int amount = drop.getAmount();
                        
                        // X2 số lượng
                        if (doubleDrop) {
                            amount *= 2;
                        }
                        
                        // Thêm vào túi
                        ItemStack finalDrop = drop.clone();
                        finalDrop.setAmount(amount);
                        
                        if (inventory.firstEmpty() != -1) {
                            inventory.addItem(finalDrop);
                        } else {
                            Location loc = target.getLocation().add(0.5, 0.5, 0.5);
                            org.bukkit.entity.Item item = player.getWorld().dropItem(loc, finalDrop);
                            item.setPickupDelay(0);
                        }
                    }
                }
                
                breakTicks = 0;
                isBreaking = false;
                
                player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getType());
                player.getWorld().spawnParticle(Particle.BLOCK,
                    target.getLocation().add(0.5, 0.5, 0.5), 5,
                    target.getBlockData());
                
                try {
                    player.sendBlockDamage(target.getLocation(), 1.0f);
                } catch (Exception ignored) {}
            }
        }

        private boolean isFortuneable(Material material) {
            String name = material.name();
            return name.contains("ORE") || name.contains("COAL") || 
                   name.contains("DIAMOND") || name.contains("EMERALD") ||
                   name.contains("LAPIS") || name.contains("REDSTONE") ||
                   name.contains("QUARTZ") || name.contains("NETHER_GOLD") ||
                   name.contains("AMETHYST");
        }

        private int getFortuneBonus(int fortuneLevel) {
            int bonus = 0;
            for (int i = 0; i < fortuneLevel; i++) {
                if (random.nextDouble() < 0.33) {
                    bonus++;
                }
            }
            return bonus;
        }

        private void resetBreak() {
            if (currentBlock != null && currentBlock.getLocation() != null) {
                try {
                    player.sendBlockDamage(currentBlock.getLocation(), 0.0f);
                } catch (Exception ignored) {}
            }
            currentBlock = null;
            breakTicks = 0;
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
