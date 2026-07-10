package com.schoolminer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Location;
import java.util.*;

public class AutoMineManager {
    private final Schoolminer plugin;
    private final Map<UUID, MineTask> tasks = new HashMap<>();
    private final ConfigManager config;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

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
            Block target = player.getTargetBlockExact(5);
            if (target != null && target.getLocation() != null) {
                player.sendBlockDamage(target.getLocation(), 0.0f);
            }
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
        private int requiredTicks = 0;
        private boolean isBreaking = false;
        private final UUID playerUUID;

        public MineTask(Player player) {
            this.player = player;
            this.playerUUID = player.getUniqueId();
            this.range = getRange(player);
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
            
            String toolName = tool.getType().name();
            boolean isValidTool = toolName.contains("PICKAXE") || 
                                  toolName.contains("AXE") || 
                                  toolName.contains("SHOVEL") ||
                                  toolName.contains("HOE");
            
            if (!isValidTool) {
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
                Collection<ItemStack> drops = target.getDrops(tool);
                
                target.setType(Material.AIR);
                
                for (ItemStack drop : drops) {
                    int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
                    if (fortune > 0 && isFortuneable(target.getType())) {
                        int bonus = getFortuneBonus(fortune);
                        drop.setAmount(drop.getAmount() * (1 + bonus));
                    }
                    
                    Location loc = target.getLocation().add(0.5, 0.5, 0.5);
                    Item item = player.getWorld().dropItem(loc, drop);
                    item.setPickupDelay(0);
                    item.setVelocity(player.getLocation().toVector()
                        .subtract(item.getLocation().toVector())
                        .normalize().multiply(0.5));
                }
                
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
            
            String toolName = tool.getType().name();
            float toolMultiplier = 1.0f;
            
            if (toolName.contains("NETHERITE_PICKAXE")) toolMultiplier = 9.0f;
            else if (toolName.contains("DIAMOND_PICKAXE")) toolMultiplier = 8.0f;
            else if (toolName.contains("IRON_PICKAXE")) toolMultiplier = 6.0f;
            else if (toolName.contains("GOLDEN_PICKAXE") || toolName.contains("GOLD_PICKAXE")) toolMultiplier = 12.0f;
            else if (toolName.contains("STONE_PICKAXE")) toolMultiplier = 4.0f;
            else if (toolName.contains("WOODEN_PICKAXE") || toolName.contains("WOOD_PICKAXE")) toolMultiplier = 2.0f;
            else if (toolName.contains("NETHERITE_AXE")) toolMultiplier = 8.0f;
            else if (toolName.contains("DIAMOND_AXE")) toolMultiplier = 7.0f;
            else if (toolName.contains("IRON_AXE")) toolMultiplier = 5.0f;
            else if (toolName.contains("NETHERITE_SHOVEL")) toolMultiplier = 6.5f;
            else if (toolName.contains("DIAMOND_SHOVEL")) toolMultiplier = 5.5f;
            else if (toolName.contains("IRON_SHOVEL")) toolMultiplier = 4.0f;

            float materialMultiplier = 1.0f;
            String material = block.getType().name();
            if (material.contains("WOOD") || material.contains("LOG") || material.contains("PLANK")) {
                materialMultiplier = 2.0f;
            } else if (material.contains("STONE") || material.contains("DEEPSLATE") || 
                       material.contains("GRANITE") || material.contains("DIORITE") || 
                       material.contains("ANDESITE") || material.contains("TUFF")) {
                materialMultiplier = 1.5f;
            } else if (material.contains("OBSIDIAN") || material.contains("CRYING_OBSIDIAN")) {
                materialMultiplier = 0.5f;
            }

            float toolSpeed = toolMultiplier * materialMultiplier;

            int efficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
            float efficiencyBonus = 0f;
            if (efficiency > 0) {
                efficiencyBonus = (float) (Math.pow(efficiency, 2) + 1);
                efficiencyBonus = Math.min(efficiencyBonus, 2000f);
            }

            float speed = toolSpeed + efficiencyBonus;

            if (player.hasPotionEffect(PotionEffectType.HASTE)) {
                int level = player.getPotionEffect(PotionEffectType.HASTE).getAmplifier() + 1;
                speed *= (1 + (0.2 * level));
            }

            if (player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
                int level = player.getPotionEffect(PotionEffectType.MINING_FATIGUE).getAmplifier() + 1;
                speed /= (1 + (0.3 * level));
            }

            float ticks = (hardness * 20) / speed;
            ticks = Math.max(1, Math.min(ticks, 1200));
            
            return (int) Math.ceil(ticks);
        }

        private boolean isFortuneable(Material material) {
            String name = material.name();
            return name.contains("ORE") || name.contains("COAL") || 
                   name.contains("DIAMOND") || name.contains("EMERALD") ||
                   name.contains("LAPIS") || name.contains("REDSTONE") ||
                   name.contains("QUARTZ") || name.contains("NETHER_GOLD");
        }

        private int getFortuneBonus(int fortuneLevel) {
            Random rand = new Random();
            int bonus = 0;
            for (int i = 0; i < fortuneLevel; i++) {
                if (rand.nextDouble() < 0.33) {
                    bonus++;
                }
            }
            return bonus;
        }

        private void resetBreak() {
            if (currentBlock != null && currentBlock.getLocation() != null) {
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
