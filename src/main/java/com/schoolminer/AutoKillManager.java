package com.schoolminer;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
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
        task.runTaskTimer(plugin, 0L, config.getAttackDelay());
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
        private final int range;

        public KillTask(Player player) {
            this.player = player;
            this.range = getRange(player);
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) {
                stopKilling(player);
                return;
            }

            Entity target = player.getNearbyEntities(range, range, range).stream()
                .filter(e -> e instanceof Monster || e instanceof Animals)
                .filter(e -> e.isValid() && !e.isDead())
                .min((e1, e2) -> Double.compare(
                    e1.getLocation().distanceSquared(player.getLocation()),
                    e2.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);

            if (target == null) return;

            ItemStack weapon = player.getInventory().getItemInMainHand();
            double damage = calculateDamage(player, weapon);
            
            if (target instanceof LivingEntity living) {
                living.damage(damage, player);
                player.attack(target);
                
                player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, 
                    Material.REDSTONE_BLOCK);
                player.getWorld().spawnParticle(Particle.CRIT, 
                    target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
                
                if (living.isDead()) {
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        living.getLocation(), 30, 0.5, 0.5, 0.5);
                }
            }
        }
    }

    private double calculateDamage(Player player, ItemStack weapon) {
        double base = 1.0;
        
        if (weapon != null && !weapon.getType().isAir()) {
            String name = weapon.getType().name();
            if (name.contains("SWORD")) base = 4.0;
            else if (name.contains("AXE")) base = 6.0;
            else if (name.contains("TRIDENT")) base = 5.0;
            else if (name.contains("MACE")) base = 7.0;
            
            base += weapon.getEnchantmentLevel(Enchantment.SHARPNESS) * 1.5;
            base += weapon.getEnchantmentLevel(Enchantment.SMITE) * 2.5;
            base += weapon.getEnchantmentLevel(Enchantment.BANE_OF_ARTHROPODS) * 2.5;
        }
        
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH)) {
            base *= 1.3;
        }
        
        return base;
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
