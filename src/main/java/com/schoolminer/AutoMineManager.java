package com.schoolminer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Location;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import java.util.*;

public class AutoMineManager implements Listener {
    private final Schoolminer plugin;
    private final Map<UUID, MineTask> tasks = new HashMap<>();
    private final ConfigManager config;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Random random = new Random();
    private final Map<Location, UUID> lockedBlocks = new HashMap<>();

    public AutoMineManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void startMining(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) {
            player.sendMessage("§e⚠️ Auto Mine đã được bật rồi!");
            return;
        }
        
        lastLocations.put(uuid, player.getLocation().clone());
        MineTask task = new MineTask(player);
        task.runTaskTimer(plugin, 0L, 1L);
        tasks.put(uuid, task);
        
        player.sendMessage("§a✅ Đã bật Auto Mine!");
        player.sendMessage("§7Đứng yên và cầm công cụ để đào tự động.");
        player.sendMessage("§7Block đang đào sẽ được §ckhóa §7cho riêng bạn!");
    }

    public void stopMining(Player player) {
        UUID uuid = player.getUniqueId();
        MineTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
            unlockAllBlocks(player);
            try {
                Block target = player.getTargetBlockExact(5);
                if (target != null && target.getLocation() != null) {
                    player.sendBlockDamage(target.getLocation(), 0.0f);
                }
            } catch (Exception ignored) {}
            player.sendMessage("§c⛔ Đã tắt Auto Mine!");
        } else {
            player.sendMessage("§e⚠️ Bạn chưa bật Auto Mine!");
        }
        lastLocations.remove(uuid);
    }

    public void stopAll() {
        for (MineTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        lastLocations.clear();
        lockedBlocks.clear();
    }

    public boolean isMining(Player player) {
        return tasks.containsKey(player.getUniqueId());
    }

    private void unlockAllBlocks(Player player) {
        UUID uuid = player.getUniqueId();
        Iterator<Map.Entry<Location, UUID>> iterator = lockedBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, UUID> entry = iterator.next();
            if (entry.getValue().equals(uuid)) {
                iterator.remove();
            }
        }
    }

    private void unlockBlock(Location location, UUID uuid) {
        if (lockedBlocks.containsKey(location) && lockedBlocks.get(location).equals(uuid)) {
            lockedBlocks.remove(location);
        }
    }

    private void lockBlock(Location location, UUID uuid) {
        lockedBlocks.put(location, uuid);
    }

    private boolean isBlockLocked(Location location, Player player) {
        if (lockedBlocks.containsKey(location)) {
            UUID owner = lockedBlocks.get(location);
            if (owner.equals(player.getUniqueId())) {
                return false;
            }
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();
        
        if (isBlockLocked(loc, player)) {
            UUID owner = lockedBlocks.get(loc);
            Player ownerPlayer = Bukkit.getPlayer(owner);
            String ownerName = ownerPlayer != null ? ownerPlayer.getName() : "Unknown";
            event.setCancelled(true);
            player.sendMessage("§c⚠️ Block này đang được §e" + ownerName + " §cauto mine!");
            player.sendMessage("§7Vui lòng đợi họ đào xong!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();
        
        if (isBlockLocked(loc, player)) {
            event.setCancelled(true);
        }
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
            this.range = 5;
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

            if (isBlockLocked(target.getLocation(), player)) {
                resetBreak();
                return;
            }

            if (currentBlock == null || !currentBlock.equals(target)) {
                if (currentBlock != null) {
                    unlockBlock(currentBlock.getLocation(), playerUUID);
                }
                currentBlock = target;
                breakTicks = 0;
                isBreaking = false;
                lockBlock(target.getLocation(), playerUUID);
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
                BlockBreakEvent breakEvent = new BlockBreakEvent(target, player);
                Bukkit.getPluginManager().callEvent(breakEvent);
                
                if (!breakEvent.isCancelled()) {
                    Collection<ItemStack> drops = target.getDrops(tool);
                    
                    int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
                    if (fortune > 0 && isFortuneable(target.getType())) {
                        for (ItemStack drop : drops) {
                            int bonus = getFortuneBonus(fortune);
                            drop.setAmount(drop.getAmount() * (1 + bonus));
                        }
                    }
                    
                    boolean doubleDrop = config.isDoubleDrop();
                    int multiBlockLevel = plugin.getConfigManager().getMultiBlockLevel(tool);
                    PlayerInventory inventory = player.getInventory();
                    
                    for (ItemStack drop : drops) {
                        if (drop != null && !drop.getType().isAir()) {
                            int amount = drop.getAmount();
                            
                            if (doubleDrop) {
                                amount *= 2;
                            }
                            
                            if (multiBlockLevel > 1) {
                                amount *= multiBlockLevel;
                            }
                            
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
                    
                    int exp = getBlockExp(target, tool);
                    if (exp > 0) {
                        player.giveExp(exp);
                    }
                    
                    // Chỉ play sound, không particle
                    player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getType());
                }
                
                if (currentBlock != null) {
                    unlockBlock(currentBlock.getLocation(), playerUUID);
                    currentBlock = null;
                }
                
                breakTicks = 0;
                isBreaking = false;
            }
        }

        private int getBlockExp(Block block, ItemStack tool) {
            Material type = block.getType();
            String name = type.name();
            
            if (name.contains("COAL_ORE") || name.contains("DEEPSLATE_COAL_ORE")) {
                return 0 + new Random().nextInt(2);
            }
            if (name.contains("IRON_ORE") || name.contains("DEEPSLATE_IRON_ORE")) {
                return 1 + new Random().nextInt(2);
            }
            if (name.contains("GOLD_ORE") || name.contains("DEEPSLATE_GOLD_ORE") || name.contains("NETHER_GOLD_ORE")) {
                return 2 + new Random().nextInt(3);
            }
            if (name.contains("DIAMOND_ORE") || name.contains("DEEPSLATE_DIAMOND_ORE")) {
                return 3 + new Random().nextInt(4);
            }
            if (name.contains("EMERALD_ORE") || name.contains("DEEPSLATE_EMERALD_ORE")) {
                return 3 + new Random().nextInt(4);
            }
            if (name.contains("LAPIS_ORE") || name.contains("DEEPSLATE_LAPIS_ORE")) {
                return 2 + new Random().nextInt(3);
            }
            if (name.contains("REDSTONE_ORE") || name.contains("DEEPSLATE_REDSTONE_ORE")) {
                return 1 + new Random().nextInt(3);
            }
            if (name.contains("NETHER_QUARTZ_ORE")) {
                return 2 + new Random().nextInt(3);
            }
            if (name.contains("ANCIENT_DEBRIS")) {
                return 4 + new Random().nextInt(5);
            }
            if (name.contains("COPPER_ORE") || name.contains("DEEPSLATE_COPPER_ORE")) {
                return 1 + new Random().nextInt(2);
            }
            
            return 0;
        }

        private boolean isFortuneable(Material material) {
            String name = material.name();
            return name.contains("ORE") || name.contains("COAL") || 
                   name.contains("DIAMOND") || name.contains("EMERALD") ||
                   name.contains("LAPIS") || name.contains("REDSTONE") ||
                   name.contains("QUARTZ") || name.contains("NETHER_GOLD") ||
                   name.contains("AMETHYST") || name.contains("COPPER");
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
                unlockBlock(currentBlock.getLocation(), playerUUID);
            }
            currentBlock = null;
            breakTicks = 0;
            isBreaking = false;
        }
    }
}