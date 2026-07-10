package com.schoolminer;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.PlayerInventory;
import java.util.*;

public class AutoKillManager {
    private final Schoolminer plugin;
    private final Map<UUID, KillTask> tasks = new HashMap<>();
    private final ConfigManager config;

    public AutoKillManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void startKilling(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) return;
        
        KillTask task = new KillTask(player);
        task.runTaskTimer(plugin, 0L, 40L); // 40 ticks = 2 giây
        tasks.put(uuid, task);
        
        player.sendMessage(config.getMessage("kill-enabled"));
    }

    public void stopKilling(Player player) {
        UUID uuid = player.getUniqueId();
        KillTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
            player.sendMessage(config.getMessage("kill-disabled"));
        }
    }

    public void stopAll() {
        for (KillTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }

    public boolean isKilling(Player player) {
        return tasks.containsKey(player.getUniqueId());
    }

    private class KillTask extends BukkitRunnable {
        private final Player player;
        private final int range = 2; // Cố định 2 block

        public KillTask(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) {
                stopKilling(player);
                return;
            }

            // Tìm mob gần nhất trong phạm vi 2 block
            Entity target = player.getNearbyEntities(range, range, range).stream()
                .filter(e -> e instanceof Monster || e instanceof Animals || e instanceof Mob)
                .filter(e -> e.isValid() && !e.isDead())
                .min((e1, e2) -> Double.compare(
                    e1.getLocation().distanceSquared(player.getLocation()),
                    e2.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);

            if (target == null) return;

            // Kiểm tra khoảng cách
            if (target.getLocation().distance(player.getLocation()) > range) return;

            ItemStack weapon = player.getInventory().getItemInMainHand();
            double damage = calculateDamage(player, weapon);
            
            if (target instanceof LivingEntity living) {
                // Tấn công mob
                living.damage(damage, player);
                player.attack(target);
                
                // Hiệu ứng
                player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, 
                    Material.REDSTONE_BLOCK);
                player.getWorld().spawnParticle(Particle.CRIT, 
                    target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
                
                // Kiểm tra nếu mob chết
                if (living.isDead() || living.getHealth() <= 0) {
                    // Lấy drops từ mob
                    Collection<ItemStack> drops = living.getDrops();
                    
                    // Drop items từ mob
                    for (ItemStack drop : drops) {
                        if (drop != null && !drop.getType().isAir()) {
                            Item item = player.getWorld().dropItem(living.getLocation(), drop);
                            item.setPickupDelay(0);
                            // Hút item về phía player
                            item.setVelocity(player.getLocation().toVector()
                                .subtract(item.getLocation().toVector())
                                .normalize().multiply(0.3));
                        }
                    }
                    
                    // XP
                    int xp = living.getExperience();
                    if (xp > 0) {
                        player.giveExp(xp);
                    }
                    
                    // Hiệu ứng chết
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        living.getLocation(), 30, 0.5, 0.5, 0.5);
                    
                    // Lấy drops từ MythicMobs (nếu có)
                    try {
                        // MythicMobs compatibility
                        if (living.hasMetadata("MythicMobs")) {
                            // MythicMobs sẽ tự động drop items
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        private double calculateDamage(Player player, ItemStack weapon) {
            double base = 1.0;
            
            if (weapon != null && !weapon.getType().isAir()) {
                String name = weapon.getType().name();
                
                // Base damage theo loại vũ khí
                if (name.contains("NETHERITE_SWORD")) base = 8.0;
                else if (name.contains("DIAMOND_SWORD")) base = 7.0;
                else if (name.contains("IRON_SWORD")) base = 6.0;
                else if (name.contains("STONE_SWORD")) base = 5.0;
                else if (name.contains("WOODEN_SWORD") || name.contains("WOOD_SWORD")) base = 4.0;
                else if (name.contains("GOLDEN_SWORD") || name.contains("GOLD_SWORD")) base = 4.0;
                else if (name.contains("NETHERITE_AXE")) base = 10.0;
                else if (name.contains("DIAMOND_AXE")) base = 9.0;
                else if (name.contains("IRON_AXE")) base = 8.0;
                else if (name.contains("STONE_AXE")) base = 7.0;
                else if (name.contains("WOODEN_AXE") || name.contains("WOOD_AXE")) base = 5.0;
                else if (name.contains("GOLDEN_AXE") || name.contains("GOLD_AXE")) base = 5.0;
                else if (name.contains("TRIDENT")) base = 9.0;
                else if (name.contains("MACE")) base = 12.0;
                
                // Enchantment Sharpness
                int sharpness = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
                if (sharpness > 0) {
                    base += (sharpness * 1.5);
                }
                
                // Enchantment Smite (cho undead)
                int smite = weapon.getEnchantmentLevel(Enchantment.SMITE);
                if (smite > 0) {
                    base += (smite * 2.5);
                }
                
                // Enchantment Bane of Arthropods
                int bane = weapon.getEnchantmentLevel(Enchantment.BANE_OF_ARTHROPODS);
                if (bane > 0) {
                    base += (bane * 2.5);
                }
                
                // Enchantment Fire Aspect
                int fireAspect = weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
                if (fireAspect > 0) {
                    base += 1.0;
                }
            }
            
            // Strength effect
            if (player.hasPotionEffect(PotionEffectType.STRENGTH)) {
                int level = player.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier() + 1;
                base *= (1 + (0.3 * level));
            }
            
            // Critical hit (20% chance)
            if (Math.random() < 0.2) {
                base *= 1.5;
            }
            
            return Math.max(base, 1.0);
        }
    }
}
