package com.schoolminer;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Location;
import java.util.*;

public class AutoKillManager {
    private final Schoolminer plugin;
    private final Map<UUID, KillTask> tasks = new HashMap<>();
    private final ConfigManager config;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public AutoKillManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void startKilling(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) return;
        
        lastLocations.put(uuid, player.getLocation().clone());
        KillTask task = new KillTask(player);
        task.runTaskTimer(plugin, 0L, 40L);
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
        lastLocations.remove(uuid);
    }

    public void stopAll() {
        for (KillTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        lastLocations.clear();
    }

    public boolean isKilling(Player player) {
        return tasks.containsKey(player.getUniqueId());
    }

    private class KillTask extends BukkitRunnable {
        private final Player player;
        private final int range = 2;
        private final UUID playerUUID;

        public KillTask(Player player) {
            this.player = player;
            this.playerUUID = player.getUniqueId();
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) {
                stopKilling(player);
                return;
            }

            Location lastLoc = lastLocations.get(playerUUID);
            if (lastLoc != null) {
                Location currentLoc = player.getLocation();
                boolean isSitting = false;
                
                try {
                    if (player.hasMetadata("GSit")) {
                        isSitting = true;
                    }
                } catch (Exception ignored) {}
                
                try {
                    if (player.hasMetadata("CMISit")) {
                        isSitting = true;
                    }
                } catch (Exception ignored) {}
                
                try {
                    if (player.hasMetadata("Sit")) {
                        isSitting = true;
                    }
                } catch (Exception ignored) {}
                
                if (!isSitting && lastLoc.distance(currentLoc) > 0.1) {
                    player.sendMessage("§c⚠️ Bạn đã di chuyển, Auto Kill đã tắt!");
                    stopKilling(player);
                    return;
                }
            }

            Entity target = player.getNearbyEntities(range, range, range).stream()
                .filter(e -> e instanceof Monster || e instanceof Animals || e instanceof Mob)
                .filter(e -> e.isValid() && !e.isDead())
                .min((e1, e2) -> Double.compare(
                    e1.getLocation().distanceSquared(player.getLocation()),
                    e2.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);

            if (target == null) return;

            if (target.getLocation().distance(player.getLocation()) > range) return;

            ItemStack weapon = player.getInventory().getItemInMainHand();
            double damage = calculateDamage(player, weapon);
            
            if (target instanceof LivingEntity living) {
                living.damage(damage, player);
                player.attack(target);
                
                player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, 
                    Material.REDSTONE_BLOCK);
                player.getWorld().spawnParticle(Particle.CRIT, 
                    target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
                
                if (living.isDead() || living.getHealth() <= 0) {
                    if (living instanceof Monster monster) {
                        ItemStack handItem = monster.getEquipment().getItemInMainHand();
                        if (handItem != null && !handItem.getType().isAir()) {
                            Item item = player.getWorld().dropItem(living.getLocation(), handItem);
                            item.setPickupDelay(0);
                            item.setVelocity(player.getLocation().toVector()
                                .subtract(item.getLocation().toVector())
                                .normalize().multiply(0.5));
                        }
                        
                        for (ItemStack armor : monster.getEquipment().getArmorContents()) {
                            if (armor != null && !armor.getType().isAir()) {
                                Item item = player.getWorld().dropItem(living.getLocation(), armor);
                                item.setPickupDelay(0);
                                item.setVelocity(player.getLocation().toVector()
                                    .subtract(item.getLocation().toVector())
                                    .normalize().multiply(0.5));
                            }
                        }
                    }
                    
                    int xp = 0;
                    if (living instanceof Monster) {
                        xp = 5 + new Random().nextInt(3);
                    } else if (living instanceof Animals) {
                        xp = 1 + new Random().nextInt(3);
                    } else if (living instanceof Mob) {
                        xp = 3 + new Random().nextInt(5);
                    }
                    
                    if (xp > 0) {
                        player.giveExp(xp);
                    }
                    
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        living.getLocation(), 30, 0.5, 0.5, 0.5);
                    
                    try {
                        if (living.hasMetadata("MythicMobs")) {
                            // MythicMobs auto drop
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        private double calculateDamage(Player player, ItemStack weapon) {
            double base = 1.0;
            
            if (weapon != null && !weapon.getType().isAir()) {
                String name = weapon.getType().name();
                
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
                
                int sharpness = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
                if (sharpness > 0) base += (sharpness * 1.5);
                
                int smite = weapon.getEnchantmentLevel(Enchantment.SMITE);
                if (smite > 0) base += (smite * 2.5);
                
                int bane = weapon.getEnchantmentLevel(Enchantment.BANE_OF_ARTHROPODS);
                if (bane > 0) base += (bane * 2.5);
                
                int fireAspect = weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
                if (fireAspect > 0) base += 1.0;
            }
            
            // Lấy sức mạnh từ armor
            double armorAttack = 0.0;
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null && !armor.getType().isAir()) {
                    if (armor.hasItemMeta() && armor.getItemMeta().hasDisplayName()) {
                        String name = armor.getItemMeta().getDisplayName();
                        if (name.contains("Lục Bảo") || name.contains("Bảo vệ") || 
                            name.contains("tấn công") || name.contains("Sức mạnh") ||
                            name.contains("Attack") || name.contains("Damage")) {
                            armorAttack += 2.0;
                        }
                    }
                    
                    if (armor.hasItemMeta() && armor.getItemMeta().hasLore()) {
                        List<String> lore = armor.getItemMeta().getLore();
                        for (String line : lore) {
                            if (line.contains("Sức tận công") || line.contains("Attack") || 
                                line.contains("+") && line.contains("tấn công")) {
                                try {
                                    String[] parts = line.split("\\+");
                                    if (parts.length > 1) {
                                        String num = parts[1].replaceAll("[^0-9.]", "");
                                        armorAttack += Double.parseDouble(num);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
            
            base += armorAttack;
            
            if (player.hasPotionEffect(PotionEffectType.STRENGTH)) {
                int level = player.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier() + 1;
                base *= (1 + (0.3 * level));
            }
            
            if (Math.random() < 0.2) {
                base *= 1.5;
            }
            
            return Math.max(base, 1.0);
        }
    }
}
